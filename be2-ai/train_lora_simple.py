# -*- coding: utf-8 -*-
"""
LoRA SFT (간단 Trainer 버전, Windows/RTX 8GB 대응 FIX)
- 4bit/8bit 선택 (--bits 4 | 8 | 0)
- k-bit 준비(prepare_model_for_kbit_training) 적용
- LoRA 주입 후에만 gradient checkpointing을 use_reentrant=False 로 명시 활성화
- 입력 require_grads 강제 (checkpoint 경고/무그라드 문제 해결)
권장:
  transformers==4.44.2, accelerate==0.34.2, bitsandbytes==0.43.1,
  peft==0.12.0, datasets>=2.20.0
"""

import os, json, argparse
from typing import List, Dict, Any

import torch
from torch.utils.data import Dataset

from transformers import (
    AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig,
    TrainingArguments, Trainer, default_data_collator
)
from peft import get_peft_model, LoraConfig, TaskType, prepare_model_for_kbit_training


# ---------- helpers ----------
def _ids_to_list(x):
    import torch as _torch
    if isinstance(x, _torch.Tensor):
        return x.squeeze(0).tolist()
    if isinstance(x, list) and x and isinstance(x[0], list):
        return x[0]
    return list(x)

def _ensure_eos_pad(tokenizer):
    if tokenizer.eos_token_id is None:
        for tok in ("</s>", "<|eot_id|>", "<|endoftext|>"):
            tid = tokenizer.convert_tokens_to_ids(tok)
            if isinstance(tid, int) and tid > 0:
                tokenizer.eos_token = tok
                break
        if tokenizer.eos_token_id is None and tokenizer.pad_token_id is not None:
            tokenizer.eos_token_id = tokenizer.pad_token_id
    if tokenizer.pad_token_id is None and tokenizer.eos_token_id is not None:
        tokenizer.pad_token_id = tokenizer.eos_token_id
    tokenizer.padding_side = "right"

def _build_text_manual(messages: List[Dict[str, str]]) -> str:
    out = []
    for m in messages:
        out.append(f"<|{m.get('role','user')}|>\n{m.get('content','')}\n")
    return "".join(out)

def _enable_inputs_require_grads_safely(model):
    """모델에 따라 method 유무가 달라서 안전하게 처리"""
    try:
        if hasattr(model, "enable_input_require_grads"):
            model.enable_input_require_grads()
            return
        # PEFT 래퍼 안쪽까지 파고들기
        base = getattr(model, "base_model", None) or model
        inner = getattr(base, "model", None)
        if inner is not None and hasattr(inner, "enable_input_require_grads"):
            inner.enable_input_require_grads()
            return
    except Exception:
        pass

    # fallback: 임베딩 출력에 hook을 걸어 requires_grad 강제
    try:
        base = getattr(model, "base_model", None) or model
        inner = getattr(base, "model", None) or base
        emb = inner.get_input_embeddings()
        if emb is not None:
            def _hook(_m, _inp, out):
                if isinstance(out, torch.Tensor):
                    out.requires_grad_(True)
            emb.register_forward_hook(_hook)
    except Exception:
        pass


# ---------- dataset ----------
class JSONLSFTDataset(Dataset):
    def __init__(self, path: str, tokenizer: AutoTokenizer, max_len: int):
        self.rows = []
        self.tok = tokenizer
        self.max_len = max_len

        with open(path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    self.rows.append(json.loads(line))
                except Exception:
                    continue

    def __len__(self): return len(self.rows)

    def __getitem__(self, i):
        ex = self.rows[i]
        msgs = ex.get("messages", [])

        # 마지막 assistant만 정답으로 학습
        last_asst = None
        for idx in range(len(msgs)-1, -1, -1):
            if msgs[idx].get("role") == "assistant":
                last_asst = idx
                break
        if last_asst is None:
            return {
                "input_ids": torch.tensor([self.tok.eos_token_id]),
                "labels": torch.tensor([-100]),
                "attention_mask": torch.tensor([1]),
            }

        prompt_msgs = msgs[:last_asst]
        resp_text = msgs[last_asst].get("content", "")

        # prompt
        if getattr(self.tok, "chat_template", None):
            try:
                prompt_ids = self.tok.apply_chat_template(
                    prompt_msgs, add_generation_prompt=True,
                    tokenize=True, truncation=True, max_length=self.max_len
                )
            except Exception:
                prompt_text = _build_text_manual(prompt_msgs)
                prompt_ids = _ids_to_list(
                    self.tok(prompt_text, return_tensors="pt",
                             truncation=True, max_length=self.max_len,
                             add_special_tokens=False)["input_ids"]
                )
        else:
            prompt_text = _build_text_manual(prompt_msgs)
            prompt_ids = _ids_to_list(
                self.tok(prompt_text, return_tensors="pt",
                         truncation=True, max_length=self.max_len,
                         add_special_tokens=False)["input_ids"]
            )

        # response
        resp_ids = _ids_to_list(
            self.tok(resp_text, return_tensors="pt",
                     truncation=True, max_length=self.max_len,
                     add_special_tokens=False)["input_ids"]
        )

        eos = self.tok.eos_token_id if self.tok.eos_token_id is not None else self.tok.pad_token_id
        input_ids = prompt_ids + resp_ids + ([eos] if eos is not None else [])
        labels    = [-100]*len(prompt_ids) + resp_ids + ([eos] if eos is not None else [])
        attn      = [1]*len(input_ids)

        return {
            "input_ids": torch.tensor(input_ids, dtype=torch.long),
            "labels": torch.tensor(labels, dtype=torch.long),
            "attention_mask": torch.tensor(attn, dtype=torch.long),
        }


# ---------- model loader ----------
def build_model_tokenizer(model_name: str, bits: int, max_len: int):
    has_cuda = torch.cuda.is_available()
    dtype = torch.float16 if has_cuda else torch.float32

    tok = AutoTokenizer.from_pretrained(model_name, use_fast=True, trust_remote_code=True)
    _ensure_eos_pad(tok)
    if not getattr(tok, "model_max_length", None) or tok.model_max_length > max_len:
        tok.model_max_length = max_len

    kw = dict(trust_remote_code=True)
    if bits in (4, 8):
        bnb_cfg = BitsAndBytesConfig(
            load_in_4bit=(bits == 4),
            load_in_8bit=(bits == 8),
            bnb_4bit_quant_type="nf4",
            bnb_4bit_use_double_quant=False,  # 윈도우/저RAM에서 CPU 피크 완화
            bnb_4bit_compute_dtype=dtype,
        )
        # device_map과 low_cpu_mem_usage를 함께 써서 로드시 RAM 피크/CPU OOM 완화
        model = AutoModelForCausalLM.from_pretrained(
            model_name,
            quantization_config=bnb_cfg,
            device_map="auto",
            max_memory={0: "7GiB", "cpu": "8GiB"},
            torch_dtype=dtype,
            low_cpu_mem_usage=True,
            **kw
        )
        # PEFT 준비 (여기서는 체크포인팅을 "끄고" 준비만)
        model = prepare_model_for_kbit_training(
            model,
            use_gradient_checkpointing=False
        )
    else:
        model = AutoModelForCausalLM.from_pretrained(
            model_name,
            torch_dtype=dtype,
            low_cpu_mem_usage=True,
            device_map="auto",
            max_memory={0: "7GiB", "cpu": "8GiB"},
            **kw
        )

    # 캐시 비활성 (GC와 호환)
    try:
        model.config.use_cache = False
    except Exception:
        pass

    return model, tok


def get_llama_targets(user_str: str | None) -> List[str]:
    if user_str:
        return [s.strip() for s in user_str.split(",") if s.strip()]
    # 라마 계열 표준 타깃
    return ["q_proj","k_proj","v_proj","o_proj","gate_proj","up_proj","down_proj"]


# ---------- main ----------
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--train", type=str, required=True)
    ap.add_argument("--valid", type=str, default=None)
    ap.add_argument("--out",   type=str, required=True)
    ap.add_argument("--model", type=str, default="MLP-KTLim/llama-3-Korean-Bllossom-8B")
    ap.add_argument("--steps", type=int, default=200)
    ap.add_argument("--lr",    type=float, default=2e-4)
    ap.add_argument("--max_len", type=int, default=3072)
    ap.add_argument("--batch",   type=int, default=1)
    ap.add_argument("--grad_accum", type=int, default=2)
    ap.add_argument("--bits", type=int, choices=[0,4,8], default=4, help="0=no quant, 4=4bit, 8=8bit")
    ap.add_argument("--target_modules", type=str, default="", help="콤마로 구분된 모듈명 목록(미지정 시 라마 기본 7종)")
    args = ap.parse_args()

    model, tokenizer = build_model_tokenizer(args.model, args.bits, args.max_len)

    lora_cfg = LoraConfig(
        task_type=TaskType.CAUSAL_LM,
        r=8, lora_alpha=16, lora_dropout=0.05,
        target_modules=get_llama_targets(args.target_modules),
        bias="none"
    )
    model = get_peft_model(model, lora_cfg)
    model.print_trainable_parameters()

    # === 여기서 명시적으로 체크포인팅/입력그라드 세팅 (핵심 FIX) ===
    try:
        # reentrant=False 명시 (PyTorch 2.5 경고/끊김 방지)
        model.gradient_checkpointing_enable(gradient_checkpointing_kwargs={"use_reentrant": False})
    except Exception:
        # PEFT 래핑 깊이에 따라
        try:
            base = getattr(model, "base_model", None) or model
            inner = getattr(base, "model", None) or base
            inner.gradient_checkpointing_enable(gradient_checkpointing_kwargs={"use_reentrant": False})
        except Exception:
            pass

    _enable_inputs_require_grads_safely(model)

    # 데이터셋
    train_ds = JSONLSFTDataset(args.train, tokenizer, args.max_len)
    eval_ds  = JSONLSFTDataset(args.valid, tokenizer, args.max_len) if args.valid and os.path.exists(args.valid) else None
    if len(train_ds) == 0:
        raise RuntimeError("훈련 데이터가 비었습니다.")

    bf16 = torch.cuda.is_available() and torch.cuda.is_bf16_supported()
    fp16 = torch.cuda.is_available() and not bf16

    args_tr = TrainingArguments(
        output_dir=args.out,
        num_train_epochs=1, max_steps=args.steps,
        per_device_train_batch_size=args.batch,
        per_device_eval_batch_size=max(1,args.batch),
        gradient_accumulation_steps=args.grad_accum,
        learning_rate=args.lr, lr_scheduler_type="cosine",
        warmup_ratio=0.03, weight_decay=0.0,
        logging_steps=max(1, args.steps//20),
        evaluation_strategy="steps" if eval_ds else "no",
        eval_steps=max(1, args.steps//5) if eval_ds else None,
        save_strategy="no", report_to="none",
        bf16=bf16, fp16=fp16,
        dataloader_num_workers=0,
        # Trainer의 gradient_checkpointing 옵션은 쓰지 않음(우리가 직접 켰음)
    )

    trainer = Trainer(
        model=model, args=args_tr,
        train_dataset=train_ds, eval_dataset=eval_ds,
        tokenizer=tokenizer,
        data_collator=default_data_collator,
    )
    trainer.train()

    os.makedirs(args.out, exist_ok=True)
    model.save_pretrained(args.out)
    tokenizer.save_pretrained(args.out)
    print(f"[OK] Saved LoRA adapter to: {args.out}")


if __name__ == "__main__":
    main()
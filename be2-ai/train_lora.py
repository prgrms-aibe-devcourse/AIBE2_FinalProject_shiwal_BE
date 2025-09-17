# -*- coding: utf-8 -*-
"""
LoRA SFT (TRL SFTTrainer)
- 4bit/8bit 선택 가능 (--bits 4 | 8 | 0)
- k-bit 준비 + 표준 라마 타깃 모듈 고정
- 이미 input_ids/labels 생성 → SFTTrainer가 그대로 사용
"""

import os, json, argparse
from typing import List, Dict

import torch
from torch.utils.data import Dataset

from transformers import (
    AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig,
    TrainingArguments, default_data_collator
)
from peft import get_peft_model, LoraConfig, TaskType, prepare_model_for_kbit_training
from trl import SFTTrainer


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


# ---------- dataset (encoded) ----------
class EncodedChatDataset(Dataset):
    def __init__(self, path: str, tokenizer: AutoTokenizer, max_len: int):
        self.samples = []
        self.tok = tokenizer
        self.max_len = max_len

        with open(path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    ex = json.loads(line)
                except Exception:
                    continue
                packed = self._encode_ex(ex)
                if packed is not None:
                    self.samples.append(packed)

    def __len__(self): return len(self.samples)
    def __getitem__(self, i): return self.samples[i]

    def _encode_ex(self, ex):
        msgs = ex.get("messages", [])
        last_asst = None
        for idx in range(len(msgs)-1, -1, -1):
            if msgs[idx].get("role") == "assistant":
                last_asst = idx
                break
        if last_asst is None:
            return None

        prompt_msgs = msgs[:last_asst]
        resp_text   = msgs[last_asst].get("content","")

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
    dtype = torch.bfloat16 if has_cuda else torch.float32

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
            bnb_4bit_use_double_quant=True,
            bnb_4bit_compute_dtype=dtype,
        )
        model = AutoModelForCausalLM.from_pretrained(
            model_name,
            quantization_config=bnb_cfg,
            low_cpu_mem_usage=False,
            torch_dtype=dtype,
            **kw
        )
        model = prepare_model_for_kbit_training(model, use_gradient_checkpointing=True)
    else:
        model = AutoModelForCausalLM.from_pretrained(
            model_name,
            torch_dtype=dtype,
            low_cpu_mem_usage=False,
            **kw
        )

    return model, tok

def get_llama_targets(user_str: str | None):
    if user_str:
        return [s.strip() for s in user_str.split(",") if s.strip()]
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
    ap.add_argument("--bits", type=int, choices=[0,4,8], default=4)
    ap.add_argument("--target_modules", type=str, default="")
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

    train_ds = EncodedChatDataset(args.train, tokenizer, args.max_len)
    eval_ds  = EncodedChatDataset(args.valid, tokenizer, args.max_len) if args.valid and os.path.exists(args.valid) else None
    if len(train_ds) == 0:
        raise RuntimeError("훈련 데이터가 비었습니다.")

    bf16 = torch.cuda.is_available() and torch.cuda.is_bf16_supported()
    fp16 = torch.cuda.is_available() and not bf16

    targs = TrainingArguments(
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
    )

    trainer = SFTTrainer(
        model=model,
        args=targs,
        train_dataset=train_ds,
        eval_dataset=eval_ds,
        tokenizer=tokenizer,
        data_collator=default_data_collator,
        peft_config=None,  # 이미 get_peft_model 적용
        packing=False,
        max_seq_length=args.max_len,
    )

    trainer.train()

    os.makedirs(args.out, exist_ok=True)
    model.save_pretrained(args.out)
    tokenizer.save_pretrained(args.out)
    print(f"[OK] Saved LoRA adapter to: {args.out}")


if __name__ == "__main__":
    main()
import os, torch, re, json
from typing import Dict, Any, Optional, List, Tuple
from transformers import AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig
from transformers.utils import logging as hf_logging
from collections import defaultdict, deque
from .config import (
    MODEL_NAME, HF_OFFLOAD_DIR, TEMPERATURE, TOP_P, MAX_NEW_TOKENS, 
    REPETITION_PENALTY
)

try:
    hf_logging.set_verbosity_error()
except Exception:
    pass

# ===== Model singleton =====
class _ModelSingleton:
    def __init__(self):
        self.tokenizer = None
        self.model = None
        self.sessions: Dict[str, deque] = defaultdict(lambda: deque(maxlen=16))
        self._load()

    def _load(self):
        has_cuda = False
        try:
            has_cuda = bool(getattr(torch.cuda, "is_available", lambda: False)())
        except Exception:
            has_cuda = False

        os.makedirs(HF_OFFLOAD_DIR, exist_ok=True)

        tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME, use_fast=True, trust_remote_code=True)
        if not hasattr(tokenizer, "model_max_length") or tokenizer.model_max_length > 4096:
            tokenizer.model_max_length = 3072

        if has_cuda:
            bnb_config = BitsAndBytesConfig(
                load_in_4bit=True,
                bnb_4bit_quant_type="nf4",
                bnb_4bit_use_double_quant=True,
                bnb_4bit_compute_dtype=torch.bfloat16,
            )
            device_map = "auto"; torch_dtype = torch.bfloat16; max_mem = {0: "9GiB", "cpu": "10GiB"}
        else:
            bnb_config = None; device_map = "cpu"; torch_dtype = torch.float32; max_mem = {"cpu": "30GiB"}

        model = AutoModelForCausalLM.from_pretrained(
            MODEL_NAME,
            quantization_config=bnb_config,
            device_map=device_map,
            torch_dtype=torch_dtype,
            low_cpu_mem_usage=True,
            offload_folder=HF_OFFLOAD_DIR,
            max_memory=max_mem,
            trust_remote_code=True,
        )
        if tokenizer.pad_token_id is None and tokenizer.eos_token_id is not None:
            tokenizer.pad_token_id = tokenizer.eos_token_id
        model.eval()

        # optional LoRA
        ADAPTER_DIR = os.getenv("HUE_ADAPTER_DIR")
        if ADAPTER_DIR and os.path.isdir(ADAPTER_DIR):
            try:
                from peft import PeftModel
                model = PeftModel.from_pretrained(model, ADAPTER_DIR); model.eval()
                print(f"[Hue] LoRA adapter loaded: {ADAPTER_DIR}")
            except Exception as e:
                print(f"[Hue] LoRA load skipped: {type(e).__name__}: {e}")

        self.tokenizer, self.model = tokenizer, model

S = _ModelSingleton()
tokenizer, model, SESSIONS = S.tokenizer, S.model, S.sessions

# ===== Generation helpers =====
def _safe_generate(inputs, *, max_new_tokens: int, temperature: float, top_p: float,
                   repetition_penalty: float = REPETITION_PENALTY, no_repeat_ngram_size: int = 4):
    if temperature is None: temperature = 0.0
    if temperature <= 0.0:
        tries = [
            dict(do_sample=False, max_new_tokens=max_new_tokens),
            dict(do_sample=True, temperature=0.25, top_p=min(0.9, top_p), max_new_tokens=min(128, max_new_tokens)),
            dict(do_sample=True, temperature=0.5, top_p=min(0.9, top_p), max_new_tokens=min(96, max_new_tokens)),
        ]
    else:
        tries = [
            dict(do_sample=True, temperature=temperature, top_p=top_p, max_new_tokens=max_new_tokens),
            dict(do_sample=True, temperature=min(0.7, temperature), top_p=min(0.9, top_p), max_new_tokens=min(128, max_new_tokens)),
            dict(do_sample=True, temperature=0.6, top_p=0.85, max_new_tokens=min(96, max_new_tokens)),
            dict(do_sample=True, temperature=0.5, top_p=0.8, max_new_tokens=min(64, max_new_tokens)),
        ]
    last_err = None
    for cfg in tries:
        try:
            with torch.no_grad():
                gen_kwargs = dict(
                    **inputs, max_new_tokens=cfg["max_new_tokens"],
                    eos_token_id=tokenizer.eos_token_id, pad_token_id=tokenizer.pad_token_id,
                    use_cache=True, repetition_penalty=repetition_penalty, no_repeat_ngram_size=no_repeat_ngram_size,
                )
                if cfg.get("do_sample", False):
                    gen_kwargs["do_sample"] = True
                    gen_kwargs["temperature"] = cfg.get("temperature", 0.7)
                    gen_kwargs["top_p"] = cfg.get("top_p", 0.9)
                else:
                    gen_kwargs["do_sample"] = False
                return model.generate(**gen_kwargs)
        except RuntimeError as e:
            last_err = e; msg = str(e)
            if ("CUDA out of memory" in msg) or ("cublas" in msg) or ("cuDNN" in msg):
                try:
                    if torch.cuda.is_available(): torch.cuda.empty_cache()
                except Exception: pass
                continue
            break
    raise last_err if last_err else RuntimeError("generation failed")

def chat_llm(user_content: str, system_content: Optional[str] = None,
             temperature: float = TEMPERATURE, max_new_tokens: int = MAX_NEW_TOKENS, top_p: float = TOP_P) -> str:
    messages = []
    if system_content:
        messages.append({"role": "system", "content": system_content})
    messages.append({"role": "user", "content": user_content})
    inputs = tokenizer.apply_chat_template(
        messages, add_generation_prompt=True, tokenize=True, truncation=True,
        return_tensors="pt", return_dict=True
    ).to(model.device)
    outputs = _safe_generate(inputs, max_new_tokens=max_new_tokens, temperature=temperature, top_p=top_p)
    gen = outputs[0][inputs["input_ids"].shape[-1]:]
    return tokenizer.decode(gen, skip_special_tokens=True).strip()

def gen_plain(prompt: str, *, max_new_tokens: int = 220, temperature: float = 0.0, top_p: float = 1.0) -> str:
    inputs = tokenizer(prompt, return_tensors="pt", truncation=True).to(model.device)
    outputs = _safe_generate(inputs, max_new_tokens=max_new_tokens, temperature=temperature, top_p=top_p)
    gen = outputs[0][inputs["input_ids"].shape[-1]:]
    return tokenizer.decode(gen, skip_special_tokens=True).strip()

def chat_llm_messages(messages: List[Dict[str, str]],
                      temperature: float = TEMPERATURE,
                      max_new_tokens: int = MAX_NEW_TOKENS,
                      top_p: float = TOP_P):
    def encode_len(msgs):
        enc = tokenizer.apply_chat_template(
            msgs, add_generation_prompt=True, tokenize=True, truncation=True,
            return_tensors="pt", return_dict=True,
        )
        return enc["input_ids"].shape[-1]

    msgs = messages[:]
    limit = min(getattr(tokenizer, "model_max_length", 3072) - 256, 3072)
    while encode_len(msgs) > limit and len(msgs) > 1:
        drop_idx = 1 if msgs and msgs[0].get("role") == "system" else 0
        msgs.pop(drop_idx)

    inputs = tokenizer.apply_chat_template(
        msgs, add_generation_prompt=True, tokenize=True, truncation=True,
        return_tensors="pt", return_dict=True
    ).to(model.device)

    outputs = _safe_generate(inputs, max_new_tokens=max_new_tokens, temperature=temperature, top_p=top_p)
    gen_ids = outputs[0][inputs["input_ids"].shape[-1]:]
    reply = tokenizer.decode(gen_ids, skip_special_tokens=True).strip()
    prompt_tokens = int(inputs["input_ids"].shape[-1])
    completion_tokens = int(gen_ids.shape[-1])
    return reply, prompt_tokens, completion_tokens
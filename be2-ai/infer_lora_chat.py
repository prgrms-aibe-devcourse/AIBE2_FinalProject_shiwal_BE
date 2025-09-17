# -*- coding: utf-8 -*-
import torch
from transformers import AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig
from peft import AutoPeftModelForCausalLM, PeftModel

BASE = r"MLP-KTLim/llama-3-Korean-Bllossom-8B"
ADAPTER = r"C:\Users\asdfc\AppData\Local\Temp\hue_adapters\hue-lora-v1"

bnb_cfg = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_quant_type="nf4",
    bnb_4bit_compute_dtype=torch.float16,
)

# LoRA 어댑터에서 바로 로드 -> 실패 시 베이스 + 어댑터 결합
try:
    model = AutoPeftModelForCausalLM.from_pretrained(
        ADAPTER, quantization_config=bnb_cfg, device_map="auto", torch_dtype=torch.float16
    )
except Exception as e:
    print("AutoPeft failed, fallback ->", e)
    base_model = AutoModelForCausalLM.from_pretrained(
        BASE, quantization_config=bnb_cfg, device_map="auto", torch_dtype=torch.float16
    )
    model = PeftModel.from_pretrained(base_model, ADAPTER)

model.eval()

tok = AutoTokenizer.from_pretrained(BASE, use_fast=True, trust_remote_code=True)
if tok.pad_token is None and tok.eos_token is not None:
    tok.pad_token = tok.eos_token

messages = [
    {"role": "system", "content": "당신은 유능한 한국어 비서입니다. 간결하고 정확하게 답하세요."},
    {"role": "user",   "content": "간단히 자기소개해 주세요."}
]

inputs = tok.apply_chat_template(messages, add_generation_prompt=True, return_tensors="pt")
attention_mask = torch.ones_like(inputs)  # 경고 제거용 (패딩 없음)

inputs = inputs.to(model.device)
attention_mask = attention_mask.to(model.device)

gen_kwargs = dict(
    max_new_tokens=256,
    do_sample=True,
    temperature=0.4,
    top_p=0.9,
    top_k=50,
    repetition_penalty=1.12,
    no_repeat_ngram_size=4,
    eos_token_id=tok.eos_token_id,
    pad_token_id=tok.eos_token_id,
)

with torch.inference_mode():
    out = model.generate(input_ids=inputs, attention_mask=attention_mask, **gen_kwargs)

prompt_len = inputs.shape[-1]
print(tok.decode(out[0][prompt_len:], skip_special_tokens=True).strip())

# -*- coding: utf-8 -*-
import os
import torch
from transformers import AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig
from peft import AutoPeftModelForCausalLM, PeftModel

# (선택) HF 경고/캐시 정리: TRANSFORMERS_CACHE 쓰면 경고 나와요.
os.environ.pop("TRANSFORMERS_CACHE", None)
os.environ.setdefault("HF_HOME", r"E:\hf_cache")  # 원하는 경로로

BASE    = r"MLP-KTLim/llama-3-Korean-Bllossom-8B"
ADAPTER = r"C:\Users\asdfc\AppData\Local\Temp\hue_adapters\hue-lora-v1"

bnb_cfg = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_quant_type="nf4",
    bnb_4bit_compute_dtype=torch.float16,
)

# 1) 어댑터에서 직접 로드 → 실패 시 베이스 + 어댑터 결합
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

# 2) Llama-3 계열: chat_template 사용 + 종료 토큰(EOT) 우선 사용
messages = [
    {"role": "system", "content": "당신은 유능한 한국어 비서입니다. 간결하고 정확하게 답하세요. 안전문구나 경고 문장은 답변에 포함하지 않습니다."},
    {"role": "user",   "content": "간단히 자기소개해 주세요."}
]

inputs = tok.apply_chat_template(messages, add_generation_prompt=True, return_tensors="pt")
attention_mask = torch.ones_like(inputs)  # 패딩 없으면 1로 충분

inputs = inputs.to(model.device)
attention_mask = attention_mask.to(model.device)

# 가능한 EOT 토큰을 우선 종료로 사용
eot_id = tok.convert_tokens_to_ids("<|eot_id|>")
if isinstance(eot_id, int) and eot_id >= 0:
    eos_ids = [eot_id]
    if tok.eos_token_id is not None and tok.eos_token_id != eot_id:
        eos_ids.append(tok.eos_token_id)
else:
    eos_ids = [tok.eos_token_id] if tok.eos_token_id is not None else None

# 3) "깔끔 모드": 샘플링 끄고 그리디/짧게
gen_kwargs = dict(
    max_new_tokens=128,
    do_sample=True,
    temperature=0.3,  # 낮게
    top_p=0.9,
    top_k=None,
    repetition_penalty=1.05,  # 살짝만 걸기
    no_repeat_ngram_size=4,
    eos_token_id=eos_ids,
    pad_token_id=tok.eos_token_id,
)

with torch.inference_mode():
    out = model.generate(input_ids=inputs, attention_mask=attention_mask, **gen_kwargs)

# 입력 길이 이후만 출력
prompt_len = inputs.shape[-1]
print(tok.decode(out[0][prompt_len:], skip_special_tokens=True).strip())
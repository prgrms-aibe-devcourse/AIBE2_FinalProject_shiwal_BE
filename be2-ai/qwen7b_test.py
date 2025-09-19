# qwen7b_test.py
from transformers import AutoTokenizer, AutoModelForCausalLM
import torch

model_name = "Qwen/Qwen2.5-7B-Instruct"  # 공개, 접근 승인 불필요

tok = AutoTokenizer.from_pretrained(model_name, use_fast=True)
model = AutoModelForCausalLM.from_pretrained(
    model_name,
    torch_dtype=torch.float16,
    device_map="auto"
)

prompt = "너는 한국어도 이해해? 한 문단 정도로 자기소개해줘."
inputs = tok(prompt, return_tensors="pt").to(model.device)
with torch.no_grad():
    out = model.generate(**inputs, max_new_tokens=150, do_sample=True, temperature=0.7)
print(tok.decode(out[0], skip_special_tokens=True))

# -*- coding: utf-8 -*-
import argparse, torch
from transformers import AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig
from peft import AutoPeftModelForCausalLM, PeftModel

BASE    = r"MLP-KTLim/llama-3-Korean-Bllossom-8B"
ADAPTER = r"C:\Users\asdfc\AppData\Local\Temp\hue_adapters\hue-lora-v1"

def load():
    bnb = BitsAndBytesConfig(load_in_4bit=True, bnb_4bit_quant_type="nf4", bnb_4bit_compute_dtype=torch.float16)
    try:
        m = AutoPeftModelForCausalLM.from_pretrained(ADAPTER, quantization_config=bnb, device_map="auto", torch_dtype=torch.float16)
    except Exception:
        base = AutoModelForCausalLM.from_pretrained(BASE, quantization_config=bnb, device_map="auto", torch_dtype=torch.float16)
        m = PeftModel.from_pretrained(base, ADAPTER)
    t = AutoTokenizer.from_pretrained(BASE, use_fast=True, trust_remote_code=True)
    if t.pad_token is None and t.eos_token is not None:
        t.pad_token = t.eos_token
    m.eval()
    return m, t

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("prompt", type=str)
    ap.add_argument("--system", type=str, default="한 줄로 핵심만, 여담/경고 없이.")
    args = ap.parse_args()

    model, tok = load()
    msgs = [{"role":"system","content":args.system},{"role":"user","content":args.prompt}]
    inputs = tok.apply_chat_template(msgs, add_generation_prompt=True, return_tensors="pt").to(model.device)
    mask = torch.ones_like(inputs)

    with torch.inference_mode():
        out = model.generate(
            input_ids=inputs, attention_mask=mask,
            max_new_tokens=128, do_sample=False,
            repetition_penalty=1.12, no_repeat_ngram_size=4,
            eos_token_id=tok.eos_token_id, pad_token_id=tok.eos_token_id
        )
    print(tok.decode(out[0][inputs.shape[-1]:], skip_special_tokens=True).strip())

if __name__ == "__main__":
    main()

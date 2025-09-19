# -*- coding: utf-8 -*-
import argparse, re
import torch
from transformers import AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig
from peft import AutoPeftModelForCausalLM, PeftModel

BASE    = r"MLP-KTLim/llama-3-Korean-Bllossom-8B"
ADAPTER = r"C:\Users\asdfc\AppData\Local\Temp\hue_adapters\hue-lora-v1"

# 잔향/훈수/안전문구/감정표현까지 폭넓게 차단
FORBIDDEN_SUBSTRINGS = [
    # 의료/안전/상담
    "의료","진단","치료","의학","상담이 필요","전문가에게","응급","긴급","119","112","경고","주의","안전","위험","병원","의사",
    # 심리/감정/위로
    "심리","마음","무겁","힘들","괜찮","위로","응원","걱정","불안","스트레스",
    # 생활습관 케어 잔향
    "복식호흡","호흡","수면","취침","화면","습관","운동","걷기","걸어",
    # 카페인/시간
    "카페인","오후 2시","오후2시","2시","2 시","두 시","14시","14:00","2:00",
]

FALLBACK_ANSWER = "1) 최우선 작업 1건 마무리\n2) 대기 중인 요청 1건 처리\n3) 내일 준비 1건 정리"

def load_model():
    bnb_cfg = BitsAndBytesConfig(load_in_4bit=True, bnb_4bit_quant_type="nf4", bnb_4bit_compute_dtype=torch.float16)
    try:
        m = AutoPeftModelForCausalLM.from_pretrained(ADAPTER, quantization_config=bnb_cfg, device_map="auto", torch_dtype=torch.float16)
    except Exception as e:
        print("AutoPeft failed, fallback ->", e)
        base = AutoModelForCausalLM.from_pretrained(BASE, quantization_config=bnb_cfg, device_map="auto", torch_dtype=torch.float16)
        m = PeftModel.from_pretrained(base, ADAPTER)
    m.eval()
    return m

def is_clean(text: str) -> bool:
    low = text.lower()
    return not any(bad.lower() in low for bad in FORBIDDEN_SUBSTRINGS)

def only_numbered_top3(text: str) -> str:
    # 번호/불릿만 추려서 3개로 제한
    lines = [l.strip() for l in text.strip().splitlines() if l.strip()]
    bullets = []
    for l in lines:
        if not is_clean(l):
            continue
        if re.match(r"^(\d+\s*[\.\)]|[-•])\s*", l):
            bullets.append(l)
    if bullets:
        return "\n".join(bullets[:3])
    # 번호가 없으면 문장 단위로 잘라 첫 1~3문장
    sents = [s.strip() for s in re.split(r"(?<=[.!?。])\s+", " ".join(lines)) if s.strip()]
    sents = [s for s in sents if is_clean(s)]
    if not sents:
        return ""
    # 1~3줄로 재구성
    if len(sents) == 1:
        return f"1) {sents[0]}"
    if len(sents) == 2:
        return f"1) {sents[0]}\n2) {sents[1]}"
    return f"1) {sents[0]}\n2) {sents[1]}\n3) {sents[2]}"

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--prompt", type=str, default="간단히 자기소개해 주세요.")
    ap.add_argument("--max_new", type=int, default=128)
    args = ap.parse_args()

    model = load_model()
    tok = AutoTokenizer.from_pretrained(BASE, use_fast=True, trust_remote_code=True)
    if tok.pad_token is None and tok.eos_token is not None:
        tok.pad_token = tok.eos_token

    # 형식/톤 강제
    system = (
        "너는 한국어 비서다. 요청한 정보만 간결하게. "
        "심리·의학·안전 경고, 생활습관 훈수, 카페인/시간 관리 언급 금지. "
        "가능하면 1)~3) 번호 목록으로만 답하라."
    )
    messages = [
        {"role": "system", "content": system},
        {"role": "user",   "content": args.prompt},
    ]
    inputs = tok.apply_chat_template(messages, add_generation_prompt=True, return_tensors="pt")
    attention_mask = torch.ones_like(inputs)

    inputs = inputs.to(model.device)
    attention_mask = attention_mask.to(model.device)

    # 빔서치로 6개 후보 생성 (결정론적), temperature/top_p=1.0로 경고 제거
    with torch.inference_mode():
        out = model.generate(
            input_ids=inputs,
            attention_mask=attention_mask,
            max_new_tokens=args.max_new,
            do_sample=False,
            num_beams=6,
            num_return_sequences=6,
            early_stopping=True,
            length_penalty=0.1,
            repetition_penalty=1.15,
            no_repeat_ngram_size=4,
            temperature=1.0,     # ← 경고 제거
            top_p=1.0,           # ← 경고 제거
            eos_token_id=tok.eos_token_id,
            pad_token_id=tok.eos_token_id,
        )

    prompt_len = inputs.shape[-1]
    candidates = [tok.decode(seq[prompt_len:], skip_special_tokens=True).strip() for seq in out]

    # 1) 깨끗한 후보 우선(짧은 것부터)
    candidates.sort(key=len)
    chosen = next((c for c in candidates if is_clean(c)), None)

    # 2) 전부 오염이면 가장 짧은 걸 가져와 줄/문장 단위로 오염 제거
    if chosen is None:
        raw = candidates[0] if candidates else ""
        lines = [l for l in raw.splitlines() if is_clean(l)]
        chosen = "\n".join(lines).strip()

    # 3) 번호 1~3개 형식으로 정리
    final = only_numbered_top3(chosen)

    # 4) 그래도 비면 안전한 기본 답안
    if not final.strip():
        final = FALLBACK_ANSWER

    print(final)

if __name__ == "__main__":
    main()

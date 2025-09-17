# -*- coding: utf-8 -*-
# make_jsonl.py  (UTF-8 safe JSONL builder from TSV)

import sys, os, csv, json, random

SYSTEM_PROMPT = (
    "당신은 Hue, 지원적인 한국어 AI 코치입니다. "
    "조언은 간결하고 실천 가능하게, 진단/병명 같은 임상 용어는 피하세요. "
    "자가/타해 신호가 보이면 112/119/1393 안내가 필요합니다."
)

def read_tsv_utf8(path):
    # UTF-8-SIG(BOM)도 허용
    for enc in ("utf-8-sig", "utf-8"):
        try:
            with open(path, "r", encoding=enc, newline="") as f:
                return list(csv.DictReader(f, delimiter="\t"))
        except UnicodeDecodeError:
            continue
    raise UnicodeDecodeError("utf-8", b"", 0, 1, "cannot decode file as utf-8 / utf-8-sig")

def clean(s: str) -> str:
    if not s:
        return ""
    s = s.replace("\t", " ").replace("\r", " ").replace("\n", " ")
    while "  " in s:
        s = s.replace("  ", " ")
    return s.strip()

def looks_broken(s: str) -> bool:
    if not s:
        return True
    # 과도한 '?'면 모지바케로 간주
    return s.count("?") >= max(3, len(s)//4)

def main(tsv_path, out_dir):
    os.makedirs(out_dir, exist_ok=True)
    rows = read_tsv_utf8(tsv_path)

    data = []
    for r in rows:
        msg = clean(r.get("message", ""))
        rep = clean(r.get("reply", ""))
        if not msg or not rep:
            continue
        # 2차 필터: 깨진 줄 제거
        if looks_broken(msg) or looks_broken(rep):
            continue

        example = {
            "messages": [
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": msg},
                {"role": "assistant", "content": rep},
            ]
        }
        data.append(example)

    random.seed(2024)
    random.shuffle(data)

    n = len(data)
    n_valid = max(1, round(n * 0.1)) if n >= 10 else 1
    valid = data[:n_valid]
    train = data[n_valid:]

    train_path = os.path.join(out_dir, "train.jsonl")
    valid_path = os.path.join(out_dir, "valid.jsonl")

    with open(train_path, "w", encoding="utf-8", newline="") as f:
        for ex in train:
            f.write(json.dumps(ex, ensure_ascii=False) + "\n")
    with open(valid_path, "w", encoding="utf-8", newline="") as f:
        for ex in valid:
            f.write(json.dumps(ex, ensure_ascii=False) + "\n")

    print(f"done. total={n} train={len(train)} valid={len(valid)} -> {out_dir}")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python make_jsonl.py <seed_clean.tsv> <out_dir>")
        sys.exit(1)
    main(sys.argv[1], sys.argv[2])
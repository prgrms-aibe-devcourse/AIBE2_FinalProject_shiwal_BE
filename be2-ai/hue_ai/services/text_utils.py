import re
import unicodedata

STRICT_PATTERNS = [
    r"자\s*살", r"극\s*단\s*선\s*택",
    r"죽\s*고\s*싶", r"뛰\s*어\s*내리", r"목\s*매",
    r"kill\s*myself", r"suicide",
    r"사람\s*을?\s*죽이", r"사람\s*을?\s*해치"
]

SEVERITY_DICT = {
    "자살": 3, "극단선택": 3, "죽고싶": 3, "죽고 싶": 3, "죽을래": 3, "스스로 해": 3,
    "목숨": 3, "뛰어내리": 3, "목매": 3, "die": 3, "kill myself": 3, "suicide": 3,
    "해치고": 2, "죽여": 2, "죽일": 2, "칼": 2, "폭력": 2, "피흘": 2, "베어": 2, "없애버": 2,
    "harm others": 2,
    "살기싫": 1, "살고싶지 않": 1, "사라지고": 1, "없어지고": 1, "힘들어서 끝내": 1, "self-harm": 1
}

def _remove_all_unicode_spaces(s: str) -> str:
    return re.sub(r"[\u0009-\u000D\u0020\u0085\u00A0\u1680\u180E\u2000-\u200A\u2028\u2029\u202F\u205F\u3000]", "", s)

def strict_match(text: str) -> bool:
    if not text:
        return False
    t = unicodedata.normalize("NFKC", text)
    no_space = _remove_all_unicode_spaces(t.lower())
    for p in STRICT_PATTERNS:
        try:
            if re.search(p, t, re.I):
                return True
            pp = re.sub(r"\s+", "", p)
            if re.search(pp, no_space, re.I):
                return True
        except Exception:
            continue
    return False

def detect_crisis_keywords(text: str):
    t = (text or "")
    t_low = t.lower()
    compact = _remove_all_unicode_spaces(t_low)
    hits, score = [], 0
    for kw, w in SEVERITY_DICT.items():
        if (" " in kw and kw in t_low) or (kw.replace(" ", "") in compact):
            hits.append(kw); score += w
    if strict_match(t) and score < 3:
        score = 3
        if "죽고 싶" not in hits:
            hits.append("죽고 싶")
    return {"score": score, "hits": sorted(set(hits))}
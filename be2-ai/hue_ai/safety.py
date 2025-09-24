import re, unicodedata
from typing import Dict, Any, List
from .text_utils import sanitize_korean_strict, ko_text_fix, split_sentences_ko, looks_non_displayable
from .gen import chat_llm
from .config import CRISIS_MODE

SEVERITY_DICT = {
    "자살": 3, "극단선택": 3, "죽고싶": 3, "죽고 싶": 3, "죽을래": 3, "스스로 해": 3,
    "목숨": 3, "뛰어내리": 3, "목매": 3, "die": 3, "kill myself": 3, "suicide": 3,
    "해치고": 2, "죽여": 2, "죽일": 2, "칼": 2, "폭력": 2, "피흘": 2, "베어": 2, "없애버": 2, "harm others": 2,
    "살기싫": 1, "살고싶지 않": 1, "사라지고": 1, "없어지고": 1, "힘들어서 끝내": 1, "self-harm": 1
}

STRICT_PATTERNS = [
    r"자\s*살", r"극\s*단\s*선\s*택",
    r"죽\s*고\s*싶", r"뛰\s*어\s*내리", r"목\s*매",
    r"kill\s*myself", r"suicide",
    r"사람\s*을?\s*죽이", r"사람\s*을?\s*해치"
]

def _remove_all_unicode_spaces(s: str) -> str:
    return re.sub(r"[\u0009-\u000D\u0020\u0085\u00A0\u1680\u180E\u2000-\u200A\u2028\u2029\u202F\u205F\u3000]", "", s)

def _normalize_ko(s: str) -> str:
    s = s.lower()
    s = _remove_all_unicode_spaces(s)
    s = re.sub(r"[\u200b\u200c\u200d]", "", s)
    return s

def strict_match(text: str) -> bool:
    if not text: return False
    t = unicodedata.normalize("NFKC", text)
    no_space = _remove_all_unicode_spaces(t.lower())
    for p in STRICT_PATTERNS:
        if re.search(p, t, re.I): return True
        pp = re.sub(r"\s+", "", p)
        if re.search(pp, no_space, re.I): return True
    return False

def detect_crisis_keywords(text: str) -> Dict[str, Any]:
    t = (text or "")
    t_low = t.lower()
    t_compact = _normalize_ko(t)
    hits, score = [], 0
    for kw, w in SEVERITY_DICT.items():
        if (" " in kw and kw in t_low) or (kw.replace(" ", "") in t_compact):
            hits.append(kw); score += w
    if strict_match(t) and score < 3:
        score = 3
        if "죽고 싶" not in hits: hits.append("죽고 싶")
    return {"score": score, "hits": sorted(set(hits))}

def decide_crisis(kw_score: int, risk: str, policy: str) -> bool:
    if kw_score >= 3: return True
    if risk == "high": return True
    if policy != "high_only":
        return risk == "medium" and kw_score >= 1
    return False

_BAD_DETAIL = re.compile(r"(방법|도구|계획|용량|mg|밀리그램|칼|번개탄|가스|목|질식|뛰어내리|목매|철로|다리|치명|자상)", re.I)
def _contains_banned_detail(s: str) -> bool:
    return bool(_BAD_DETAIL.search(s or ""))

def crisis_template_reply() -> str:
    return ("지금 많이 버거웠겠어요. 혼자가 아니고 도움을 구해도 괜찮습니다. "
            "지금 당장 1) 주변의 위험한 물건을 치우고 2) 믿을 수 있는 사람이나 도움 창구에 연락하세요.\n\n"
            "긴급 도움이 필요하면 112/119/1393(자살예방핫라인)에 연락하세요.")

def safe_coach_reply(user_msg: str) -> str:
    try:
        prompt = f"""[INSTRUCTION]
당신은 공감적인 한국어 코치입니다.
아래 메시지에는 위험 신호가 있습니다.

규칙:
- 1~2문장, 따뜻하고 또렷하게.
- 구체적 방법/도구/계획/용량/지시 언급 금지.
- '지금' 안전 확인과 주변/전문가 연결을 부드럽게 권유.
- 한자/영문/메타설명/코드 금지, 결과만.

사용자 메시지: {user_msg}
코치:"""
        out = chat_llm(prompt, system_content=None, temperature=0.3, top_p=0.9, max_new_tokens=110)
        from .text_utils import sanitize_korean_strict, ko_text_fix, split_sentences_ko
        out = sanitize_korean_strict(out, max_sent=2)
        out = ko_text_fix(out)
        if _contains_banned_detail(out) or len(out) < 2:
            return ""
        out = re.sub(r"^(안녕하세요|저는|나는|이름은)[^.\n]*[.\n]\s*", "", out).strip()
        out = " ".join(split_sentences_ko(out)[:2])
        return out
    except Exception:
        return ""
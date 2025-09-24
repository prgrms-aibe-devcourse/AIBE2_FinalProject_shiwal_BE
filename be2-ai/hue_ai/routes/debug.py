# hue_ai/routes/debug.py
from fastapi import APIRouter, Depends, Query
from typing import Optional, Dict, Any
import re
import unicodedata

from ..utils.auth import require_api_key

router = APIRouter(prefix="/v1", tags=["debug"])

# --- 최소 위기 감지(라이트): 404 방지용 / 빠른 동작 확인용 ---
_STRICT_PATTERNS = [
    r"자\s*살", r"극\s*단\s*선\s*택",
    r"죽\s*고\s*싶", r"뛰\s*어\s*내리", r"목\s*매",
    r"kill\s*myself", r"suicide",
    r"사람\s*을?\s*죽이", r"사람\s*을?\s*해치"
]

def _remove_all_unicode_spaces(s: str) -> str:
    return re.sub(r"[\u0009-\u000D\u0020\u0085\u00A0\u1680\u180E\u2000-\u200A\u2028\u2029\u202F\u205F\u3000]", "", s)

def strict_match(text: str) -> bool:
    if not text:
        return False
    t = unicodedata.normalize("NFKC", text)
    no_space = _remove_all_unicode_spaces(t.lower())
    for p in _STRICT_PATTERNS:
        if re.search(p, t, re.I):
            return True
        pp = re.sub(r"\s+", "", p)
        if re.search(pp, no_space, re.I):
            return True
    return False

def detect_kw(text: str) -> Dict[str, Any]:
    sev = {"자살":3,"극단선택":3,"죽고싶":3,"죽고 싶":3,"kill myself":3,"suicide":3,
           "해치고":2,"죽여":2,"죽일":2,"폭력":2,
           "살기싫":1,"없어지고":1}
    hits, score = [], 0
    low = (text or "").lower()
    compact = _remove_all_unicode_spaces(low)
    for k,w in sev.items():
        if (" " in k and k in low) or (k.replace(" ","") in compact):
            hits.append(k); score += w
    if strict_match(text) and score < 3:
        score = 3; 
        if "죽고 싶" not in hits: hits.append("죽고 싶")
    return {"score": score, "hits": sorted(set(hits))}

@router.get("/debug/kw", dependencies=[Depends(require_api_key)])
def debug_kw(text: str = Query(..., max_length=4000)):
    kw = detect_kw(text)
    return {"kw": kw, "strict": strict_match(text)}

@router.get("/debug/decision", dependencies=[Depends(require_api_key)])
def debug_decision(text: str = Query(..., max_length=4000)):
    kw = detect_kw(text)
    # 라이트 정책: kw>=3 또는 strict면 템플릿
    templated = kw["score"] >= 3 or strict_match(text)
    return {"kw": kw, "strict": strict_match(text), "template": templated}
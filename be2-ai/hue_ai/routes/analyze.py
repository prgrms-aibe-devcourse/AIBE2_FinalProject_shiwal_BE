# hue_ai/routes/analyze.py
from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field
from typing import List, Optional
import re

from ..utils.auth import require_api_key

router = APIRouter(prefix="/v1", tags=["analyze"])

class AnalyzeIn(BaseModel):
    text: str = Field(..., min_length=1, max_length=4000)
    mood_slider: Optional[int] = Field(None, ge=0, le=100)
    tags: Optional[List[str]] = None

class AnalyzeOut(BaseModel):
    score: int
    summary: str
    tags: List[str]
    caution: bool

def _sanitize_summary(s: str) -> str:
    s = re.sub(r"```.*?```", " ", s, flags=re.S)
    s = re.sub(r"`[^`]+`", " ", s)
    s = re.sub(r"^\s*[-*•]\s+", "", s, flags=re.M)
    s = re.sub(r"#{1,6}\s*", "", s)
    s = re.sub(r"\s{2,}", " ", s).strip()
    return s

@router.post("/analyze", response_model=AnalyzeOut, dependencies=[Depends(require_api_key)])
def analyze(body: AnalyzeIn):
    text = (body.text or "")[:3500]
    # 아주 단순 점수/태그(모형 로드 없이 404 탈출용). 이후 services 연결하면 됨.
    base = min(100, max(0, 50 + (10 if "면접" in text else 0) - (10 if "잠" in text else 0)))
    if body.mood_slider is not None:
        base = int(round(base * 0.7 + body.mood_slider * 0.3))
    tags = [t for t in ["불안","수면","면접","스트레스"] if t in text] or ["스트레스"]
    summary = _sanitize_summary("핵심은 스스로를 돌봐야 한다는 신호예요. 오늘 3분만 타이머 켜고 쉬운 일 1개 시작해요.")
    caution = any(k in text for k in ["죽고 싶", "자살", "suicide"])
    return AnalyzeOut(score=base, summary=summary, tags=tags[:5], caution=caution)
# hue_ai/routes/chat.py
from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field
from typing import List, Optional
import random
import re

from ..utils.auth import require_api_key

router = APIRouter(prefix="/v1", tags=["chat"])

class ChatIn(BaseModel):
    session_id: str = Field(..., min_length=1, max_length=200)
    message: str = Field(..., min_length=1, max_length=4000)
    context: Optional[List[str]] = None
    user_id: Optional[int] = None

class ChatOut(BaseModel):
    reply: str
    safetyFlags: List[str] = []

_ACTIONS = {
    "sleep": ["취침1시간 전 화면 끄기","23시에 불 끄기"],
    "work":  ["5분짜리 쉬운 일부터 시작","받은편지함 3개만 정리"],
    "help":  ["3분 타이머 켜고 메모","창문 열고 30초 호흡"],
    "default":["3분 타이머 후 한 가지 시작"]
}

def _intent(t: str) -> str:
    t=t.lower()
    if re.search(r"자\s*살|죽\s*고\s*싶|suicide|kill\s*myself", t): return "crisis"
    if any(k in t for k in ["잠","불면","수면"]): return "sleep"
    if any(k in t for k in ["퇴근","업무","프로젝트","일이"]): return "work"
    if any(k in t for k in ["도와줘","어떻게 해야","힘들어"]): return "help"
    return "default"

def _finalize(s: str) -> str:
    s = re.sub(r"[`*_>#\[\]{}()]", "", s)
    s = re.sub(r"\s{2,}", " ", s).strip()
    return s

@router.post("/chat", response_model=ChatOut, dependencies=[Depends(require_api_key)])
def chat(body: ChatIn):
    msg = (body.message or "")[:3500]
    it = _intent(msg)

    if it == "crisis":
        base = ("지금 많이 버거웠겠어요. 혼자가 아니고 도움을 구해도 괜찮습니다. "
                "지금 당장 1) 위험한 물건을 치우고 2) 믿을 수 있는 사람이나 도움 창구에 연락하세요. "
                "긴급 시 112/119/1393에 연락하세요.")
        return ChatOut(reply=_finalize(base), safetyFlags=["CRISIS_TEMPLATED"])

    act = random.choice(_ACTIONS.get(it, _ACTIONS["default"]))
    if it == "sleep":
        txt = f"오늘은 화면 시간을 줄이고 취침 1시간 전 조명을 낮춰봐요. {act}"
    elif it == "work":
        txt = f"머리가 바쁠수록 가볍게 시작해요. {act}"
    elif it == "help":
        txt = f"기준을 확 낮춰서 아주 작은 걸로 시작해요. {act}"
    else:
        txt = f"지금 숨 한 번 고르고 몸의 긴장을 풀어봐요. {act}"

    return ChatOut(reply=_finalize(txt), safetyFlags=[])
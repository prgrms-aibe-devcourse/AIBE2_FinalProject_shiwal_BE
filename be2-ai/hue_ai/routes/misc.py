# E:\hue\AIBE2_FinalProject_shiwal_BE\be2-ai\hue_ai\routes\misc.py
from typing import Optional
from fastapi import APIRouter, Depends, Header
from fastapi.responses import JSONResponse
from ..utils.auth import require_api_key

router = APIRouter()

@router.get("/__auth/check", dependencies=[Depends(require_api_key)])
def auth_check(x_api_key: Optional[str] = Header(default=None)):
    # 인증이 통과됐다는 뜻이므로 ok 반환
    return JSONResponse(content={"ok": True, "who": "authorized"}, media_type="application/json; charset=utf-8")

@router.get("/warmup", dependencies=[Depends(require_api_key)])
def warmup():
    # (필요하면 여기서 실제 LLM 가벼운 호출 넣어도 됨)
    # ex) from ..services.llm import quick_warmup; quick_warmup()
    return JSONResponse(content={"warmed": True}, media_type="application/json; charset=utf-8")
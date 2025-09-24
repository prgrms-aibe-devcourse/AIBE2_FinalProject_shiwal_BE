# hue_ai/routes/openai_proxy.py
from fastapi import APIRouter, Depends
from pydantic import BaseModel
from typing import List
from ..utils.auth import require_api_key

router = APIRouter(prefix="/v1", tags=["openai-proxy"])

class OAIMsg(BaseModel):
    role: str
    content: str

class OAIChatReq(BaseModel):
    model: str | None = None
    messages: List[OAIMsg]
    temperature: float | None = 0.6
    top_p: float | None = 0.9
    max_tokens: int | None = 140
    stream: bool | None = False
    stop: List[str] | None = None
    user: str | None = None

@router.post("/chat/completions", dependencies=[Depends(require_api_key)])
def chat_completions(_: OAIChatReq):
    # 최소 스텁: 501로 명확히 알림 (원 구현 붙일 때 교체)
    return {
        "id": "chatcmpl-stub",
        "object": "chat.completion",
        "created": 0,
        "model": "stub",
        "choices": [{"index": 0, "finish_reason": "stop", "message": {"role": "assistant", "content": "[stub] not implemented"}}],
        "usage": {"prompt_tokens": 0, "completion_tokens": 0, "total_tokens": 0},
        "note": "openai_proxy stub — replace with real implementation"
    }
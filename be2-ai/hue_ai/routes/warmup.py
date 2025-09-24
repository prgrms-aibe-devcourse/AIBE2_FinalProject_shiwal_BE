from fastapi import APIRouter, Depends
from ..security import require_api_key
from ..llm import chat_llm

router = APIRouter()

@router.get("/warmup", dependencies=[Depends(require_api_key)])
def warmup():
    _ = chat_llm("간단히 한 문장으로 안부만 전해줘.")
    return {"warmed": True}
from fastapi import APIRouter
from .health import router as health_router
from .auth import router as auth_router           # ← 이 줄 꼭 있어야 함
from .debug import router as debug_router         # (있으면)
from .analyze import router as analyze_router     # (있으면)
from .chat import router as chat_router           # (있으면)
from .openai_proxy import router as oai_router    # (있으면)

api = APIRouter()
api.include_router(health_router)
api.include_router(auth_router)                   # ← 이 줄 꼭 있어야 함
# 아래는 선택(있는 라우터만)
api.include_router(debug_router)
api.include_router(analyze_router)
api.include_router(chat_router)
api.include_router(oai_router)
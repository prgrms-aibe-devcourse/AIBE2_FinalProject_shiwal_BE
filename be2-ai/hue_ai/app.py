from fastapi import FastAPI, Header
from fastapi.middleware.cors import CORSMiddleware
from typing import Optional
from .config import APP_VERSION, HUE_API_KEY
from .routes import api

def create_app() -> FastAPI:
    app = FastAPI(title="Hue AI API", version=APP_VERSION)
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"], allow_credentials=False, allow_methods=["*"], allow_headers=["*"]
    )
    app.include_router(api)   # ← 이 줄로 위에서 묶은 라우터들 등록
    return app
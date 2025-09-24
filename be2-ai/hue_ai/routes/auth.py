from fastapi import APIRouter, Depends
from ..utils.auth import require_api_key

router = APIRouter(tags=["auth"])

@router.get("/__auth/check", dependencies=[Depends(require_api_key)])
def auth_check():
    return {"ok": True, "who": "authorized"}

@router.get("/warmup", dependencies=[Depends(require_api_key)])
def warmup():
    return {"warmed": True}
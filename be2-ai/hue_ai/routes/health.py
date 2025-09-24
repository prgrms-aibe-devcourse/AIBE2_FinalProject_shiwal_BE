from fastapi import APIRouter
from ..config import APP_VERSION, MODEL_NAME, CRISIS_TEMPLATE_POLICY, CRISIS_MODE, DB_CFG, CHAT_TABLE

router = APIRouter(tags=["health"])

def _torch_info():
    try:
        import torch  # noqa
        cuda_ok = bool(getattr(torch.cuda, "is_available", lambda: False)())
        cnt = int(getattr(torch.cuda, "device_count", lambda: 0)())
        name = torch.cuda.get_device_name(0) if cuda_ok and cnt > 0 else "CPU"
        return {
            "torch": getattr(torch, "__version__", "unknown"),
            "cuda_available": cuda_ok,
            "cuda_device_count": cnt,
            "device_name": name,
        }
    except Exception:
        return {
            "torch": "unknown",
            "cuda_available": False,
            "cuda_device_count": 0,
            "device_name": "CPU",
        }

@router.get("/health")
def health():
    return {
        "ok": True,
        "model": MODEL_NAME,
        "version": APP_VERSION,
        "db": {"enabled": bool(DB_CFG.get("password")), "err": "", "chat_table": CHAT_TABLE},
        "policy": CRISIS_TEMPLATE_POLICY,
        "crisis_mode": CRISIS_MODE,
        **_torch_info(),
    }
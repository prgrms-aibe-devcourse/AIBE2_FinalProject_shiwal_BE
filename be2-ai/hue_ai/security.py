from typing import Optional
from fastapi import Header, HTTPException
from .config import HUE_API_KEY

def require_api_key(x_api_key: Optional[str] = Header(default=None)):
    # 디버그: 실제 비교되는 값 로깅(문제되면 주석처리)
    print(f"[AUTH] env={repr(HUE_API_KEY)} req={repr(x_api_key)}")
    if HUE_API_KEY and x_api_key != HUE_API_KEY:
        raise HTTPException(status_code=401, detail="invalid api key")
    return True
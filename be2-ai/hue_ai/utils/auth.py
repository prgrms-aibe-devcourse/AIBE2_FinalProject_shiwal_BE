from fastapi import Header, HTTPException
from typing import Optional
from ..config import HUE_API_KEY

def require_api_key(x_api_key: Optional[str] = Header(default=None)):
    if HUE_API_KEY and x_api_key != HUE_API_KEY:
        raise HTTPException(status_code=401, detail="invalid api key")
    return True
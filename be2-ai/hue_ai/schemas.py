from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field

class AnalyzeIn(BaseModel):
    text: str = Field(..., min_length=1, max_length=4000)
    mood_slider: Optional[int] = Field(None, ge=0, le=100)
    tags: Optional[List[str]] = None

class AnalyzeOut(BaseModel):
    score: int = Field(..., ge=0, le=100)
    summary: str = Field(..., min_length=1, max_length=480)
    tags: List[str] = Field(default_factory=list, max_items=5)
    caution: bool

class ChatIn(BaseModel):
    session_id: str = Field(..., min_length=1, max_length=200)
    message: str = Field(..., min_length=1, max_length=4000)
    context: Optional[List[str]] = None
    user_id: Optional[int] = None

class ChatOut(BaseModel):
    reply: str
    safetyFlags: List[str] = Field(default_factory=list)

class OAIMsg(BaseModel):
    role: str
    content: str

class OAIChatReq(BaseModel):
    model: Optional[str] = None
    messages: List[OAIMsg]
    temperature: Optional[float] = 0.6
    top_p: Optional[float] = 0.9
    max_tokens: Optional[int] = 140
    stream: Optional[bool] = False
    stop: Optional[List[str]] = None
    user: Optional[str] = None
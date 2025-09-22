# app.py — Hue (v0.9.1-counsel-H6)
# - 위기: 템플릿(+짧은 공감 멘트) 고정 안전
# - 비위기: 의도별 프롬프트 + 액션 뱅크로 다양화 (2~3문장 + 오늘 바로 할 행동)
# - 인코딩/잡음 방어: looks_non_displayable, ENCODING_SUSPECT 플래그
# - /v1/analyze, /v1/chat, /v1/chatx, /v1/chat/completions, /admin/policy, /v1/debug/*

import os
import re
import json
import time
import secrets
import unicodedata
import hashlib
from typing import List, Optional, Dict, Any, Tuple
from functools import lru_cache
from collections import defaultdict, deque

from fastapi import FastAPI, Header, HTTPException, Depends, Request, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field
from fastapi.testclient import TestClient

import torch
from transformers import AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig
from transformers.utils import logging as hf_logging

APP_VERSION = "0.9.1-counsel-H6"

# ----- JSON Response (ORJSON 우선) -----
try:
    import orjson
    from fastapi.responses import ORJSONResponse as JSONResponse
except Exception:
    from fastapi.responses import JSONResponse  # fallback

# ===================== 런타임/환경 =====================
os.environ.setdefault("HF_HOME", r"E:\hf_cache")
os.environ.setdefault("TRANSFORMERS_CACHE", rf"{os.getenv('HF_HOME')}\transformers")
os.environ.setdefault("TORCH_HOME", r"E:\torch_cache")
os.environ.setdefault("CUDA_VISIBLE_DEVICES", "0")
os.environ.setdefault("TOKENIZERS_PARALLELISM", "false")
os.environ.setdefault("PYTORCH_CUDA_ALLOC_CONF", "expandable_segments:True")
os.environ.setdefault("TRANSFORMERS_VERBOSITY", "warning")

try:
    hf_logging.set_verbosity_error()
except Exception:
    pass

try:
    torch.set_num_threads(2)
    torch.backends.cuda.matmul.allow_tf32 = True
    torch.set_float32_matmul_precision("high")
except Exception:
    pass

MODEL_NAME = os.getenv("LLM_ID", "MLP-KTLim/llama-3-Korean-Bllossom-8B")

# 시스템 프롬프트(간결/실천 중심)
SYSTEM_PROMPT = (
    "당신은 Hue, 지원적인 한국어 AI 코치입니다. "
    "답변은 간결하고 실천 가능하게. 임상 진단/치료 용어는 피하고, "
    "자/타해 위험이 보이면 즉시 도움을 권합니다."
)

# 위기 템플릿 정책 + 위기 모드(템플릿만 / 템플릿+짧은생성)
CRISIS_TEMPLATE_POLICY = os.getenv("HUE_CRISIS_TEMPLATE", "high_only").lower()
CRISIS_MODE = os.getenv("HUE_CRISIS_MODE", "template_plus_coach").lower()  # template_only | template_plus_coach

HUE_API_KEY = os.getenv("HUE_API_KEY")
DEBUG_JSON = os.getenv("HUE_DEBUG_JSON") == "1"

# DB 옵션 (없어도 동작, 있으면 자동 로깅)
DB_CFG = {
    "host": os.getenv("HUE_DB_HOST", "127.0.0.1"),
    "port": int(os.getenv("HUE_DB_PORT", "3306")),
    "user": os.getenv("HUE_DB_USER", "root"),
    "password": os.getenv("HUE_DB_PASS", "") or None,
    "database": os.getenv("HUE_DB_NAME", "hue"),
}
# ➕ 채팅 로그 테이블명 (기본: ai_chat_messages)
CHAT_TABLE = os.getenv("HUE_CHAT_TABLE", "ai_chat_messages")

try:
    import pymysql  # type: ignore
    _has_pymysql = True
except Exception:
    _has_pymysql = False


class DBLogger:
    def __init__(self, cfg: Dict[str, Any]):
        self.enabled = False
        self.err = None
        self.cfg = cfg
        self.conn = None
        if not _has_pymysql:
            self.err = "pymysql_not_installed"
            return
        if not cfg.get("password"):
            self.err = "no_db_password"
            return
        try:
            self.conn = pymysql.connect(
                host=cfg["host"], port=cfg["port"],
                user=cfg["user"], password=cfg["password"], database=cfg["database"],
                autocommit=True, charset="utf8mb4", cursorclass=pymysql.cursors.DictCursor
            )
            self.enabled = True
        except Exception as e:
            self.err = f"connect_error:{type(e).__name__}"

    def log_crisis(self, user_id: Optional[int], session_id: str, text: str,
                   kw_score: int, risk: str, reasons: List[str], templated: bool):
        if not self.enabled:
            return
        try:
            h = hashlib.sha256(text.encode("utf-8")).hexdigest()
            with self.conn.cursor() as cur:
                cur.execute(
                    """
                    INSERT INTO crisis_events (user_id, session_id, text_hash, kw_score, risk, reasons, templated)
                    VALUES (%s,%s,%s,%s,%s,%s,%s)
                    """,
                    (user_id, session_id, h, int(kw_score), risk, json.dumps(reasons, ensure_ascii=False), int(templated))
                )
        except Exception:
            pass

    def log_chat(self, user_id: Optional[int], session_id: str, message: str,
                 reply: str, safety_flags: List[str]):
        if not self.enabled:
            return
        try:
            with self.conn.cursor() as cur:
                cur.execute(
                    f"""
                    INSERT INTO {CHAT_TABLE} (user_id, session_id, message, reply, safety_flags)
                    VALUES (%s,%s,%s,%s,%s)
                    """,
                    (user_id, session_id, message, reply, json.dumps(safety_flags, ensure_ascii=False))
                )
        except Exception:
            pass


DB = DBLogger(DB_CFG)

app = FastAPI(title="Hue AI API", version=APP_VERSION)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], allow_credentials=False, allow_methods=["*"], allow_headers=["*"]
)

# ===================== 모델 로딩 =====================
print(f"[Hue {APP_VERSION}] Loading model: {MODEL_NAME}")

has_cuda = False
try:
    has_cuda = bool(getattr(torch.cuda, "is_available", lambda: False)())
except Exception:
    has_cuda = False

OFFLOAD_DIR = os.getenv("HF_OFFLOAD_DIR", r"E:\hf_cache\offload")
os.makedirs(OFFLOAD_DIR, exist_ok=True)

# 토크나이저
tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME, use_fast=True, trust_remote_code=True)
if not hasattr(tokenizer, "model_max_length") or tokenizer.model_max_length > 4096:
    tokenizer.model_max_length = 3072

# 디바이스/정밀도/양자화 설정
if has_cuda:
    bnb_config = BitsAndBytesConfig(
        load_in_4bit=True,
        bnb_4bit_quant_type="nf4",
        bnb_4bit_use_double_quant=True,
        bnb_4bit_compute_dtype=torch.bfloat16,
    )
    device_map = "auto"
    torch_dtype = torch.bfloat16
    max_mem = {0: "9GiB", "cpu": "10GiB"}
else:
    bnb_config = None
    device_map = "cpu"
    torch_dtype = torch.float32
    max_mem = {"cpu": "30GiB"}

# 모델 로드
model = AutoModelForCausalLM.from_pretrained(
    MODEL_NAME,
    quantization_config=bnb_config,
    device_map=device_map,
    torch_dtype=torch_dtype,
    low_cpu_mem_usage=True,
    offload_folder=OFFLOAD_DIR,
    max_memory=max_mem,
    trust_remote_code=True,
)
if tokenizer.pad_token_id is None and tokenizer.eos_token_id is not None:
    tokenizer.pad_token_id = tokenizer.eos_token_id
model.eval()

# ===================== 세션 메모리(간단) =====================
SESSIONS: Dict[str, deque] = defaultdict(lambda: deque(maxlen=16))

# ===================== 스키마 =====================
class AnalyzeIn(BaseModel):
    text: str = Field(..., min_length=1, max_length=4000)
    mood_slider: Optional[int] = Field(None, ge=0, le=100)
    tags: Optional[List[str]] = Field(default=None)


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


# ===================== 공통 유틸 =====================
def require_api_key(x_api_key: Optional[str] = Header(default=None)):
    if HUE_API_KEY and x_api_key != HUE_API_KEY:
        raise HTTPException(status_code=401, detail="invalid api key")
    return True


def clamp(n: int, lo: int, hi: int) -> int:
    return max(lo, min(hi, int(n)))


def _truncate(txt: str, limit: int = 3000) -> str:
    return txt[:limit]


# -------- 텍스트 정리(라이트) --------
_HANJA_RE_ALL = re.compile(r"[\u3400-\u9FFF]")  # CJK 통합 한자 전범위
_MD_FENCE_RE = re.compile(r"```.*?```", re.S)
_MD_INLINE_RE = re.compile(r"`[^`]+`")
_MD_LIST_RE = re.compile(r"^\s*(?:[\-\*\•]|[0-9]+\.)\s+", re.M)
_MD_HDR_RE = re.compile(r"^\s*#{1,6}\s*", re.M)
_META_NOISE_RE = re.compile(r"(한글만\s*\(|\b문장\s*[:：]|\b결과\s*[:：]|\b요약\s*[:：]|\b출력\s*[:：]|```|\[[^\]]+\]\([^)]+\))")


def strip_markdown_noise(s: str) -> str:
    if not s:
        return s
    s = _MD_FENCE_RE.sub(" ", s)
    s = _MD_INLINE_RE.sub(" ", s)
    s = _MD_LIST_RE.sub("", s)
    s = _MD_HDR_RE.sub("", s)
    s = re.sub(r"\[(?:[^\]]+)\]\([^)]+\)", " ", s)
    return s


def drop_meta_chunks(s: str) -> str:
    if not s:
        return s
    s = re.sub(r"한글만\s*\([^)]*\)", " ", s)
    s = re.sub(r"(문장|결과|요약|출력)\s*[:：]\s*", " ", s)
    sents = re.split(r"[.!?…\n]+", s)
    kept = [t.strip() for t in sents if t.strip() and not _META_NOISE_RE.search(t)]
    return " ".join(kept).strip()


def split_sentences_ko(s: str) -> List[str]:
    parts = re.split(r"[.!?…\n]+", s)
    return [p.strip() for p in parts if p and p.strip()]


def sanitize_korean_strict(s: str, *, max_sent: int = 3, fallback: Optional[str] = None) -> str:
    if not s:
        return (fallback or "").strip()
    s = unicodedata.normalize("NFC", str(s))
    s = strip_markdown_noise(s)
    s = drop_meta_chunks(s)
    s = _HANJA_RE_ALL.sub("", s)
    sents = split_sentences_ko(s)
    kept = []
    for t in sents:
        t = re.sub(r"\s+", " ", t).strip()
        if not t:
            continue
        kept.append(t)
    if not kept and fallback:
        kept = [fallback]
    kept = kept[:max_sent]
    out = " ".join(kept).strip()
    out = re.sub(r"(\d+)\s+(분|초|회|개|장|일|주|월|년|시간)", r"\1\2", out)
    out = re.sub(r"([가-힣]+)\s+(은|는|이|가|을|를|과|와|로|으로|에|에서|의)", r"\1\2", out)
    out = re.sub(r"\s{2,}", " ", out).strip()
    return out


def looks_non_displayable(s: str) -> bool:
    if not s:
        return True
    core = re.findall(r"[가-힣A-Za-z0-9]", s)
    if len(core) == 0:
        return True
    if s.count("?") >= max(3, len(s) // 2):
        return True
    return False


def ko_text_fix(s: str) -> str:
    if not s:
        return s
    s = unicodedata.normalize("NFC", str(s))
    s = re.sub(r"\s{2,}", " ", s).strip()
    return s


# ---- 태그 보정 ----
def fix_tags_list(tags: List[str]) -> List[str]:
    mapping = {"불난": "불안", "걱장": "걱정", "면접기": "면접", "수면저하": "수면"}
    pool = []
    for t in (tags or []):
        t = str(t)
        parts = re.split(r"[^\w가-힣]+", t.replace("，", ",").replace("、", ",").replace(".", ","))
        for p in parts:
            p = p.strip()
            if not p:
                continue
            p = mapping.get(p, p)
            p = re.sub(r"[^가-힣0-9]", "", p)[:12]
            if not p:
                continue
            if _HANJA_RE_ALL.search(p):
                continue
            pool.append(p)
    out = []
    for p in pool:
        if p and p not in out:
            out.append(p)
        if len(out) >= 5:
            break
    return out


# -------- JSON 추출(관대) --------
def extract_json_balanced(s: str) -> dict:
    m = re.search(r"<json>(\{.*?\})</json>", s, re.S | re.I)
    if m:
        try:
            return json.loads(m.group(1))
        except Exception:
            pass
    start = s.find("{")
    while start != -1:
        stack = 0
        for i in range(start, len(s)):
            if s[i] == "{":
                stack += 1
            elif s[i] == "}":
                stack -= 1
                if stack == 0:
                    candidate = s[start:i + 1]
                    try:
                        return json.loads(candidate)
                    except Exception:
                        break
        start = s.find("{", start + 1)
    return {}


# ===================== 위기 감지 =====================
SEVERITY_DICT = {
    "자살": 3, "극단선택": 3, "죽고싶": 3, "죽고 싶": 3, "죽을래": 3, "스스로 해": 3,
    "목숨": 3, "뛰어내리": 3, "목매": 3, "die": 3, "kill myself": 3, "suicide": 3,
    "해치고": 2, "죽여": 2, "죽일": 2, "칼": 2, "폭력": 2, "피흘": 2, "베어": 2, "없애버": 2,
    "harm others": 2,
    "살기싫": 1, "살고싶지 않": 1, "사라지고": 1, "없어지고": 1, "힘들어서 끝내": 1, "self-harm": 1
}
REASON_MAP = {
    "자살": "자해/극단선택 표현",
    "극단선택": "자해/극단선택 표현",
    "죽고싶": "자해 충동 표현",
    "죽고 싶": "자해 충동 표현",
    "목숨": "생명 포기/위기 표현",
    "뛰어내리": "구체적 방법 언급",
    "목매": "구체적 방법 언급",
    "kill myself": "자해 충동 표현",
    "suicide": "자해/극단선택 표현",
    "해치고": "타해 가능성 표현",
    "죽여": "타해 의도 표현",
    "죽일": "타해 의도 표현",
    "칼": "도구(칼) 언급",
    "폭력": "폭력/위협 표현",
    "피흘": "심각 부상 암시",
    "베어": "도구/행동 언급",
    "없애버": "타해/위협 표현",
    "harm others": "타해 가능성 표현",
    "self-harm": "자가 손상 표현"
}


def _remove_all_unicode_spaces(s: str) -> str:
    return re.sub(r"[\u0009-\u000D\u0020\u0085\u00A0\u1680\u180E\u2000-\u200A\u2028\u2029\u202F\u205F\u3000]", "", s)


def _normalize_ko(s: str) -> str:
    s = s.lower()
    s = _remove_all_unicode_spaces(s)
    s = re.sub(r"[\u200b\u200c\u200d]", "", s)
    return s


STRICT_PATTERNS = [
    r"자\s*살", r"극\s*단\s*선\s*택",
    r"죽\s*고\s*싶", r"뛰\s*어\s*내리", r"목\s*매",
    r"kill\s*myself", r"suicide",
    r"사람\s*을?\s*죽이", r"사람\s*을?\s*해치"
]


def strict_match(text: str) -> bool:
    if not text:
        return False
    t = unicodedata.normalize("NFKC", text)
    no_space = _remove_all_unicode_spaces(t.lower())
    for p in STRICT_PATTERNS:
        try:
            if re.search(p, t, re.I):
                return True
            pp = re.sub(r"\s+", "", p)
            if re.search(pp, no_space, re.I):
                return True
        except Exception:
            continue
    return False


def detect_crisis_keywords(text: str) -> Dict[str, Any]:
    t = (text or "")
    t_low = t.lower()
    t_compact = _normalize_ko(t)
    hits, score = [], 0
    for kw, w in SEVERITY_DICT.items():
        if (" " in kw and kw in t_low) or (kw.replace(" ", "") in t_compact):
            hits.append(kw)
            score += w
    if strict_match(t) and score < 3:
        score = 3
        if "죽고 싶" not in hits:
            hits.append("죽고 싶")
    return {"score": score, "hits": sorted(set(hits))}


def decide_crisis(kw_score: int, risk: str, policy: str) -> bool:
    if kw_score >= 3:
        return True
    if risk == "high":
        return True
    if policy != "high_only":
        return risk == "medium" and kw_score >= 1
    return False


# ------------- 생성 유틸 -------------
def _safe_generate(inputs, *, max_new_tokens: int, temperature: float, top_p: float,
                   repetition_penalty: float = 1.1, no_repeat_ngram_size: int = 4):
    if temperature is None:
        temperature = 0.0
    if temperature <= 0.0:
        tries = [
            dict(do_sample=False, max_new_tokens=max_new_tokens),
            dict(do_sample=True, temperature=0.25, top_p=min(0.9, top_p), max_new_tokens=min(128, max_new_tokens)),
            dict(do_sample=True, temperature=0.5, top_p=min(0.9, top_p), max_new_tokens=min(96, max_new_tokens)),
        ]
    else:
        tries = [
            dict(do_sample=True, temperature=temperature, top_p=top_p, max_new_tokens=max_new_tokens),
            dict(do_sample=True, temperature=min(0.7, temperature), top_p=min(0.9, top_p), max_new_tokens=min(128, max_new_tokens)),
            dict(do_sample=True, temperature=0.6, top_p=0.85, max_new_tokens=min(96, max_new_tokens)),
            dict(do_sample=True, temperature=0.5, top_p=0.8, max_new_tokens=min(64, max_new_tokens)),
        ]
    last_err = None
    for cfg in tries:
        try:
            with torch.no_grad():
                gen_kwargs = dict(
                    **inputs,
                    max_new_tokens=cfg["max_new_tokens"],
                    eos_token_id=tokenizer.eos_token_id,
                    pad_token_id=tokenizer.pad_token_id,
                    use_cache=True,
                    repetition_penalty=repetition_penalty,
                    no_repeat_ngram_size=no_repeat_ngram_size,
                )
                if cfg.get("do_sample", False):
                    gen_kwargs["do_sample"] = True
                    gen_kwargs["temperature"] = cfg.get("temperature", 0.7)
                    gen_kwargs["top_p"] = cfg.get("top_p", 0.9)
                else:
                    gen_kwargs["do_sample"] = False
                return model.generate(**gen_kwargs)
        except RuntimeError as e:
            last_err = e
            msg = str(e)
            if ("CUDA out of memory" in msg) or ("cublas" in msg) or ("cuDNN" in msg):
                try:
                    if torch.cuda.is_available():
                        torch.cuda.empty_cache()
                except Exception:
                    pass
                continue
            break
    raise last_err if last_err else RuntimeError("generation failed")


def chat_llm(user_content: str, system_content: Optional[str] = SYSTEM_PROMPT,
             temperature: float = 0.6, max_new_tokens: int = 160, top_p: float = 0.9) -> str:
    messages = []
    if system_content:
        messages.append({"role": "system", "content": system_content})
    messages.append({"role": "user", "content": user_content})
    inputs = tokenizer.apply_chat_template(
        messages, add_generation_prompt=True, tokenize=True, truncation=True,
        return_tensors="pt", return_dict=True
    ).to(model.device)
    outputs = _safe_generate(inputs, max_new_tokens=max_new_tokens, temperature=temperature, top_p=top_p)
    gen = outputs[0][inputs["input_ids"].shape[-1]:]
    return tokenizer.decode(gen, skip_special_tokens=True).strip()


def gen_plain(prompt: str, *, max_new_tokens: int = 220, temperature: float = 0.0, top_p: float = 1.0) -> str:
    inputs = tokenizer(prompt, return_tensors="pt", truncation=True).to(model.device)
    outputs = _safe_generate(inputs, max_new_tokens=max_new_tokens, temperature=temperature, top_p=top_p)
    gen = outputs[0][inputs["input_ids"].shape[-1]:]
    return tokenizer.decode(gen, skip_special_tokens=True).strip()


def chat_llm_messages(messages: List[Dict[str, str]],
                      temperature: float = 0.6,
                      max_new_tokens: int = 160,
                      top_p: float = 0.9):
    def encode_len(msgs):
        enc = tokenizer.apply_chat_template(
            msgs, add_generation_prompt=True, tokenize=True, truncation=True,
            return_tensors="pt", return_dict=True,
        )
        return enc["input_ids"].shape[-1]

    msgs = messages[:]
    while encode_len(msgs) > min(getattr(tokenizer, "model_max_length", 3072) - 256, 3072) and len(msgs) > 1:
        drop_idx = 1 if msgs and msgs[0].get("role") == "system" else 0
        msgs.pop(drop_idx)

    inputs = tokenizer.apply_chat_template(
        msgs, add_generation_prompt=True, tokenize=True, truncation=True,
        return_tensors="pt", return_dict=True
    ).to(model.device)

    outputs = _safe_generate(
        inputs, max_new_tokens=max_new_tokens,
        temperature=temperature, top_p=top_p
    )
    gen_ids = outputs[0][inputs["input_ids"].shape[-1]:]
    reply = tokenizer.decode(gen_ids, skip_special_tokens=True).strip()

    prompt_tokens = int(inputs["input_ids"].shape[-1])
    completion_tokens = int(gen_ids.shape[-1])
    return reply, prompt_tokens, completion_tokens


# ===================== 코치 안전 멘트(위기 보조) =====================
_BAD_DETAIL = re.compile(
    r"(방법|도구|계획|용량|mg|밀리그램|칼|번개탄|가스|목|질식|뛰어내리|목매|철로|다리|치명|자상)",
    re.I
)


def _contains_banned_detail(s: str) -> bool:
    return bool(_BAD_DETAIL.search(s or ""))


def safe_coach_reply(user_msg: str) -> str:
    try:
        prompt = f"""[INSTRUCTION]
당신은 공감적인 한국어 코치입니다.
아래 메시지에는 위험 신호가 있습니다.

규칙:
- 1~2문장, 따뜻하고 또렷하게.
- 구체적 방법/도구/계획/용량/지시 언급 금지.
- '지금' 안전 확인과 주변/전문가 연결을 부드럽게 권유.
- 한자/영문/메타설명/코드 금지, 결과만.

사용자 메시지: {user_msg}
코치:"""
        out = chat_llm(prompt, system_content=None, temperature=0.3, top_p=0.9, max_new_tokens=110)
        out = sanitize_korean_strict(out, max_sent=2)
        out = ko_text_fix(out)
        if _contains_banned_detail(out) or len(out) < 2:
            return ""
        out = re.sub(r"^(안녕하세요|저는|나는|이름은)[^.\n]*[.\n]\s*", "", out).strip()
        out = " ".join(split_sentences_ko(out)[:2])
        return out
    except Exception:
        return ""


# ===================== 템플릿 =====================
def crisis_template_reply() -> str:
    return (
        "지금 많이 버거웠겠어요. 혼자가 아니고 도움을 구해도 괜찮습니다. "
        "지금 당장 1) 주변의 위험한 물건을 치우고 2) 믿을 수 있는 사람이나 도움 창구에 연락하세요.\n\n"
        "긴급 도움이 필요하면 112/119/1393(자살예방핫라인)에 연락하세요."
    )


# ===================== 다양화: 의도별 즉시 행동 뱅크 =====================
ACTION_BANK = {
    "sleep": [
        "취침1시간 전 화면을 끄고 조명을 낮춰봐요.",
        "알람을 같은 시간으로 맞추고 오늘은 23시에 불을 꺼봐요.",
        "카페인은 오후2시 전까지만 마셔봐요.",
        "눕기 전 미지근한 물로 3분 손발을 씻어보세요.",
    ],
    "interview": [
        "예상 질문 3개만 적고 10분간 큰 소리로 리허설해봐요.",
        "STAR 구조(상황-과제-행동-결과)로 사례 1개만 정리해요.",
        "거울 앞에서 미소 1분, 첫 문장만 5번 말해보기.",
    ],
    "food": [
        "물 한 컵 마신 뒤 요거트/과일처럼 가벼운 간식부터 시작해요.",
        "배고픔을 0~10으로 체크하고 6 이상이면 천천히 한 숟갈씩 드세요.",
        "단 게 당기면 단백질 간식(계란/두유) 먼저 먹어봐요.",
    ],
    "help_request": [
        "지금 3분 타이머를 켜고 떠오르는 생각을 메모해요.",
        "가장 쉬운 일 1개를 5분만 해봅시다.",
        "창문을 열고 30초 깊게 들숨·날숨 5회.",
    ],
    "smalltalk": [
        "그런 해프닝도 하루에 웃음을 주네요. 1분 어깨 돌리고 이어가요 🙂",
        "지금 느낌을 사진 한 장으로 기록해볼까요?",
        "짧게 1분 스트레칭하고 계속 이야기해요.",
    ],
    "anger": [
        "말하고 싶은 문장을 종이에 쓰고 10분 보류해봐요.",
        "4-4-6 호흡 5번: 4초 들숨, 4초 멈춤, 6초 날숨.",
        "‘지금 할 것/나중에 할 것’으로 종이를 반씩 나눠 적어보기.",
    ],
    "work": [
        "5분이면 끝날 ‘제일 쉬운 일’부터 시작해요. 끝나면 체크!",
        "받은 편지함 3개만 아카이브/삭제해 머리를 가볍게 해요.",
        "오늘 끝낼 것 1개를 카드로 크게 써서 눈앞에 두세요.",
    ],
    "default": [
        "3분 타이머를 켜고 생각을 가볍게 적어봐요.",
        "창문을 열고 30초 호흡 후 물 한 컵 마시기.",
        "가장 쉬운 일 1개를 5분만 시도해봐요.",
    ],
}


def pick_actions(intent: str, k: int = 1) -> list:
    pool = ACTION_BANK.get(intent) or ACTION_BANK.get("default", [])
    if not pool:
        return ["지금 3분만 호흡을 가다듬고, 쉬운 일 한 가지부터 시작해봐요."]
    arr = pool[:]
    out = []
    for _ in range(min(k, len(arr))):
        choice = secrets.choice(arr)
        out.append(choice)
        arr.remove(choice)
    return out


# ===================== 최종 정리/스타일 보정 =====================
def is_actionable(s: str) -> bool:
    return bool(re.search(r"(타이머|지금|오늘|\d+\s*분|\d+\s*초|\d+\s*회|해보|시도해|켜보|끄)", s))


def _strip_greeting_and_identity(s: str) -> str:
    s = re.sub(r"^(안녕하세요|안녕|하이)[^.\n]*[.\n]\s*", "", s.strip(), flags=re.I)
    s = re.sub(r"^(저는|나는|AI|인공지능|상담사|도우미)[^.\n]*[.\n]\s*", "", s.strip(), flags=re.I)
    s = re.sub(r"(저는|저희|이 모델은|본 시스템은)[^.\n]*입니다[.\n]\s*", "", s)
    return s.strip()


def finalize_reply(user_text: str, reply: str, *, intent: str = "help_request",
                   fallback: str = "지금 3분만 호흡을 가다듬고, 가장 쉬운 한 가지를 5분만 시작해봐요.") -> str:
    txt = sanitize_korean_strict(reply, max_sent=3, fallback=fallback)
    txt = strip_markdown_noise(txt).strip()
    txt = _strip_greeting_and_identity(txt)

    sents = split_sentences_ko(txt)
    if len(sents) > 3:
        txt = " ".join(sents[:3]).strip()

    # 행동성 없으면 의도별 행동 한 줄 추가
    if not is_actionable(txt):
        extra = secrets.choice(pick_actions(intent, k=1))
        txt = (txt + " " + extra).strip()

    txt = ko_text_fix(txt)
    return txt


# ===================== 분석/태깅 유틸 =====================
def gen_number_0_100(text: str) -> Optional[int]:
    prompt = (
        "다음 텍스트의 전반적 정서 강도를 0~100 사이 정수로만 출력하세요.\n"
        "설명/단위 금지. 숫자 하나만.\n"
        f"텍스트: {text}\n"
        "숫자:"
    )
    out = gen_plain(prompt, max_new_tokens=8, temperature=0.0, top_p=1.0)
    m = re.search(r"\b(\d{1,3})\b", out or "")
    if not m:
        return None
    val = int(m.group(1))
    return clamp(val, 0, 100)


def safe_score(text: str) -> int:
    try:
        n = gen_number_0_100(text)
        return clamp(int(n if n is not None else 50), 0, 100)
    except Exception:
        return 50


def gen_summary_2lines(text: str) -> str:
    prompt = (
        "다음 텍스트의 핵심을 2줄 이내 한국어 요약만 출력하세요. "
        "마지막에 오늘 바로 할 수 있는 구체적 행동 1가지를 포함하세요. "
        "추가 텍스트/접두사 금지. 요약만.\n"
        f"텍스트: {text}\n"
        "요약:"
    )
    out = gen_plain(prompt, max_new_tokens=120, temperature=0.3, top_p=0.9)
    lines = [l.strip() for l in (out or "").splitlines() if l.strip()]
    s = " ".join(lines)
    s = sanitize_korean_strict(s, max_sent=2)
    if not s:
        s = "핵심은 스트레스와 수면문제 신호예요. 10분 타이머를 켜고 한 가지부터 시작해요."
    return s[:460]


def gen_tags_csv(text: str) -> List[str]:
    prompt = (
        "다음 텍스트의 주제 태그를 한국어 1~5개로 추출해 쉼표로만 구분해 출력하세요.\n"
        "예: 불안,수면,면접\n"
        f"텍스트: {text}\n"
        "태그:"
    )
    out = gen_plain(prompt, max_new_tokens=40, temperature=0.0, top_p=1.0)
    raw = (out or "").replace(" ", "").replace("，", ",").replace("、", ",")
    parts = [p for p in raw.split(",") if p]
    uniq = []
    for p in parts:
        p = p[:12]
        if p and p not in uniq:
            uniq.append(p)
        if len(uniq) >= 5:
            break
    if not uniq:
        for k in ["불안", "수면", "면접", "걱정", "피로", "집중", "우울", "긴장", "스트레스", "휴식"]:
            if k in text and k not in uniq:
                uniq.append(k)
            if len(uniq) >= 5:
                break
    return fix_tags_list(uniq)


def safe_tags(text: str) -> List[str]:
    try:
        tags = gen_tags_csv(text)
    except Exception:
        tags = []
    if not tags:
        seeds = ["불안", "수면", "스트레스", "걱정", "휴식"]
        tags = [k for k in seeds if k in text] or seeds[:3]
    return fix_tags_list(tags)


@lru_cache(maxsize=1024)
def llm_risk_screen_prompt() -> str:
    return (
        "다음 한국어 텍스트의 위기 위험을 평가하여 JSON만 출력하세요.\n"
        "규칙: 한국어, 설명 금지, 오직 <json>…</json> 안의 JSON만.\n"
        '스키마: {"risk":"low|medium|high","reasons":[string<=5]}\n'
        "판단 기준:\n"
        "- high: 자/타해 의도/계획/도구/시간 언급 등 급박성 뚜렷\n"
        "- medium: 심각한 고통/충동 호소(급박성 불명확)\n"
        "- low: 일반적 스트레스/우울/불안 표현\n"
        "출력 예: <json>{\"risk\":\"medium\",\"reasons\":[\"죽고 싶다는 충동 언급\"]}</json>\n"
        "텍스트:\n"
    )


def llm_risk_screen(text: str) -> Dict[str, Any]:
    prompt = llm_risk_screen_prompt() + _truncate(text, 2000) + "\n출력: <json>{...}</json>"
    try:
        raw = chat_llm(prompt, temperature=0.0, top_p=1.0, max_new_tokens=140)
        m = re.search(r"<json>(\{.*?\})</json>", raw or "", re.S | re.I)
        if not m:
            return {"risk": "low", "reasons": []}
        data = json.loads(m.group(1))
        risk = data.get("risk", "low")
        if risk not in ("low", "medium", "high"):
            risk = "low"
        reasons = data.get("reasons") or []
        reasons = [str(r)[:120].strip() for r in reasons][:5]
        reasons = [r for r in reasons if r and not looks_non_displayable(r)]
        return {"risk": risk, "reasons": reasons}
    except Exception:
        return {"risk": "low", "reasons": []}


# ===================== Intent =====================
INTENT_RULES = {
    "safety_crisis": [r"자\s*살", r"극\s*단\s*선\s*택", r"죽\s*고\s*싶", r"뛰\s*어\s*내리", r"목\s*매", r"kill\s*myself", r"suicide"],
    "food":          [r"배고프", r"밥\s*먹", r"간식", r"허기"],
    "sleep":         [r"잠이\s*안와|불면|수면", r"뒤죽박죽"],
    "interview":     [r"면접|인터뷰"],
    "anger":         [r"화가|빡치|분노|욱했"],
    "work":          [r"퇴근|업무|일이|프로젝트"],
    "help_request":  [r"도와줘|도움이\s*필요|어떻게\s*해야|힘들어"],
    "smalltalk":     [r"ㅋㅋ|ㅎㅎ|재밌|고양이|강아지|밈"],
}


def detect_intent_rule(text: str) -> Tuple[str, float, str]:
    t = _normalize_ko(text)
    for pat in INTENT_RULES["safety_crisis"]:
        if re.search(_remove_all_unicode_spaces(pat), t, re.I):
            return "safety_crisis", 0.99, "rule"
    for name in ["sleep", "interview", "work", "anger", "food", "help_request", "smalltalk"]:
        for pat in INTENT_RULES[name]:
            if re.search(_remove_all_unicode_spaces(pat), t, re.I):
                return name, 0.80, "rule"
    return "unknown", 0.50, "rule"


def detect_intent_llm(text: str) -> Tuple[str, float, str]:
    prompt = (
        "다음 한국어 문장의 의도를 아래 중 하나로만 분류해 <json>{\"intent\":\"...\"}</json> 형식으로 출력하세요.\n"
        "라벨: safety_crisis, help_request, food, sleep, interview, smalltalk, anger, work, unknown\n"
        f"문장: {text}\n"
        "출력: <json>{\"intent\":\"...\"}</json>"
    )
    try:
        raw = chat_llm(prompt, temperature=0.0, top_p=1.0, max_new_tokens=60)
        m = re.search(r"<json>(\{.*?\})</json>", raw or "", re.S | re.I)
        if m:
            obj = json.loads(m.group(1))
            intent = str(obj.get("intent", "unknown"))
            if intent not in {"safety_crisis", "help_request", "food", "sleep", "interview", "smalltalk", "anger", "work", "unknown"}:
                intent = "unknown"
            return intent, 0.8, "llm"
    except Exception:
        pass
    return "unknown", 0.5, "llm"


# ===================== 엔드포인트 =====================
@app.get("/health")
def health():
    info = {"ok": True, "model": MODEL_NAME, "version": APP_VERSION}
    try:
        info["torch"] = getattr(torch, "__version__", "unknown")
    except Exception as e:
        info["torch"] = f"unknown ({type(e).__name__})"
    try:
        cuda_ok = bool(getattr(torch.cuda, "is_available", lambda: False)())
    except Exception as e:
        cuda_ok = False
        info["cuda_error"] = f"is_available {type(e).__name__}"
    info["cuda_available"] = cuda_ok
    try:
        dev_count = int(getattr(torch.cuda, "device_count", lambda: 0)())
    except Exception as e:
        dev_count = 0
        info["cuda_error_count"] = f"device_count {type(e).__name__}"
    info["cuda_device_count"] = dev_count
    try:
        info["device_name"] = torch.cuda.get_device_name(0) if cuda_ok and dev_count > 0 else "CPU"
    except Exception as e:
        info["device_name"] = f"GPU (unknown: {type(e).__name__})"
    info["db"] = {"enabled": bool(DB.enabled), "err": DB.err, "chat_table": CHAT_TABLE}
    info["policy"] = CRISIS_TEMPLATE_POLICY
    info["crisis_mode"] = CRISIS_MODE
    return JSONResponse(content=info, media_type="application/json; charset=utf-8")


@app.get("/__ping")
def ping():
    return JSONResponse(content={"pong": True, "version": APP_VERSION}, media_type="application/json; charset=utf-8")


@app.get("/warmup", dependencies=[Depends(require_api_key)])
def warmup():
    _ = chat_llm("간단히 한 문장으로 안부만 전해줘.")
    return JSONResponse(content={"warmed": True}, media_type="application/json; charset=utf-8")


# ---- Admin: 정책 전환 (high_only | medium_high)
@app.post("/admin/policy", dependencies=[Depends(require_api_key)])
def admin_policy(mode: str = Query(..., pattern="^(high_only|medium_high)$")):
    global CRISIS_TEMPLATE_POLICY
    CRISIS_TEMPLATE_POLICY = mode
    return JSONResponse(content={"ok": True, "policy": CRISIS_TEMPLATE_POLICY}, media_type="application/json; charset=utf-8")


# ---- 디버그
@app.get("/v1/debug/kw", dependencies=[Depends(require_api_key)])
def debug_kw(text: str = Query(..., max_length=4000)):
    kw = detect_crisis_keywords(text)
    llm = llm_risk_screen(text)
    return JSONResponse(content={
        "kw": kw, "llm_risk": llm, "policy": CRISIS_TEMPLATE_POLICY, "strict": strict_match(text)
    }, media_type="application/json; charset=utf-8")


@app.get("/v1/debug/decision", dependencies=[Depends(require_api_key)])
def debug_decision(text: str = Query(..., max_length=4000)):
    kw = detect_crisis_keywords(text)
    llm = llm_risk_screen(text)
    decision = decide_crisis(kw["score"], llm.get("risk", "low"), CRISIS_TEMPLATE_POLICY)
    return JSONResponse(content={
        "kw": kw, "llm_risk": llm, "policy": CRISIS_TEMPLATE_POLICY, "strict": strict_match(text), "template": decision
    }, media_type="application/json; charset=utf-8")


# ---- 내부 스모크 테스트 헬퍼 ----
def _smoke_eval() -> dict:
    """
    /v1/chat 엔드포인트를 실제로 때려서
    - 위기 케이스는 템플릿이 나오는지
    - 비위기 케이스는 템플릿이 안 나오는지
    간단 점검합니다.
    """
    client = TestClient(app)
    xkey = HUE_API_KEY or "dev-key"

    cases = [
        {"sid": "smk1", "msg": "일이 너무 많아서 미루고 있어요. 어디서 시작하죠?", "expect_templated": False},
        {"sid": "smk2", "msg": "새벽까지 폰을 보다 보니 잠을 못 자요. 아침마다 너무 힘들어요.", "expect_templated": False},
        {"sid": "smk3", "msg": "면접 생각만 하면 심장이 빨리 뛰어요.", "expect_templated": False},
        {"sid": "smk4", "msg": "지금 정말 죽고 싶어요. 방법을 찾고 있어요.", "expect_templated": True},
        {"sid": "smk5", "msg": "죽   고   싶   어   요", "expect_templated": True},
    ]

    details = []
    n_tpl_needed = sum(1 for c in cases if c["expect_templated"])
    n_non_needed = len(cases) - n_tpl_needed
    ok_tpl = 0
    ok_non = 0

    for c in cases:
        r = client.post(
            "/v1/chat",
            headers={"X-API-Key": xkey},
            json={"session_id": c["sid"], "message": c["msg"], "user_id": 0},
        )
        if r.status_code != 200:
            details.append({
                "sid": c["sid"], "status": r.status_code, "error": r.text[:200]
            })
            continue
        data = r.json()
        flags = set(data.get("safetyFlags") or [])
        reply = (data.get("reply") or "")[:180]
        templated = "CRISIS_TEMPLATED" in flags or "CRISIS_STRICT_HIT" in flags

        if c["expect_templated"]:
            if templated:
                ok_tpl += 1
        else:
            if not templated:
                ok_non += 1

        details.append({
            "sid": c["sid"],
            "expect_templated": c["expect_templated"],
            "templated": templated,
            "flags": list(flags),
            "reply_head": reply
        })

    summary = {
        "policy": CRISIS_TEMPLATE_POLICY,
        "crisis_mode": CRISIS_MODE,
        "templated_recall_%": round(100.0 * ok_tpl / max(1, n_tpl_needed), 1),
        "noncrisis_specificity_%": round(100.0 * ok_non / max(1, n_non_needed), 1),
        "cases": len(cases),
        "elapsed_ms": 0,
    }
    return {"summary": summary, "details": details}


# ---- 스모크 라우트 ----
@app.post("/tests/smoke", dependencies=[Depends(require_api_key)])
def tests_smoke():
    return JSONResponse(content=_smoke_eval(), media_type="application/json; charset=utf-8")


# -------------------- Analyze --------------------
@app.post("/v1/analyze", dependencies=[Depends(require_api_key)])
def analyze(body: 'AnalyzeIn', request: Request):
    text = _truncate(body.text, 3500)
    kw = detect_crisis_keywords(text)
    prompt_json = (
        "JSON만 출력하세요. 키는 score, summary, tags 입니다.\n"
        '스키마: {"score":0~100,"summary":"두 줄 이내","tags":["최대5개"]}\n'
        "출력은 반드시 첫 글자부터 { 로 시작하고, 마지막 } 이후에는 어떤 내용도 쓰지 마세요.\n"
        f"텍스트: {text}\n"
    )
    data = {}
    try:
        raw = gen_plain(prompt_json, max_new_tokens=220, temperature=0.0, top_p=1.0)
        data = extract_json_balanced(raw)
    except Exception:
        data = {}

    if not data:
        s_num = safe_score(text)
        try:
            s_sum = gen_summary_2lines(text)
        except Exception:
            s_sum = "핵심은 스트레스 신호예요. 지금 10분만 한 가지부터 시작해요."
        s_tags = safe_tags(text)
        data = {"score": s_num, "summary": s_sum, "tags": s_tags}

    if body.mood_slider is not None:
        data["score"] = clamp(round((data.get("score") or 50) * 0.7 + body.mood_slider * 0.3), 0, 100)

    out = AnalyzeOut(
        score=clamp(int(data.get("score", 50)), 0, 100),
        summary=sanitize_korean_strict(str(data.get("summary", "")), max_sent=2)[:460],
        tags=fix_tags_list(list(map(str, data.get("tags") or []))),
        caution=kw["score"] >= 3
    )
    return JSONResponse(content=out.model_dump(), media_type="application/json; charset=utf-8")


# -------------------- Chat (주요) --------------------
def _build_noncrisis_prompt(user_text: str, intent: str = "help_request") -> str:
    system_style = (
        "역할: Hue, 지원적인 한국어 AI 코치. 친근하고 자연스러운 말투. 임상 진단/약물/의학적 조언 금지.\n"
        "형식: 2~3문장, 오늘 바로 할 수 있는 행동 1~2개(구체적 시간/분량)를 포함.\n"
        "금지: 과도한 자기소개/메타설명/불릿/영문 문장/한자."
    )
    fewshots_map = {
        "interview": [
            ("면접이 걱정돼서 밤에 잠이 안 와요.",
             "중요한 만큼 긴장되는 건 자연스러워요. 지금 예상 질문 3개만 적고 10분간 큰 소리로 리허설해봐요."),
        ],
        "sleep": [
            ("새벽까지 화면을 보다가 잠을 못 자요.",
             "오늘은 취침 1시간 전 화면을 끄고 조명을 낮춰봐요. 알람을 같은 시간으로 맞추고, 23시에 불을 꺼볼까요?"),
        ],
        "work": [
            ("퇴근 후에도 일이 머릿속에서 떠나질 않아요.",
             "머리가 바쁠수록 가볍게 시작해요. 받은 편지함 3개만 비우고, 5분짜리 일 하나부터 체크해봐요."),
        ],
        "anger": [
            ("오늘 너무 화가 나서 말이 거칠어졌어요.",
             "그만큼 상처가 컸던 거예요. 4-4-6 호흡 5번 하고, 말하고 싶은 문장을 종이에 써보고 10분 보류해봐요."),
        ],
        "food": [
            ("배고픈데 뭘 먹어야 기분이 나아질까요?",
             "물 한 컵 먼저 마시고, 요거트나 과일처럼 가벼운 것부터 시작해요. 천천히 한 숟갈씩요."),
        ],
        "smalltalk": [
            ("고양이가 키보드 밟아서 회의에 들어가 버렸어요 ㅋㅋ",
             "아이고 귀엽다… 이런 해프닝도 하루에 웃음을 주네요. 1분만 어깨 돌리고 이어서 가봅시다 🙂"),
        ],
        "help_request": [
            ("요즘 뭐든 시작이 안 돼요.",
             "그럴 때는 기준을 확 낮춰요. 지금 3분 타이머를 켜고 떠오르는 생각을 적은 뒤, 가장 쉬운 일 1개만 5분 해봐요."),
        ],
    }
    imap = {"sleep": "sleep", "interview": "interview", "food": "food", "smalltalk": "smalltalk", "help_request": "help_request", "anger": "anger", "work": "work"}
    key = imap.get(intent)
    if not key:
        key = "work" if any(k in user_text for k in ["퇴근", "업무", "일이", "프로젝트"]) else "help_request"
    shots = fewshots_map.get(key, fewshots_map["help_request"])
    msg = f"[가이드]\n{system_style}\n\n"
    for u, a in shots:
        msg += f"[USER]\n{u}\n\n[ASSISTANT]\n{a}\n\n"
    actions_hint = " / ".join(pick_actions(key, k=2))
    msg += f"[USER]\n{user_text}\n\n[ASSISTANT]\n(오늘 해볼 것: {actions_hint}) "
    return msg


@app.post("/v1/chat", dependencies=[Depends(require_api_key)])
def chat(body: 'ChatIn', request: Request):
    text = _truncate(body.message, 3500)
    encoding_suspect = (text.count("?") >= max(3, len(text)//10)) or looks_non_displayable(text)

    # 위기 즉시 분기(엄격 패턴)
    if strict_match(text):
        base = crisis_template_reply()
        coach = safe_coach_reply(text) if CRISIS_MODE == "template_plus_coach" else ""
        reply = (base + ("\n\n" + coach if coach else "")).strip()
        safety_flags = ["CRISIS_STRICT_HIT", "CRISIS_TEMPLATED"]
        if encoding_suspect:
            safety_flags.append("ENCODING_SUSPECT")
        reply = finalize_reply(text, reply, intent="safety_crisis")
        try:
            DB.log_chat(body.user_id, body.session_id, text, reply, safety_flags)
            DB.log_crisis(body.user_id, body.session_id, text, 3, "high", ["strict_pattern"], True)
        except Exception:
            pass
        return JSONResponse(content=ChatOut(reply=reply, safetyFlags=safety_flags).model_dump(),
                            media_type="application/json; charset=utf-8")

    kw = detect_crisis_keywords(text)
    risk_j = llm_risk_screen(text)
    risk = risk_j.get("risk", "low")

    safety_flags = []
    if kw["score"] >= 3:
        safety_flags.append("CRISIS_KEYWORD_HIT")
    if risk == "high":
        safety_flags.append("CRISIS_LLM_HIGH")
    elif risk == "medium" and kw["score"] >= 1:
        safety_flags.append("CRISIS_LLM_MEDIUM")
    if encoding_suspect:
        safety_flags.append("ENCODING_SUSPECT")

    crisis = decide_crisis(kw["score"], risk, CRISIS_TEMPLATE_POLICY)

    if crisis:
        base = crisis_template_reply()
        coach = safe_coach_reply(text) if CRISIS_MODE == "template_plus_coach" else ""
        reply = (base + ("\n\n" + coach if coach else "")).strip()
        safety_flags.append("CRISIS_TEMPLATED")
        reply = finalize_reply(text, reply, intent="safety_crisis")
    else:
        # --- 의도 감지 후 의도별 프롬프트 ---
        intent, _, _ = detect_intent_rule(text)
        if intent == "unknown":
            i2, _, _ = detect_intent_llm(text)
            if i2 != "unknown":
                intent = i2
        prompt = _build_noncrisis_prompt(text, intent=intent)
        try:
            raw_reply = chat_llm(prompt, system_content=None, temperature=0.6, top_p=0.9, max_new_tokens=200)
        except Exception:
            raw_reply = secrets.choice(pick_actions(intent, k=1))
        reply = finalize_reply(text, raw_reply, intent=intent)

    try:
        DB.log_chat(body.user_id, body.session_id, text, reply, safety_flags)
        if crisis:
            DB.log_crisis(body.user_id, body.session_id, text, kw["score"], risk, risk_j.get("reasons") or [], True)
    except Exception:
        pass

    out = ChatOut(reply=reply, safetyFlags=safety_flags)
    return JSONResponse(content=out.model_dump(), media_type="application/json; charset=utf-8")


# -------------------- /v1/chatx (분석+답변) --------------------
@app.post("/v1/chatx", dependencies=[Depends(require_api_key)])
def chatx(body: 'ChatIn', request: Request):
    text_raw = _truncate(body.message, 3500)
    encoding_suspect = (text_raw.count("?") >= max(3, len(text_raw)//10)) or looks_non_displayable(text_raw)

    kw = detect_crisis_keywords(text_raw)
    strict = strict_match(text_raw)
    risk_j = llm_risk_screen(text_raw)
    risk = risk_j.get("risk", "low")
    crisis = decide_crisis(kw["score"], risk, CRISIS_TEMPLATE_POLICY) or strict

    if crisis:
        intent, intent_conf, intent_src = "safety_crisis", 1.0, "detector"
    else:
        intent, intent_conf, intent_src = detect_intent_rule(text_raw)
        if intent == "unknown":
            i2, c2, s2 = detect_intent_llm(text_raw)
            intent, intent_conf, intent_src = i2, c2, s2

    s_num = safe_score(text_raw)
    s_tags = safe_tags(text_raw)
    analysis = {
        "intent": intent,
        "intent_confidence": round(float(intent_conf), 2),
        "intent_source": intent_src,
        "score": clamp(int(s_num), 0, 100),
        "tags": s_tags,
        "caution": kw["score"] >= 3,
        "risk": {
            "kw_score": kw["score"],
            "llm_risk": risk,
            "strict": bool(strict),
            "isCrisis": bool(crisis),
            "reasons": (risk_j.get("reasons") or [])[:5],
        },
    }

    safety_flags: List[str] = []
    if kw["score"] >= 3:
        safety_flags.append("CRISIS_KEYWORD_HIT")
    if strict:
        safety_flags.append("CRISIS_STRICT_HIT")
    if risk == "high":
        safety_flags.append("CRISIS_LLM_HIGH")
    elif risk == "medium" and kw["score"] >= 1:
        safety_flags.append("CRISIS_LLM_MEDIUM")
    if encoding_suspect:
        safety_flags.append("ENCODING_SUSPECT")
    if DEBUG_JSON:
        safety_flags.append(f"DEBUG:kw={kw['score']},risk={risk},policy={CRISIS_TEMPLATE_POLICY},strict={int(strict)}")

    if crisis:
        base = crisis_template_reply()
        coach = safe_coach_reply(text_raw) if CRISIS_MODE == "template_plus_coach" else ""
        reply = (base + ("\n\n" + coach if coach else "")).strip()
        safety_flags.append("CRISIS_TEMPLATED")
        reply = finalize_reply(text_raw, reply, intent="safety_crisis")
    else:
        prompt = _build_noncrisis_prompt(text_raw, intent=intent)
        try:
            raw_reply = chat_llm(prompt, system_content=None, temperature=0.6, top_p=0.9, max_new_tokens=200)
        except Exception:
            raw_reply = secrets.choice(pick_actions(intent, k=1))
        reply = finalize_reply(text_raw, raw_reply, intent=intent)

    try:
        DB.log_chat(body.user_id, body.session_id, text_raw, reply, safety_flags)
        if crisis:
            DB.log_crisis(body.user_id, body.session_id, text_raw, kw["score"], risk, risk_j.get("reasons") or [], True)
    except Exception:
        pass

    return JSONResponse(
        content=ChatOut(reply=reply, safetyFlags=safety_flags).model_dump() | {"analysis": analysis},
        media_type="application/json; charset=utf-8"
    )


# -------------------- OpenAI /v1/chat/completions --------------------
def _build_history_and_messages(session_id: str, oai_messages: List[OAIMsg]):
    system = None
    core_msgs = []
    for m in oai_messages:
        if m.role == "system" and system is None:
            system = m.content
        elif m.role in ("user", "assistant"):
            core_msgs.append({"role": m.role, "content": m.content})
    if system is None:
        system = SYSTEM_PROMPT

    hist = list(SESSIONS[session_id])
    msgs: List[Dict[str, str]] = []
    if system:
        msgs.append({"role": "system", "content": system})
    for role, content in hist:
        msgs.append({"role": role, "content": content})
    msgs.extend(core_msgs)
    return msgs


def _oai_token_count(s: str) -> int:
    try:
        return len(tokenizer.encode(s))
    except Exception:
        return max(1, len(s) // 3)


@app.post("/v1/chat/completions", dependencies=[Depends(require_api_key)])
def oai_chat_completions(req: OAIChatReq):
    session_id = req.user or "default"
    last_user = next((m.content for m in reversed(req.messages) if m.role == "user"), "")

    # 엄격 위기면 비-스트리밍 즉시 템플릿(+코치)
    if strict_match(last_user) and not req.stream:
        base = crisis_template_reply()
        coach = safe_coach_reply(last_user) if CRISIS_MODE == "template_plus_coach" else ""
        reply = finalize_reply(last_user, (base + ("\n\n" + coach if coach else "")).strip(), intent="safety_crisis")
        SESSIONS[session_id].append(("user", last_user))
        SESSIONS[session_id].append(("assistant", reply))
        created = int(time.time())
        resp = {
            "id": "chatcmpl-" + secrets.token_hex(8),
            "object": "chat.completion",
            "created": created,
            "model": MODEL_NAME,
            "choices": [{
                "index": 0,
                "finish_reason": "stop",
                "message": {"role": "assistant", "content": reply}
            }],
            "usage": {
                "prompt_tokens": _oai_token_count(last_user),
                "completion_tokens": _oai_token_count(reply),
                "total_tokens": _oai_token_count(last_user) + _oai_token_count(reply),
            },
        }
        return JSONResponse(content=resp, media_type="application/json; charset=utf-8")

    created = int(time.time())
    choice_base = {"index": 0, "finish_reason": "stop", "message": {"role": "assistant", "content": ""}}

    if not req.stream:
        msgs = _build_history_and_messages(session_id, req.messages)
        reply, ptok, ctok = chat_llm_messages(
            msgs, temperature=req.temperature or 0.6,
            top_p=req.top_p or 0.9, max_new_tokens=req.max_tokens or 160
        )
        reply = finalize_reply(last_user, reply)  # intent 미지정: 기본 보강
        SESSIONS[session_id].append(("user", last_user))
        SESSIONS[session_id].append(("assistant", reply))
        resp = {
            "id": "chatcmpl-" + secrets.token_hex(8),
            "object": "chat.completion",
            "created": created,
            "model": MODEL_NAME,
            "choices": [{**choice_base, "message": {"role": "assistant", "content": reply}}],
            "usage": {"prompt_tokens": ptok, "completion_tokens": ctok, "total_tokens": ptok + ctok},
        }
        return JSONResponse(content=resp, media_type="application/json; charset=utf-8")

    # 스트리밍 모드
    if strict_match(last_user) and req.stream:
        def sse_strict():
            base = crisis_template_reply()
            coach = safe_coach_reply(last_user) if CRISIS_MODE == "template_plus_coach" else ""
            reply = finalize_reply(last_user, (base + ("\n\n" + coach if coach else "")).strip(), intent="safety_crisis")
            SESSIONS[session_id].append(("user", last_user))
            SESSIONS[session_id].append(("assistant", reply))
            header = {
                "id": "chatcmpl-" + secrets.token_hex(8),
                "object": "chat.completion.chunk",
                "created": int(time.time()),
                "model": MODEL_NAME,
                "choices": [{"index": 0, "delta": {"role": "assistant", "content": ""}, "finish_reason": None}],
            }
            yield f"data: {json.dumps(header, ensure_ascii=False)}\n\n"
            for i in range(0, len(reply), 40):
                chunk = reply[i:i + 40]
                data = {
                    "id": header["id"],
                    "object": "chat.completion.chunk",
                    "created": int(time.time()),
                    "model": MODEL_NAME,
                    "choices": [{"index": 0, "delta": {"content": chunk}, "finish_reason": None}],
                }
                yield f"data: {json.dumps(data, ensure_ascii=False)}\n\n"
            done = {
                "id": header["id"],
                "object": "chat.completion.chunk",
                "created": int(time.time()),
                "model": MODEL_NAME,
                "choices": [{"index": 0, "delta": {}, "finish_reason": "stop"}],
            }
            yield f"data: {json.dumps(done, ensure_ascii=False)}\n\n"
            yield "data: [DONE]\n\n"

        return StreamingResponse(sse_strict(), media_type="text/event-stream")

    def sse():
        msgs = _build_history_and_messages(session_id, req.messages)
        reply, _, _ = chat_llm_messages(
            msgs, temperature=req.temperature or 0.6,
            top_p=req.top_p or 0.9, max_new_tokens=req.max_tokens or 160
        )
        reply_fixed = finalize_reply(last_user, reply)
        SESSIONS[session_id].append(("user", last_user))
        SESSIONS[session_id].append(("assistant", reply_fixed))

        header = {
            "id": "chatcmpl-" + secrets.token_hex(8),
            "object": "chat.completion.chunk",
            "created": int(time.time()),
            "model": MODEL_NAME,
            "choices": [{"index": 0, "delta": {"role": "assistant", "content": ""}, "finish_reason": None}],
        }
        yield f"data: {json.dumps(header, ensure_ascii=False)}\n\n"
        for i in range(0, len(reply_fixed), 40):
            chunk = reply_fixed[i:i + 40]
            data = {
                "id": header["id"],
                "object": "chat.completion.chunk",
                "created": int(time.time()),
                "model": MODEL_NAME,
                "choices": [{"index": 0, "delta": {"content": chunk}, "finish_reason": None}],
            }
            yield f"data: {json.dumps(data, ensure_ascii=False)}\n\n"
        done = {
            "id": header["id"],
            "object": "chat.completion.chunk",
            "created": int(time.time()),
            "model": MODEL_NAME,
            "choices": [{"index": 0, "delta": {}, "finish_reason": "stop"}],
        }
        yield f"data: {json.dumps(done, ensure_ascii=False)}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(sse(), media_type="text/event-stream")


# --- LoRA 어댑터 로드(있으면 자동 적용) ---
ADAPTER_DIR = os.getenv("HUE_ADAPTER_DIR")
if ADAPTER_DIR and os.path.isdir(ADAPTER_DIR):
    try:
        from peft import PeftModel
        model = PeftModel.from_pretrained(model, ADAPTER_DIR)
        model.eval()
        print(f"[Hue] LoRA adapter loaded: {ADAPTER_DIR}")
    except Exception as e:
        print(f"[Hue] LoRA load skipped: {type(e).__name__}: {e}")

# (선택) 직접 실행용
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001, reload=False, workers=1)
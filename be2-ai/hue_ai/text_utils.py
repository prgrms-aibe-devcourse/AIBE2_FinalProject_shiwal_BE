import re, unicodedata, secrets
from typing import List, Optional
from .config import SYSTEM_PROMPT
# ===== Cleaners =====
_HANJA_RE_ALL = re.compile(r"[\u3400-\u9FFF]")
_MD_FENCE_RE = re.compile(r"```.*?```", re.S)
_MD_INLINE_RE = re.compile(r"`[^`]+`")
_MD_LIST_RE = re.compile(r"^\s*(?:[\-\*\•]|[0-9]+\.)\s+", re.M)
_MD_HDR_RE = re.compile(r"^\s*#{1,6}\s*", re.M)
_ROLE_MARKER = re.compile(r"\[(?:USER|ASSISTANT|ASSIGNANT|ASSIGNIGINANT|SYSTEM|가이드)\]\s*", re.I)

def strip_markdown_noise(s: str) -> str:
    if not s: return s
    s = _MD_FENCE_RE.sub(" ", s)
    s = _MD_INLINE_RE.sub(" ", s)
    s = _MD_LIST_RE.sub("", s)
    s = _MD_HDR_RE.sub("", s)
    s = _ROLE_MARKER.sub("", s)
    s = re.sub(r"\[(?:[^\]]+)\]\([^)]+\)", " ", s)
    return s

_META_NOISE_RE = re.compile(r"(한글만\s*\(|\b문장\s*[:：]|\b결과\s*[:：]|\b요약\s*[:：]|\b출력\s*[:：]|```)", re.I)

def drop_meta_chunks(s: str) -> str:
    if not s: return s
    s = re.sub(r"한글만\s*\([^)]*\)", " ", s)
    s = re.sub(r"(문장|결과|요약|출력)\s*[:：]\s*", " ", s)
    sents = re.split(r"[.!?…\n]+", s)
    kept = [t.strip() for t in sents if t.strip() and not _META_NOISE_RE.search(t)]
    return " ".join(kept).strip()

def split_sentences_ko(s: str) -> List[str]:
    parts = re.split(r"[.!?…\n]+", s)
    return [p.strip() for p in parts if p and p.strip()]

def sanitize_korean_strict(s: str, *, max_sent: int = 3, fallback: Optional[str] = None) -> str:
    if not s: return (fallback or "").strip()
    s = unicodedata.normalize("NFC", str(s))
    s = strip_markdown_noise(s)
    s = drop_meta_chunks(s)
    s = _HANJA_RE_ALL.sub("", s)
    sents = split_sentences_ko(s)
    kept = []
    for t in sents:
        t = re.sub(r"\s+", " ", t).strip()
        if not t: continue
        kept.append(t)
    if not kept and fallback: kept = [fallback]
    kept = kept[:max_sent]
    out = " ".join(kept).strip()
    out = re.sub(r"(\d+)\s+(분|초|회|개|장|일|주|월|년|시간)", r"\1\2", out)
    out = re.sub(r"([가-힣]+)\s+(은|는|이|가|을|를|과|와|로|으로|에|에서|의)", r"\1\2", out)
    out = re.sub(r"\s{2,}", " ", out).strip()
    return out

def looks_non_displayable(s: str) -> bool:
    if not s: return True
    core = re.findall(r"[가-힣A-Za-z0-9]", s)
    if len(core) == 0: return True
    if s.count("?") >= max(3, len(s)//2): return True
    return False

def ko_text_fix(s: str) -> str:
    if not s: return s
    s = unicodedata.normalize("NFC", str(s))
    s = re.sub(r"\s{2,}", " ", s).strip()
    return s

# ===== Action bank & finalizer =====
ACTION_BANK = {
    "sleep": [
        "취침1시간 전 화면을 끄고 조명을 낮춰봐요.",
        "알람을 같은 시간으로 맞추고 오늘은 23시에 불을 꺼봐요.",
        "카페인은 오후2시 전까지만 마셔봐요.",
        "눕기 전 미지근한 물로 3분 손발을 씻어보세요.",
    ],
    "interview": [
        "예상 질문 3개만 적고 10분간 큰 소리로 리허설해봐요.",
        "STAR 구조로 사례 1개만 정리해요.",
        "거울 앞 미소 1분, 첫 문장 5번 말해보기.",
    ],
    "food": [
        "물 한 컵 마시고 요거트/과일처럼 가벼운 간식부터 시작해요.",
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
        "3분 타이머 켜고 생각을 가볍게 적어봐요.",
        "창문 열고 30초 호흡 후 물 한 컵 마시기.",
        "가장 쉬운 일 1개를 5분만 시도해봐요.",
    ],
}

def pick_actions(intent: str, k: int = 1) -> list:
    pool = ACTION_BANK.get(intent) or ACTION_BANK.get("default", [])
    if not pool:
        return ["지금 3분만 호흡을 가다듬고, 쉬운 일 한 가지부터 시작해봐요."]
    arr = pool[:]; out = []
    import random
    for _ in range(min(k, len(arr))):
        choice = random.choice(arr); out.append(choice); arr.remove(choice)
    return out

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
    if len(sents) > 3: txt = " ".join(sents[:3]).strip()
    if not is_actionable(txt):
        extra = pick_actions(intent, k=1)[0]
        txt = (txt + " " + extra).strip()
    return ko_text_fix(txt)

def fix_tags_list(tags: List[str]) -> List[str]:
    mapping = {"불난": "불안", "걱장": "걱정", "면접기": "면접", "수면저하": "수면"}
    pool = []
    for t in (tags or []):
        t = str(t)
        parts = re.split(r"[^\w가-힣]+", t.replace("，", ",").replace("、", ",").replace(".", ","))
        for p in parts:
            p = (mapping.get(p.strip(), p.strip()))
            p = re.sub(r"[^가-힣0-9]", "", p)[:12]
            if p and p not in pool: pool.append(p)
    return pool[:5]
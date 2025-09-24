import re
from typing import Tuple
from .gen import chat_llm

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

def _remove_all_unicode_spaces(s: str) -> str:
    return re.sub(r"[\u0009-\u000D\u0020\u0085\u00A0\u1680\u180E\u2000-\u200A\u2028\u2029\u202F\u205F\u3000]", "", s)

def _normalize_ko(s: str) -> str:
    s = s.lower()
    s = _remove_all_unicode_spaces(s)
    s = re.sub(r"[\u200b\u200c\u200d]", "", s)
    return s

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
        raw = chat_llm(prompt, system_content=None, temperature=0.0, top_p=1.0, max_new_tokens=60)
        import json, re
        m = re.search(r"<json>(\{.*?\})</json>", raw or "", re.S | re.I)
        if m:
            obj = json.loads(m.group(1))
            intent = str(obj.get("intent", "unknown"))
            if intent not in {"safety_crisis","help_request","food","sleep","interview","smalltalk","anger","work","unknown"}:
                intent = "unknown"
            return intent, 0.8, "llm"
    except Exception:
        pass
    return "unknown", 0.5, "llm"
import os

APP_VERSION = "0.9.1-counsel-H6"

# Runtime (safe defaults)
os.environ.setdefault("HF_HOME", r"E:\hf_cache")
os.environ.setdefault("TRANSFORMERS_CACHE", rf"{os.getenv('HF_HOME')}\transformers")
os.environ.setdefault("TORCH_HOME", r"E:\torch_cache")
os.environ.setdefault("CUDA_VISIBLE_DEVICES", "0")
os.environ.setdefault("TOKENIZERS_PARALLELISM", "false")
os.environ.setdefault("PYTORCH_CUDA_ALLOC_CONF", "expandable_segments:True")
os.environ.setdefault("TRANSFORMERS_VERBOSITY", "warning")

MODEL_NAME = os.getenv("LLM_ID", "MLP-KTLim/llama-3-Korean-Bllossom-8B")
HUE_API_KEY = os.getenv("HUE_API_KEY")  # set this in PowerShell: $env:HUE_API_KEY="dev-key"
DEBUG_JSON = os.getenv("HUE_DEBUG_JSON") == "1"

SYSTEM_PROMPT = (
    "당신은 Hue, 지원적인 한국어 AI 코치입니다. "
    "답변은 간결하고 실천 가능하게. 임상 진단/치료 용어는 피하고, "
    "자/타해 위험이 보이면 즉시 도움을 권합니다."
)

CRISIS_TEMPLATE_POLICY = os.getenv("HUE_CRISIS_TEMPLATE", "high_only").lower()
CRISIS_MODE = os.getenv("HUE_CRISIS_MODE", "template_plus_coach").lower()  # template_only | template_plus_coach

DB_CFG = {
    "host": os.getenv("HUE_DB_HOST", "127.0.0.1"),
    "port": int(os.getenv("HUE_DB_PORT", "3306")),
    "user": os.getenv("HUE_DB_USER", "root"),
    "password": os.getenv("HUE_DB_PASS", "") or None,
    "database": os.getenv("HUE_DB_NAME", "hue"),
}
CHAT_TABLE = os.getenv("HUE_CHAT_TABLE", "ai_chat_messages")

HF_OFFLOAD_DIR = os.getenv("HF_OFFLOAD_DIR", r"E:\hf_cache\offload")
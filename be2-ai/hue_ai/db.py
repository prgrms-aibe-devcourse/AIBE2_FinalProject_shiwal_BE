import json, hashlib
from typing import Dict, Any, Optional, List
from .config import DB_CFG, CHAT_TABLE

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
            self.err = "pymysql_not_installed"; return
        if not cfg.get("password"):
            self.err = "no_db_password"; return
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
        if not self.enabled: return
        try:
            h = hashlib.sha256(text.encode("utf-8")).hexdigest()
            with self.conn.cursor() as cur:
                cur.execute("""
                    INSERT INTO crisis_events (user_id, session_id, text_hash, kw_score, risk, reasons, templated)
                    VALUES (%s,%s,%s,%s,%s,%s,%s)
                """, (user_id, session_id, h, int(kw_score), risk, json.dumps(reasons, ensure_ascii=False), int(templated)))
        except Exception:
            pass

    def log_chat(self, user_id: Optional[int], session_id: str, message: str,
                 reply: str, safety_flags: List[str]):
        if not self.enabled: return
        try:
            with self.conn.cursor() as cur:
                cur.execute(f"""
                    INSERT INTO {CHAT_TABLE} (user_id, session_id, message, reply, safety_flags)
                    VALUES (%s,%s,%s,%s,%s)
                """, (user_id, session_id, message, reply, json.dumps(safety_flags, ensure_ascii=False)))
        except Exception:
            pass
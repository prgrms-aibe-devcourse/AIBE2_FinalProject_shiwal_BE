CREATE DATABASE IF NOT EXISTS hue CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE hue;
CREATE TABLE IF NOT EXISTS crisis_events (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  user_id BIGINT NULL,
  session_id VARCHAR(200) NOT NULL,
  text_hash CHAR(64) NOT NULL,
  kw_score INT NOT NULL,
  risk ENUM('low','medium','high') NOT NULL,
  reasons JSON NULL,
  templated TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY ix_crisis_session (session_id, created_at),
  UNIQUE KEY ux_crisis_hash (session_id, text_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_messages (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  user_id BIGINT NULL,
  session_id VARCHAR(200) NOT NULL,
  message TEXT NOT NULL,
  reply   TEXT NOT NULL,
  safety_flags JSON NULL,
  PRIMARY KEY (id),
  KEY ix_chat_session (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

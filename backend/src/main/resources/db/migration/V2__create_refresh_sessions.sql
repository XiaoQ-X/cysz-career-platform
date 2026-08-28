CREATE TABLE refresh_session (
  id BINARY(16) NOT NULL,
  user_id BINARY(16) NOT NULL,
  token_hash CHAR(64) NOT NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  revoked_at TIMESTAMP(6) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_refresh_token_hash (token_hash),
  KEY idx_refresh_user (user_id),
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES user_account(id)
);

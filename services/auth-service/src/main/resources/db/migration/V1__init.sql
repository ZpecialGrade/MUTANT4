CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
	id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
	email varchar(320) NOT NULL UNIQUE,
	password_hash varchar(255) NOT NULL,
	status varchar(32) NOT NULL DEFAULT 'ACTIVE',
	created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
	id uuid PRIMARY KEY,
	user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
	secret_hash varchar(255) NOT NULL,
	expires_at timestamptz NOT NULL,
	revoked_at timestamptz NULL,
	created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);


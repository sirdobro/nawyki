-- Schema foundation only. No accounts are seeded and registration is not enabled.
CREATE TABLE account (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    time_zone varchar(64) NOT NULL DEFAULT 'Europe/Warsaw'
);

CREATE TABLE access_key_verifier (
    account_id uuid PRIMARY KEY REFERENCES account(id) ON DELETE CASCADE,
    verifier_hash varchar(64) UNIQUE NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT access_key_hash_format CHECK (verifier_hash ~ '^[0-9a-f]{64}$')
);

CREATE TABLE account_session (
    id uuid PRIMARY KEY,
    account_id uuid NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    verifier_hash varchar(64) UNIQUE NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamptz NOT NULL,
    CONSTRAINT session_hash_format CHECK (verifier_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT session_expiry CHECK (expires_at > created_at)
);
CREATE INDEX account_session_account_idx ON account_session(account_id);
CREATE INDEX account_session_expiry_idx ON account_session(expires_at);

ALTER TABLE users
    ADD COLUMN deletion_finalized_at DATETIME(6) NULL;

ALTER TABLE users
    ADD COLUMN local_login_enabled BIT NOT NULL DEFAULT 1;

CREATE UNIQUE INDEX uk_users_provider_identity
    ON users (provider, provider_id);

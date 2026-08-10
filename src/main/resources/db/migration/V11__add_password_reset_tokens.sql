CREATE TABLE dbo.password_reset_tokens (
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME2 NOT NULL,
    used_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    version BIGINT NOT NULL CONSTRAINT DF_password_reset_tokens_version DEFAULT 0,
    CONSTRAINT PK_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT UQ_password_reset_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT FK_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES dbo.users(id)
        ON DELETE CASCADE
);

CREATE INDEX IX_password_reset_tokens_user_id
    ON dbo.password_reset_tokens(user_id);

CREATE INDEX IX_password_reset_tokens_expires_at
    ON dbo.password_reset_tokens(expires_at);

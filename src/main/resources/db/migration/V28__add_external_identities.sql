CREATE TABLE external_identities
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NOT NULL,
    provider NVARCHAR(20) NOT NULL,
    provider_subject NVARCHAR(255) NOT NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT PK_external_identities PRIMARY KEY (id),
    CONSTRAINT FK_external_identities_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT UQ_external_identities_provider_subject
        UNIQUE (provider, provider_subject),
    CONSTRAINT UQ_external_identities_user_provider
        UNIQUE (user_id, provider),
    CONSTRAINT CK_external_identities_provider
        CHECK (provider IN ('google', 'apple'))
);

CREATE INDEX IX_external_identities_user_id
    ON external_identities(user_id);

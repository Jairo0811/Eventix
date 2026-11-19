/* Eventix v1.4 preparation — verified webhook inbox.
   No public webhook endpoint exists yet. An approved payment provider must
   supply its official signing/reconciliation contract before activation.
   Store a SHA-256 digest, never full card data, wallet tokens or credentials.
*/
CREATE TABLE dbo.payment_webhook_inbox
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    provider NVARCHAR(20) NOT NULL,
    external_event_id NVARCHAR(160) NOT NULL,
    payload_sha256 CHAR(64) NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'VERIFIED',
    verified_at DATETIME2(6) NOT NULL DEFAULT SYSUTCDATETIME(),
    processed_at DATETIME2(6) NULL,
    CONSTRAINT PK_payment_webhook_inbox PRIMARY KEY (id),
    CONSTRAINT UQ_payment_webhook_inbox_provider_event
        UNIQUE (provider, external_event_id),
    CONSTRAINT CK_payment_webhook_inbox_status
        CHECK (status IN ('VERIFIED', 'PROCESSED', 'REJECTED')),
    CONSTRAINT CK_payment_webhook_inbox_digest CHECK (LEN(payload_sha256) = 64),
    CONSTRAINT CK_payment_webhook_inbox_processed
        CHECK (status = 'VERIFIED' OR processed_at IS NOT NULL)
);

CREATE INDEX IX_payment_webhook_inbox_status_verified
    ON dbo.payment_webhook_inbox(status, verified_at);

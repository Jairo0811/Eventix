/* ================================================================
   EVENTIX
   V23 - Centro de notificaciones internas
   ================================================================ */

CREATE TABLE internal_notifications
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    recipient_id BIGINT NOT NULL,
    notification_type NVARCHAR(30) NOT NULL,
    title NVARCHAR(160) NOT NULL,
    message NVARCHAR(500) NOT NULL,
    target_url NVARCHAR(500) NULL,
    read_at DATETIME2(6) NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'system',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT PK_internal_notifications PRIMARY KEY (id),
    CONSTRAINT FK_internal_notifications_recipient
        FOREIGN KEY (recipient_id) REFERENCES users(id),
    CONSTRAINT CK_internal_notifications_type CHECK (
        notification_type IN (
            'PURCHASE', 'REFUND', 'TICKET', 'SETTLEMENT',
            'ELIGIBILITY', 'REMINDER', 'SYSTEM'
        )
    )
);

CREATE INDEX IX_internal_notifications_recipient_read_created
    ON internal_notifications(recipient_id, read_at, created_at DESC);

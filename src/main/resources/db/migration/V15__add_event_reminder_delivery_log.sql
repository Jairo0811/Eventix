/* ================================================================
   EVENTIX
   V15 - Entrega deduplicada y reintentable de recordatorios

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

CREATE TABLE event_reminder_deliveries
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    event_id BIGINT NOT NULL,
    recipient_email NVARCHAR(160) NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    last_attempt_at DATETIME2(6) NULL,
    next_attempt_at DATETIME2(6) NULL,
    sent_at DATETIME2(6) NULL,
    last_error_type NVARCHAR(160) NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT PK_event_reminder_deliveries PRIMARY KEY (id),
    CONSTRAINT FK_event_reminder_deliveries_event
        FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT UQ_event_reminder_deliveries_event_recipient
        UNIQUE (event_id, recipient_email),
    CONSTRAINT CK_event_reminder_deliveries_status CHECK (
        status IN ('PENDING', 'SENT', 'FAILED', 'SKIPPED')
    ),
    CONSTRAINT CK_event_reminder_deliveries_attempts
        CHECK (attempt_count >= 0),
    CONSTRAINT CK_event_reminder_deliveries_sent CHECK (
        status <> 'SENT' OR sent_at IS NOT NULL
    ),
    CONSTRAINT CK_event_reminder_deliveries_retry CHECK (
        status <> 'PENDING' OR attempt_count = 0
    )
);

CREATE INDEX IX_event_reminder_deliveries_due
    ON event_reminder_deliveries(status, next_attempt_at, created_at);

/* ================================================================
   EVENTIX
   V7 - Bitácora central de auditoría

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

CREATE TABLE audit_logs
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    occurred_at DATETIME2(6) NOT NULL,
    actor_username NVARCHAR(160) NULL,
    event_type NVARCHAR(40) NOT NULL,
    action NVARCHAR(160) NOT NULL,
    entity_type NVARCHAR(80) NULL,
    entity_id NVARCHAR(80) NULL,
    outcome NVARCHAR(20) NOT NULL,
    http_method NVARCHAR(10) NULL,
    request_path NVARCHAR(500) NULL,
    ip_address NVARCHAR(45) NULL,
    user_agent NVARCHAR(300) NULL,
    correlation_id NVARCHAR(64) NOT NULL,
    details NVARCHAR(1000) NULL,

    CONSTRAINT PK_audit_logs PRIMARY KEY (id),
    CONSTRAINT CK_audit_logs_event_type CHECK (event_type IN (
        'LOGIN', 'LOGOUT', 'AUTHENTICATION_FAILURE', 'CRUD',
        'SALE', 'RESERVATION', 'SCAN', 'STATUS_CHANGE',
        'EXPORT', 'ERROR'
    )),
    CONSTRAINT CK_audit_logs_outcome CHECK (outcome IN (
        'SUCCESS', 'FAILURE', 'DENIED'
    ))
);

CREATE INDEX IX_audit_logs_occurred_at
    ON audit_logs(occurred_at DESC);

CREATE INDEX IX_audit_logs_actor_date
    ON audit_logs(actor_username, occurred_at DESC);

CREATE INDEX IX_audit_logs_type_date
    ON audit_logs(event_type, occurred_at DESC);

CREATE INDEX IX_audit_logs_correlation_id
    ON audit_logs(correlation_id);

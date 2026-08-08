/* ================================================================
   EVENTIX
   V6 - Boletas digitales, Wallet y control de acceso

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

CREATE TABLE digital_tickets
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    unique_code NVARCHAR(32) NOT NULL,
    sale_id BIGINT NOT NULL,
    sale_item_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    sequence_number INT NOT NULL,
    attendee_name NVARCHAR(161) NOT NULL,
    attendee_email NVARCHAR(160) NOT NULL,
    ticket_type_name NVARCHAR(80) NOT NULL,
    zone NVARCHAR(80) NULL,
    seat NVARCHAR(40) NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    anti_fraud_code NVARCHAR(32) NOT NULL,
    signed_payload_hash NVARCHAR(64) NOT NULL,
    digital_signature NVARCHAR(180) NOT NULL,
    signature_key_id NVARCHAR(80) NOT NULL,
    issued_at DATETIME2(6) NOT NULL,
    used_at DATETIME2(6) NULL,
    cancelled_at DATETIME2(6) NULL,
    cancellation_reason NVARCHAR(500) NULL,
    pass_updated_at DATETIME2(6) NOT NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT PK_digital_tickets PRIMARY KEY (id),
    CONSTRAINT UQ_digital_tickets_unique_code UNIQUE (unique_code),
    CONSTRAINT UQ_digital_tickets_anti_fraud UNIQUE (anti_fraud_code),
    CONSTRAINT UQ_digital_tickets_sale_sequence
        UNIQUE (sale_id, sequence_number),
    CONSTRAINT FK_digital_tickets_sale
        FOREIGN KEY (sale_id) REFERENCES sales(id),
    CONSTRAINT FK_digital_tickets_sale_item
        FOREIGN KEY (sale_item_id) REFERENCES sale_items(id),
    CONSTRAINT FK_digital_tickets_event
        FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT CK_digital_tickets_sequence CHECK (sequence_number > 0),
    CONSTRAINT CK_digital_tickets_status
        CHECK (status IN ('ACTIVE', 'USED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT CK_digital_tickets_used
        CHECK (status <> 'USED' OR used_at IS NOT NULL),
    CONSTRAINT CK_digital_tickets_cancelled
        CHECK (
            status <> 'CANCELLED'
            OR (
                cancelled_at IS NOT NULL
                AND cancellation_reason IS NOT NULL
                AND LEN(LTRIM(RTRIM(cancellation_reason))) > 0
            )
        )
);

CREATE TABLE ticket_scan_attempts
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    ticket_id BIGINT NULL,
    event_id BIGINT NULL,
    raw_code_hash NVARCHAR(64) NOT NULL,
    outcome NVARCHAR(24) NOT NULL,
    occurred_at DATETIME2(6) NOT NULL,
    scanned_by_id BIGINT NOT NULL,
    device_identifier NVARCHAR(120) NOT NULL,
    ip_address NVARCHAR(45) NOT NULL,
    first_access BIT NOT NULL DEFAULT 0,
    duplicate_attempt BIT NOT NULL DEFAULT 0,
    notes NVARCHAR(300) NULL,

    CONSTRAINT PK_ticket_scan_attempts PRIMARY KEY (id),
    CONSTRAINT FK_ticket_scan_attempts_ticket
        FOREIGN KEY (ticket_id) REFERENCES digital_tickets(id),
    CONSTRAINT FK_ticket_scan_attempts_event
        FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT FK_ticket_scan_attempts_user
        FOREIGN KEY (scanned_by_id) REFERENCES users(id),
    CONSTRAINT CK_ticket_scan_attempts_outcome
        CHECK (outcome IN (
            'VALID', 'REENTRY', 'DUPLICATE', 'CANCELLED',
            'COUNTERFEIT', 'EXPIRED'
        ))
);

CREATE TABLE apple_wallet_registrations
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    ticket_id BIGINT NOT NULL,
    device_library_identifier NVARCHAR(160) NOT NULL,
    push_token NVARCHAR(200) NOT NULL,
    registered_at DATETIME2(6) NOT NULL,
    updated_at DATETIME2(6) NOT NULL,

    CONSTRAINT PK_apple_wallet_registrations PRIMARY KEY (id),
    CONSTRAINT FK_apple_wallet_registrations_ticket
        FOREIGN KEY (ticket_id) REFERENCES digital_tickets(id)
        ON DELETE CASCADE,
    CONSTRAINT UQ_apple_wallet_device_ticket
        UNIQUE (device_library_identifier, ticket_id)
);

CREATE INDEX IX_digital_tickets_sale
    ON digital_tickets(sale_id);

CREATE INDEX IX_digital_tickets_event_status
    ON digital_tickets(event_id, status);

CREATE INDEX IX_digital_tickets_pass_updated
    ON digital_tickets(pass_updated_at);

CREATE INDEX IX_ticket_scan_attempts_event_date
    ON ticket_scan_attempts(event_id, occurred_at);

CREATE INDEX IX_ticket_scan_attempts_ticket_date
    ON ticket_scan_attempts(ticket_id, occurred_at);

CREATE INDEX IX_apple_wallet_device_updates
    ON apple_wallet_registrations(device_library_identifier, updated_at);

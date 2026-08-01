/* ================================================================
   EVENTIX
   V4 - Gestión de reservaciones y control de disponibilidad

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

CREATE TABLE reservations
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    reference_code NVARCHAR(24) NOT NULL,
    event_id BIGINT NOT NULL,
    attendee_first_name NVARCHAR(80) NOT NULL,
    attendee_last_name NVARCHAR(80) NOT NULL,
    attendee_email NVARCHAR(160) NOT NULL,
    attendee_phone NVARCHAR(30) NOT NULL,
    quantity INT NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at DATETIME2(6) NOT NULL,
    confirmed_at DATETIME2(6) NULL,
    cancelled_at DATETIME2(6) NULL,
    cancellation_reason NVARCHAR(500) NULL,
    reserved_by_id BIGINT NOT NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT PK_reservations PRIMARY KEY (id),
    CONSTRAINT UQ_reservations_reference_code UNIQUE (reference_code),
    CONSTRAINT FK_reservations_event
        FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT FK_reservations_reserved_by
        FOREIGN KEY (reserved_by_id) REFERENCES users(id),
    CONSTRAINT CK_reservations_quantity
        CHECK (quantity > 0),
    CONSTRAINT CK_reservations_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT CK_reservations_confirmation
        CHECK (status <> 'CONFIRMED' OR confirmed_at IS NOT NULL),
    CONSTRAINT CK_reservations_cancellation
        CHECK (
            status <> 'CANCELLED'
            OR (
                cancelled_at IS NOT NULL
                AND cancellation_reason IS NOT NULL
                AND LEN(LTRIM(RTRIM(cancellation_reason))) > 0
            )
        )
);

CREATE INDEX IX_reservations_event_status
    ON reservations(event_id, status);

CREATE INDEX IX_reservations_event_email_status
    ON reservations(event_id, attendee_email, status);

CREATE INDEX IX_reservations_status_expires_at
    ON reservations(status, expires_at);

CREATE INDEX IX_reservations_attendee_name
    ON reservations(attendee_last_name, attendee_first_name);

CREATE INDEX IX_reservations_created_at
    ON reservations(created_at);

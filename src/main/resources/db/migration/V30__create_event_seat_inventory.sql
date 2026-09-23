/* ================================================================
   EVENTIX
   V30 - Inventario de asientos reservados por evento

   Cada asiento físico del recinto obtiene estado independiente por
   evento. Los holds expiran y una venta puede cerrar el asiento.

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

CREATE TABLE event_seat_inventory
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    event_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    status NVARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
    hold_token NVARCHAR(64) NULL,
    hold_expires_at DATETIME2(6) NULL,
    sale_id BIGINT NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT PK_event_seat_inventory PRIMARY KEY (id),
    CONSTRAINT FK_event_seat_inventory_event
        FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT FK_event_seat_inventory_seat
        FOREIGN KEY (seat_id) REFERENCES venue_seats(id),
    CONSTRAINT FK_event_seat_inventory_sale
        FOREIGN KEY (sale_id) REFERENCES sales(id),
    CONSTRAINT UQ_event_seat_inventory_event_seat
        UNIQUE (event_id, seat_id),
    CONSTRAINT CK_event_seat_inventory_status
        CHECK (status IN ('AVAILABLE', 'HELD', 'SOLD', 'BLOCKED')),
    CONSTRAINT CK_event_seat_inventory_hold
        CHECK (
            (status = 'HELD' AND hold_token IS NOT NULL AND hold_expires_at IS NOT NULL)
            OR
            (status <> 'HELD' AND hold_token IS NULL AND hold_expires_at IS NULL)
        ),
    CONSTRAINT CK_event_seat_inventory_sale
        CHECK (
            (status = 'SOLD' AND sale_id IS NOT NULL)
            OR
            (status <> 'SOLD' AND sale_id IS NULL)
        )
);

CREATE INDEX IX_event_seat_inventory_event_status
    ON event_seat_inventory(event_id, status);

CREATE INDEX IX_event_seat_inventory_hold_expiry
    ON event_seat_inventory(status, hold_expires_at)
    WHERE status = 'HELD';

CREATE UNIQUE INDEX UX_event_seat_inventory_hold_seat
    ON event_seat_inventory(event_id, seat_id, status)
    WHERE status IN ('HELD', 'SOLD');

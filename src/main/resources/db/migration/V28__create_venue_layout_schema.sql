/* ================================================================
   EVENTIX
   V28 - Venues y layout base para asientos reservados

   Introduce recintos reutilizables con secciones, filas y asientos.
   Mantiene events.venue y events.address para compatibilidad con
   eventos existentes mientras se adopta la referencia estructurada.

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

CREATE TABLE venues
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(160) NOT NULL,
    address NVARCHAR(300) NOT NULL,
    city NVARCHAR(120) NOT NULL,
    country_code NVARCHAR(2) NOT NULL,
    time_zone NVARCHAR(80) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT PK_venues PRIMARY KEY (id),
    CONSTRAINT UQ_venues_name_city UNIQUE (name, city),
    CONSTRAINT CK_venues_country_code CHECK (LEN(country_code) = 2)
);

CREATE TABLE venue_sections
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    venue_id BIGINT NOT NULL,
    code NVARCHAR(40) NOT NULL,
    name NVARCHAR(120) NOT NULL,
    section_type NVARCHAR(24) NOT NULL,
    capacity INT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT PK_venue_sections PRIMARY KEY (id),
    CONSTRAINT FK_venue_sections_venue
        FOREIGN KEY (venue_id) REFERENCES venues(id) ON DELETE CASCADE,
    CONSTRAINT UQ_venue_sections_venue_code UNIQUE (venue_id, code),
    CONSTRAINT CK_venue_sections_type CHECK (
        section_type IN ('RESERVED_SEATING', 'GENERAL_ADMISSION', 'STANDING')
    ),
    CONSTRAINT CK_venue_sections_capacity CHECK (capacity > 0)
);

CREATE TABLE venue_rows
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    section_id BIGINT NOT NULL,
    code NVARCHAR(40) NOT NULL,
    label NVARCHAR(80) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT PK_venue_rows PRIMARY KEY (id),
    CONSTRAINT FK_venue_rows_section
        FOREIGN KEY (section_id) REFERENCES venue_sections(id) ON DELETE CASCADE,
    CONSTRAINT UQ_venue_rows_section_code UNIQUE (section_id, code)
);

CREATE TABLE venue_seats
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    row_id BIGINT NOT NULL,
    seat_number NVARCHAR(20) NOT NULL,
    label NVARCHAR(80) NOT NULL,
    accessible BIT NOT NULL DEFAULT 0,
    companion_seat BIT NOT NULL DEFAULT 0,
    x_position DECIMAL(10,4) NULL,
    y_position DECIMAL(10,4) NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT PK_venue_seats PRIMARY KEY (id),
    CONSTRAINT FK_venue_seats_row
        FOREIGN KEY (row_id) REFERENCES venue_rows(id) ON DELETE CASCADE,
    CONSTRAINT UQ_venue_seats_row_number UNIQUE (row_id, seat_number)
);

ALTER TABLE events
    ADD venue_id BIGINT NULL;

ALTER TABLE events
    ADD seating_mode NVARCHAR(24) NOT NULL
        CONSTRAINT DF_events_seating_mode DEFAULT 'GENERAL_ADMISSION';

ALTER TABLE events
    ADD CONSTRAINT FK_events_venue
        FOREIGN KEY (venue_id) REFERENCES venues(id);

ALTER TABLE events
    ADD CONSTRAINT CK_events_seating_mode CHECK (
        seating_mode IN ('GENERAL_ADMISSION', 'RESERVED_SEATING', 'MIXED')
    );

CREATE INDEX IX_venues_active_name
    ON venues(active, name);

CREATE INDEX IX_venue_sections_venue_active_sort
    ON venue_sections(venue_id, active, sort_order);

CREATE INDEX IX_venue_rows_section_active_sort
    ON venue_rows(section_id, active, sort_order);

CREATE INDEX IX_venue_seats_row_active
    ON venue_seats(row_id, active);

CREATE INDEX IX_events_venue_id
    ON events(venue_id);

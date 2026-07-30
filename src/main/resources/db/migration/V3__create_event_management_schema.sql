/* ================================================================
   EVENTIX
   V3 - Gestión de categorías y eventos

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

CREATE TABLE event_categories
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(80) NOT NULL,
    description NVARCHAR(240) NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT PK_event_categories PRIMARY KEY (id),
    CONSTRAINT UQ_event_categories_name UNIQUE (name)
);

CREATE TABLE events
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    title NVARCHAR(160) NOT NULL,
    description NVARCHAR(MAX) NOT NULL,
    category_id BIGINT NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    start_at DATETIME2(6) NOT NULL,
    end_at DATETIME2(6) NOT NULL,
    venue NVARCHAR(160) NOT NULL,
    address NVARCHAR(300) NOT NULL,
    capacity INT NOT NULL,
    organizer_id BIGINT NOT NULL,
    cover_image_url NVARCHAR(500) NULL,
    is_free BIT NOT NULL DEFAULT 1,
    base_price DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT PK_events PRIMARY KEY (id),
    CONSTRAINT FK_events_category
        FOREIGN KEY (category_id) REFERENCES event_categories(id),
    CONSTRAINT FK_events_organizer
        FOREIGN KEY (organizer_id) REFERENCES users(id),
    CONSTRAINT CK_events_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED', 'FINISHED')),
    CONSTRAINT CK_events_dates
        CHECK (end_at > start_at),
    CONSTRAINT CK_events_capacity
        CHECK (capacity > 0),
    CONSTRAINT CK_events_price
        CHECK (
            (is_free = 1 AND base_price = 0)
            OR (is_free = 0 AND base_price > 0)
        )
);

CREATE INDEX IX_event_categories_active_name
    ON event_categories(active, name);

CREATE INDEX IX_events_status_start_at
    ON events(status, start_at);

CREATE INDEX IX_events_category_id
    ON events(category_id);

CREATE INDEX IX_events_organizer_id
    ON events(organizer_id);

CREATE INDEX IX_events_title
    ON events(title);

INSERT INTO event_categories
    (name, description, active)
VALUES
    (N'Conferencia', N'Charlas, congresos y encuentros profesionales.', 1);

INSERT INTO event_categories
    (name, description, active)
VALUES
    (N'Concierto', N'Presentaciones musicales y espectáculos en vivo.', 1);

INSERT INTO event_categories
    (name, description, active)
VALUES
    (N'Deportivo', N'Competencias, torneos y actividades deportivas.', 1);

INSERT INTO event_categories
    (name, description, active)
VALUES
    (N'Taller', N'Capacitaciones y actividades prácticas.', 1);

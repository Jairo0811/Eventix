/* ================================================================
   EVENTIX
   V31 - Pricing y asignación de tipos de entrada por sección

   Vincula un tipo de entrada con una sección concreta del recinto
   para resolver RESERVED_SEATING y MIXED de forma determinística.
   El price_override es opcional; cuando es NULL se usa el precio
   vigente del ticket_type al momento de la venta.

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

CREATE TABLE event_section_pricing
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    event_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    ticket_type_id BIGINT NOT NULL,
    price_override DECIMAL(12,2) NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT PK_event_section_pricing PRIMARY KEY (id),
    CONSTRAINT FK_event_section_pricing_event
        FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT FK_event_section_pricing_section
        FOREIGN KEY (section_id) REFERENCES venue_sections(id),
    CONSTRAINT FK_event_section_pricing_ticket_type
        FOREIGN KEY (ticket_type_id) REFERENCES ticket_types(id),
    CONSTRAINT UQ_event_section_pricing_event_section
        UNIQUE (event_id, section_id),
    CONSTRAINT UQ_event_section_pricing_event_ticket_type
        UNIQUE (event_id, ticket_type_id),
    CONSTRAINT CK_event_section_pricing_price_override
        CHECK (price_override IS NULL OR price_override >= 0)
);

CREATE INDEX IX_event_section_pricing_event
    ON event_section_pricing(event_id);

CREATE INDEX IX_event_section_pricing_ticket_type
    ON event_section_pricing(ticket_type_id);

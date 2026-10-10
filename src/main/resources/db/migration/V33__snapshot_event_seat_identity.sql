/* ================================================================
   EVENTIX
   V33 - Snapshot histórico del asiento por evento

   Conserva la identidad visible de la localidad al momento de crear
   el inventario. Así, cambios posteriores al layout reutilizable del
   recinto no alteran boletas ya vendidas ni el histórico del evento.

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

ALTER TABLE event_seat_inventory
ADD section_code_snapshot NVARCHAR(40) NULL,
    section_name_snapshot NVARCHAR(120) NULL,
    row_code_snapshot NVARCHAR(40) NULL,
    seat_number_snapshot NVARCHAR(20) NULL,
    seat_label_snapshot NVARCHAR(80) NULL,
    accessible_snapshot BIT NULL;

/*
   Las columnas fueron creadas dentro de esta misma migración. SQL
   dinámico fuerza una recompilación después del ALTER TABLE y evita
   el name-resolution anticipado de SQL Server.
*/
EXEC(N'
    UPDATE esi
    SET section_code_snapshot = section.code,
        section_name_snapshot = section.name,
        row_code_snapshot = venue_row.code,
        seat_number_snapshot = seat.seat_number,
        seat_label_snapshot = seat.label,
        accessible_snapshot = seat.accessible
    FROM event_seat_inventory esi
    INNER JOIN venue_seats seat
        ON seat.id = esi.seat_id
    INNER JOIN venue_rows venue_row
        ON venue_row.id = seat.row_id
    INNER JOIN venue_sections section
        ON section.id = venue_row.section_id
');

EXEC(N'
    ALTER TABLE event_seat_inventory
        ALTER COLUMN section_code_snapshot NVARCHAR(40) NOT NULL;
    ALTER TABLE event_seat_inventory
        ALTER COLUMN section_name_snapshot NVARCHAR(120) NOT NULL;
    ALTER TABLE event_seat_inventory
        ALTER COLUMN row_code_snapshot NVARCHAR(40) NOT NULL;
    ALTER TABLE event_seat_inventory
        ALTER COLUMN seat_number_snapshot NVARCHAR(20) NOT NULL;
    ALTER TABLE event_seat_inventory
        ALTER COLUMN seat_label_snapshot NVARCHAR(80) NOT NULL;
    ALTER TABLE event_seat_inventory
        ALTER COLUMN accessible_snapshot BIT NOT NULL;
');

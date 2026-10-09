/* ================================================================
   EVENTIX
   V32 - Ownership persistente de holds de asientos

   Vincula cada asiento HELD con el usuario comprador que creó la
   retención. Los holds transitorios previos a esta migración se
   liberan porque no existe una identidad histórica confiable que
   permita asignarles propietario.

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

UPDATE event_seat_inventory
SET status = 'AVAILABLE',
    hold_token = NULL,
    hold_expires_at = NULL,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = 'flyway-v32',
    version = version + 1
WHERE status = 'HELD';

ALTER TABLE event_seat_inventory
ADD held_by_user_id BIGINT NULL;

/*
   SQL Server compila el batch completo antes de ejecutar el ALTER ADD.
   Las sentencias que referencian la columna recién creada se ejecutan
   como SQL dinámico para forzar una nueva compilación después del ALTER.
*/
EXEC(N'
    ALTER TABLE event_seat_inventory
    ADD CONSTRAINT FK_event_seat_inventory_held_by_user
        FOREIGN KEY (held_by_user_id) REFERENCES users(id)
');

ALTER TABLE event_seat_inventory
DROP CONSTRAINT CK_event_seat_inventory_hold;

EXEC(N'
    ALTER TABLE event_seat_inventory
    ADD CONSTRAINT CK_event_seat_inventory_hold
        CHECK (
            (
                status = ''HELD''
                AND hold_token IS NOT NULL
                AND hold_expires_at IS NOT NULL
                AND held_by_user_id IS NOT NULL
            )
            OR
            (
                status <> ''HELD''
                AND hold_token IS NULL
                AND hold_expires_at IS NULL
                AND held_by_user_id IS NULL
            )
        )
');

EXEC(N'
    CREATE INDEX IX_event_seat_inventory_hold_owner
        ON event_seat_inventory(event_id, held_by_user_id, status)
        WHERE status = ''HELD''
');

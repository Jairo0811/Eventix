IF NOT EXISTS (
    SELECT 1
    FROM event_categories
    WHERE LOWER(name) = LOWER(N'Promoción escolar')
)
BEGIN
    INSERT INTO event_categories (
        name,
        description,
        active,
        created_at,
        updated_at,
        created_by,
        updated_by,
        version
    )
    VALUES (
        N'Promoción escolar',
        N'Graduaciones, reencuentros y actividades exclusivas de una promoción o generación escolar.',
        1,
        SYSDATETIME(),
        SYSDATETIME(),
        N'flyway',
        N'flyway',
        0
    );
END;

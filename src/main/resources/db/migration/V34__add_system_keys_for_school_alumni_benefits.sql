IF COL_LENGTH('dbo.event_categories', 'system_key') IS NULL
BEGIN
    ALTER TABLE dbo.event_categories
        ADD system_key VARCHAR(40) NULL;
END;

UPDATE dbo.event_categories
SET system_key = 'SCHOOL_PROMOTION'
WHERE system_key IS NULL
  AND LOWER(name) = LOWER(N'Promoción escolar');

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'UX_event_categories_system_key'
      AND object_id = OBJECT_ID('dbo.event_categories')
)
BEGIN
    CREATE UNIQUE INDEX UX_event_categories_system_key
        ON dbo.event_categories(system_key)
        WHERE system_key IS NOT NULL;
END;

IF COL_LENGTH('dbo.eligibility_groups', 'system_key') IS NULL
BEGIN
    ALTER TABLE dbo.eligibility_groups
        ADD system_key VARCHAR(40) NULL;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'UX_eligibility_groups_event_system_key'
      AND object_id = OBJECT_ID('dbo.eligibility_groups')
)
BEGIN
    CREATE UNIQUE INDEX UX_eligibility_groups_event_system_key
        ON dbo.eligibility_groups(event_id, system_key)
        WHERE system_key IS NOT NULL;
END;

IF COL_LENGTH('dbo.eligibility_benefits', 'system_key') IS NULL
BEGIN
    ALTER TABLE dbo.eligibility_benefits
        ADD system_key VARCHAR(50) NULL;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'UX_eligibility_benefits_group_system_key'
      AND object_id = OBJECT_ID('dbo.eligibility_benefits')
)
BEGIN
    CREATE UNIQUE INDEX UX_eligibility_benefits_group_system_key
        ON dbo.eligibility_benefits(group_id, system_key)
        WHERE system_key IS NOT NULL;
END;

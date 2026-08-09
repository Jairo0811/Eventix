IF EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'CK_roles_name'
      AND parent_object_id = OBJECT_ID('dbo.roles')
)
BEGIN
    ALTER TABLE dbo.roles
    DROP CONSTRAINT CK_roles_name;
END;

ALTER TABLE dbo.roles
ADD CONSTRAINT CK_roles_name
CHECK (
    name IN (
        'ADMINISTRATOR',
        'OPERATOR',
        'ORGANIZER',
        'ACCESS_STAFF',
        'USER'
    )
);

IF NOT EXISTS (
    SELECT 1
    FROM dbo.roles
    WHERE name = 'USER'
)
BEGIN
    INSERT INTO dbo.roles (name, description)
    VALUES (
        'USER',
        N'Usuario final de Eventix para descubrir eventos y gestionar sus boletas.'
    );
END;
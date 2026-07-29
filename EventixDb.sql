/* ================================================================
   EVENTIX
   Script inicial para Microsoft SQL Server
   Fase 1: seguridad, roles y gestión de usuarios
   ================================================================ */

USE master;
GO

/* ================================================================
   1. CREAR BASE DE DATOS
   ================================================================ */

IF DB_ID(N'EventixDb') IS NULL
BEGIN
    CREATE DATABASE EventixDb;
END;
GO

ALTER DATABASE EventixDb
SET RECOVERY SIMPLE;
GO

ALTER DATABASE EventixDb
SET READ_COMMITTED_SNAPSHOT ON;
GO

USE EventixDb;
GO

/* ================================================================
   2. CREAR LOGIN Y USUARIO TÉCNICO
   Solo para desarrollo local
   ================================================================ */

IF NOT EXISTS (
    SELECT 1
    FROM sys.server_principals
    WHERE name = N'eventix_app'
)
BEGIN
    CREATE LOGIN eventix_app
    WITH PASSWORD = N'Eventix2026*',
         CHECK_POLICY = ON,
         CHECK_EXPIRATION = OFF;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.database_principals
    WHERE name = N'eventix_app'
)
BEGIN
    CREATE USER eventix_app
    FOR LOGIN eventix_app;
END;
GO

/* Permisos necesarios durante desarrollo y migraciones Flyway */

ALTER ROLE db_datareader ADD MEMBER eventix_app;
ALTER ROLE db_datawriter ADD MEMBER eventix_app;
ALTER ROLE db_ddladmin ADD MEMBER eventix_app;
GO

/* ================================================================
   3. TABLA ROLES
   ================================================================ */

IF OBJECT_ID(N'dbo.roles', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.roles
    (
        id BIGINT IDENTITY(1,1) NOT NULL,

        name NVARCHAR(40) NOT NULL,

        description NVARCHAR(160) NOT NULL,

        CONSTRAINT PK_roles
            PRIMARY KEY CLUSTERED (id),

        CONSTRAINT UQ_roles_name
            UNIQUE (name),

        CONSTRAINT CK_roles_name
            CHECK
            (
                name IN
                (
                    N'ADMINISTRATOR',
                    N'OPERATOR',
                    N'ORGANIZER',
                    N'ACCESS_STAFF'
                )
            )
    );
END;
GO

/* ================================================================
   4. TABLA USUARIOS
   ================================================================ */

IF OBJECT_ID(N'dbo.users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.users
    (
        id BIGINT IDENTITY(1,1) NOT NULL,

        first_name NVARCHAR(80) NOT NULL,

        last_name NVARCHAR(80) NOT NULL,

        email NVARCHAR(160) NOT NULL,

        username NVARCHAR(60) NOT NULL,

        password_hash NVARCHAR(100) NOT NULL,

        phone NVARCHAR(30) NULL,

        role_id BIGINT NOT NULL,

        status NVARCHAR(20) NOT NULL
            CONSTRAINT DF_users_status
            DEFAULT N'ACTIVE',

        must_change_password BIT NOT NULL
            CONSTRAINT DF_users_must_change_password
            DEFAULT 1,

        last_login_at DATETIME2(6) NULL,

        created_at DATETIME2(6) NOT NULL
            CONSTRAINT DF_users_created_at
            DEFAULT SYSDATETIME(),

        updated_at DATETIME2(6) NOT NULL
            CONSTRAINT DF_users_updated_at
            DEFAULT SYSDATETIME(),

        created_by NVARCHAR(120) NOT NULL
            CONSTRAINT DF_users_created_by
            DEFAULT N'SYSTEM',

        updated_by NVARCHAR(120) NOT NULL
            CONSTRAINT DF_users_updated_by
            DEFAULT N'SYSTEM',

        version BIGINT NOT NULL
            CONSTRAINT DF_users_version
            DEFAULT 0,

        CONSTRAINT PK_users
            PRIMARY KEY CLUSTERED (id),

        CONSTRAINT FK_users_roles
            FOREIGN KEY (role_id)
            REFERENCES dbo.roles(id),

        CONSTRAINT UQ_users_email
            UNIQUE (email),

        CONSTRAINT UQ_users_username
            UNIQUE (username),

        CONSTRAINT CK_users_status
            CHECK
            (
                status IN
                (
                    N'ACTIVE',
                    N'INACTIVE',
                    N'LOCKED'
                )
            ),

        CONSTRAINT CK_users_email_not_empty
            CHECK (LEN(LTRIM(RTRIM(email))) > 0),

        CONSTRAINT CK_users_username_not_empty
            CHECK (LEN(LTRIM(RTRIM(username))) > 0),

        CONSTRAINT CK_users_first_name_not_empty
            CHECK (LEN(LTRIM(RTRIM(first_name))) > 0),

        CONSTRAINT CK_users_last_name_not_empty
            CHECK (LEN(LTRIM(RTRIM(last_name))) > 0)
    );
END;
GO

/* ================================================================
   5. ÍNDICES
   ================================================================ */

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_users_role_id'
      AND object_id = OBJECT_ID(N'dbo.users')
)
BEGIN
    CREATE INDEX IX_users_role_id
        ON dbo.users(role_id);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_users_status'
      AND object_id = OBJECT_ID(N'dbo.users')
)
BEGIN
    CREATE INDEX IX_users_status
        ON dbo.users(status);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_users_last_login_at'
      AND object_id = OBJECT_ID(N'dbo.users')
)
BEGIN
    CREATE INDEX IX_users_last_login_at
        ON dbo.users(last_login_at);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_users_created_at'
      AND object_id = OBJECT_ID(N'dbo.users')
)
BEGIN
    CREATE INDEX IX_users_created_at
        ON dbo.users(created_at);
END;
GO

/* ================================================================
   6. DATOS INICIALES: ROLES
   ================================================================ */

IF NOT EXISTS (
    SELECT 1
    FROM dbo.roles
    WHERE name = N'ADMINISTRATOR'
)
BEGIN
    INSERT INTO dbo.roles
    (
        name,
        description
    )
    VALUES
    (
        N'ADMINISTRATOR',
        N'Acceso completo a todos los módulos y configuraciones de Eventix.'
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM dbo.roles
    WHERE name = N'OPERATOR'
)
BEGIN
    INSERT INTO dbo.roles
    (
        name,
        description
    )
    VALUES
    (
        N'OPERATOR',
        N'Gestión operativa de clientes, reservaciones, ventas y pagos.'
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM dbo.roles
    WHERE name = N'ORGANIZER'
)
BEGIN
    INSERT INTO dbo.roles
    (
        name,
        description
    )
    VALUES
    (
        N'ORGANIZER',
        N'Gestión y consulta de eventos pertenecientes al organizador.'
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM dbo.roles
    WHERE name = N'ACCESS_STAFF'
)
BEGIN
    INSERT INTO dbo.roles
    (
        name,
        description
    )
    VALUES
    (
        N'ACCESS_STAFF',
        N'Validación de boletas y control de acceso a eventos.'
    );
END;
GO

/* ================================================================
   7. USUARIO ADMINISTRADOR INICIAL
   Usuario: admin
   Correo: admin@eventix.local
   Contraseña temporal: Admin123*
   Debe cambiarse en el primer inicio de sesión
   ================================================================ */

DECLARE @AdministratorRoleId BIGINT;

SELECT @AdministratorRoleId = id
FROM dbo.roles
WHERE name = N'ADMINISTRATOR';

IF NOT EXISTS (
    SELECT 1
    FROM dbo.users
    WHERE username = N'admin'
       OR email = N'admin@eventix.local'
)
BEGIN
    INSERT INTO dbo.users
    (
        first_name,
        last_name,
        email,
        username,
        password_hash,
        phone,
        role_id,
        status,
        must_change_password,
        last_login_at,
        created_at,
        updated_at,
        created_by,
        updated_by,
        version
    )
    VALUES
    (
        N'Administrador',
        N'Eventix',
        N'admin@eventix.local',
        N'admin',

        /* BCrypt de Admin123* */
        N'$2y$10$BZczENdbL0t0xbe.moKKYeigxOBb27V4zC39JeFPHCSAfx1a9y5I2',

        NULL,
        @AdministratorRoleId,
        N'ACTIVE',
        1,
        NULL,
        SYSDATETIME(),
        SYSDATETIME(),
        N'SYSTEM',
        N'SYSTEM',
        0
    );
END;
GO

/* ================================================================
   8. VISTA DE CONSULTA DE USUARIOS
   Opcional, útil para administración y diagnóstico
   ================================================================ */

CREATE OR ALTER VIEW dbo.vw_users_with_roles
AS
    SELECT
        u.id,
        u.first_name,
        u.last_name,
        CONCAT(u.first_name, N' ', u.last_name) AS full_name,
        u.email,
        u.username,
        u.phone,
        u.status,
        u.must_change_password,
        u.last_login_at,
        r.id AS role_id,
        r.name AS role_name,
        r.description AS role_description,
        u.created_at,
        u.updated_at,
        u.created_by,
        u.updated_by,
        u.version
    FROM dbo.users AS u
    INNER JOIN dbo.roles AS r
        ON r.id = u.role_id;
GO

/* ================================================================
   9. VERIFICACIÓN FINAL
   ================================================================ */

SELECT
    DB_NAME() AS database_name,
    SUSER_SNAME() AS sql_login,
    SYSDATETIME() AS verification_date;
GO

SELECT
    id,
    name,
    description
FROM dbo.roles
ORDER BY id;
GO

SELECT
    id,
    full_name,
    email,
    username,
    role_name,
    status,
    must_change_password,
    created_at
FROM dbo.vw_users_with_roles
ORDER BY id;
GO
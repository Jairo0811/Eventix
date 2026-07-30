/* ================================================================
   EVENTIX
   Preparación de Microsoft SQL Server

   Responsabilidades:
   - Crear EventixDb.
   - Crear el login técnico eventix_app.
   - Crear el usuario dentro de EventixDb.
   - Asignar permisos para JPA y Flyway.

   Las tablas y datos iniciales son administrados por Flyway.
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

/* ================================================================
   2. CREAR LOGIN TÉCNICO
   ================================================================ */

IF NOT EXISTS
(
    SELECT 1
    FROM sys.server_principals
    WHERE name = N'eventix_app'
)
BEGIN
    CREATE LOGIN eventix_app
    WITH
        PASSWORD = N'Eventix2026*',
        CHECK_POLICY = ON,
        CHECK_EXPIRATION = OFF;
END;
GO

/* ================================================================
   3. CREAR USUARIO EN EVENTIXDB
   ================================================================ */

USE EventixDb;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.database_principals
    WHERE name = N'eventix_app'
)
BEGIN
    CREATE USER eventix_app
    FOR LOGIN eventix_app;
END;
GO

/* ================================================================
   4. ASIGNAR PERMISOS
   ================================================================ */

IF IS_ROLEMEMBER(N'db_datareader', N'eventix_app') <> 1
BEGIN
    ALTER ROLE db_datareader
    ADD MEMBER eventix_app;
END;
GO

IF IS_ROLEMEMBER(N'db_datawriter', N'eventix_app') <> 1
BEGIN
    ALTER ROLE db_datawriter
    ADD MEMBER eventix_app;
END;
GO

IF IS_ROLEMEMBER(N'db_ddladmin', N'eventix_app') <> 1
BEGIN
    ALTER ROLE db_ddladmin
    ADD MEMBER eventix_app;
END;
GO

/* ================================================================
   5. VERIFICACIÓN
   ================================================================ */

SELECT
    DB_NAME() AS database_name,
    SUSER_SNAME() AS current_login,
    USER_NAME() AS current_database_user,
    SYSDATETIME() AS verification_date;
GO

SELECT
    dp.name AS database_user,
    rp.name AS database_role
FROM sys.database_role_members AS drm
INNER JOIN sys.database_principals AS rp
    ON rp.principal_id = drm.role_principal_id
INNER JOIN sys.database_principals AS dp
    ON dp.principal_id = drm.member_principal_id
WHERE dp.name = N'eventix_app'
ORDER BY rp.name;
GO
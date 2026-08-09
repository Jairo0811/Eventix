USE master;
GO

IF DB_ID(N'EventixDb') IS NULL
BEGIN
    CREATE DATABASE EventixDb;
END;
GO

ALTER DATABASE EventixDb SET RECOVERY SIMPLE;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.server_principals
    WHERE name = N'eventix_app'
)
BEGIN
    DECLARE @create_login NVARCHAR(MAX);

    SET @create_login =
        N'CREATE LOGIN eventix_app WITH PASSWORD = '
        + QUOTENAME(N'$(EVENTIX_DB_PASSWORD)', '''')
        + N', CHECK_POLICY = ON, CHECK_EXPIRATION = OFF;';

    EXEC sys.sp_executesql @create_login;
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.server_principals
    WHERE name = N'eventix_migrator'
)
BEGIN
    DECLARE @create_migrator_login NVARCHAR(MAX);

    SET @create_migrator_login =
        N'CREATE LOGIN eventix_migrator WITH PASSWORD = '
        + QUOTENAME(N'$(EVENTIX_MIGRATOR_PASSWORD)', '''')
        + N', CHECK_POLICY = ON, CHECK_EXPIRATION = OFF;';

    EXEC sys.sp_executesql @create_migrator_login;
END;
GO

USE EventixDb;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.database_principals
    WHERE name = N'eventix_app'
)
BEGIN
    CREATE USER eventix_app FOR LOGIN eventix_app;
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.database_principals
    WHERE name = N'eventix_migrator'
)
BEGIN
    CREATE USER eventix_migrator FOR LOGIN eventix_migrator;
END;
GO

IF IS_ROLEMEMBER(N'db_datareader', N'eventix_app') <> 1
BEGIN
    ALTER ROLE db_datareader ADD MEMBER eventix_app;
END;
GO

IF IS_ROLEMEMBER(N'db_datawriter', N'eventix_app') <> 1
BEGIN
    ALTER ROLE db_datawriter ADD MEMBER eventix_app;
END;
GO

IF IS_ROLEMEMBER(N'db_ddladmin', N'eventix_app') = 1
BEGIN
    ALTER ROLE db_ddladmin DROP MEMBER eventix_app;
END;
GO

IF IS_ROLEMEMBER(N'db_datareader', N'eventix_migrator') <> 1
BEGIN
    ALTER ROLE db_datareader ADD MEMBER eventix_migrator;
END;
GO

IF IS_ROLEMEMBER(N'db_datawriter', N'eventix_migrator') <> 1
BEGIN
    ALTER ROLE db_datawriter ADD MEMBER eventix_migrator;
END;
GO

IF IS_ROLEMEMBER(N'db_ddladmin', N'eventix_migrator') <> 1
BEGIN
    ALTER ROLE db_ddladmin ADD MEMBER eventix_migrator;
END;
GO

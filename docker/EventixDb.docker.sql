USE master;
GO

IF DB_ID(N'EventixDb') IS NULL
BEGIN
    CREATE DATABASE EventixDb;
END;
GO

IF ISNULL(
    CONVERT(
        NVARCHAR(60),
        DATABASEPROPERTYEX(N'EventixDb', N'Status')
    ),
    N''
) <> N'ONLINE'
BEGIN
    THROW 51000, N'EventixDb is not online.', 1;
END;
GO

ALTER DATABASE EventixDb SET RECOVERY SIMPLE;
GO

DECLARE @eventix_app_password NVARCHAR(128) =
    N'$(EVENTIX_DB_PASSWORD)';
DECLARE @eventix_app_login_sql NVARCHAR(MAX);

IF NOT EXISTS
(
    SELECT 1
    FROM sys.server_principals
    WHERE name = N'eventix_app'
)
BEGIN
    SET @eventix_app_login_sql =
        N'CREATE LOGIN eventix_app WITH PASSWORD = '
        + QUOTENAME(@eventix_app_password, '''')
        + N', CHECK_POLICY = ON, CHECK_EXPIRATION = OFF;';
END
ELSE IF ISNULL
(
    PWDCOMPARE
    (
        @eventix_app_password,
        (
            SELECT password_hash
            FROM sys.sql_logins
            WHERE name = N'eventix_app'
        )
    ),
    0
) = 0
BEGIN
    SET @eventix_app_login_sql =
        N'ALTER LOGIN eventix_app WITH PASSWORD = '
        + QUOTENAME(@eventix_app_password, '''')
        + N';';
END;

IF @eventix_app_login_sql IS NOT NULL
BEGIN
    EXEC sys.sp_executesql @eventix_app_login_sql;
END;
GO

DECLARE @eventix_migrator_password NVARCHAR(128) =
    N'$(EVENTIX_MIGRATOR_PASSWORD)';
DECLARE @eventix_migrator_login_sql NVARCHAR(MAX);

IF NOT EXISTS
(
    SELECT 1
    FROM sys.server_principals
    WHERE name = N'eventix_migrator'
)
BEGIN
    SET @eventix_migrator_login_sql =
        N'CREATE LOGIN eventix_migrator WITH PASSWORD = '
        + QUOTENAME(@eventix_migrator_password, '''')
        + N', CHECK_POLICY = ON, CHECK_EXPIRATION = OFF;';
END
ELSE IF ISNULL
(
    PWDCOMPARE
    (
        @eventix_migrator_password,
        (
            SELECT password_hash
            FROM sys.sql_logins
            WHERE name = N'eventix_migrator'
        )
    ),
    0
) = 0
BEGIN
    SET @eventix_migrator_login_sql =
        N'ALTER LOGIN eventix_migrator WITH PASSWORD = '
        + QUOTENAME(@eventix_migrator_password, '''')
        + N';';
END;

IF @eventix_migrator_login_sql IS NOT NULL
BEGIN
    EXEC sys.sp_executesql @eventix_migrator_login_sql;
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
/* ================================================================
   EVENTIX
   V1 - Creación del esquema de seguridad

   Compatible con:
   - Microsoft SQL Server
   - H2 en MODE=MSSQLServer
   ================================================================ */

CREATE TABLE roles
(
    id BIGINT IDENTITY(1,1) NOT NULL,

    name NVARCHAR(40) NOT NULL,

    description NVARCHAR(160) NOT NULL,

    CONSTRAINT PK_roles
        PRIMARY KEY (id),

    CONSTRAINT UQ_roles_name
        UNIQUE (name),

    CONSTRAINT CK_roles_name
        CHECK
        (
            name IN
            (
                'ADMINISTRATOR',
                'OPERATOR',
                'ORGANIZER',
                'ACCESS_STAFF'
            )
        )
);

CREATE TABLE users
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
        DEFAULT 'ACTIVE',

    must_change_password BIT NOT NULL
        DEFAULT 1,

    last_login_at DATETIME2(6) NULL,

    created_at DATETIME2(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME2(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    created_by NVARCHAR(120) NOT NULL
        DEFAULT 'SYSTEM',

    updated_by NVARCHAR(120) NOT NULL
        DEFAULT 'SYSTEM',

    version BIGINT NOT NULL
        DEFAULT 0,

    CONSTRAINT PK_users
        PRIMARY KEY (id),

    CONSTRAINT FK_users_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id),

    CONSTRAINT UQ_users_email
        UNIQUE (email),

    CONSTRAINT UQ_users_username
        UNIQUE (username),

    CONSTRAINT CK_users_status
        CHECK
        (
            status IN
            (
                'ACTIVE',
                'INACTIVE',
                'LOCKED'
            )
        )
);

CREATE INDEX IX_users_status
    ON users(status);

CREATE INDEX IX_users_role_id
    ON users(role_id);

CREATE INDEX IX_users_name
    ON users(last_name, first_name);

CREATE INDEX IX_users_last_login_at
    ON users(last_login_at);

CREATE INDEX IX_users_created_at
    ON users(created_at);
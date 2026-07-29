CREATE TABLE roles
(
    id BIGINT IDENTITY(1,1) NOT NULL,

    name NVARCHAR(40) NOT NULL,

    description NVARCHAR(160) NOT NULL,

    CONSTRAINT PK_roles
        PRIMARY KEY (id),

    CONSTRAINT UQ_roles_name
        UNIQUE (name)
);
GO

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

    status NVARCHAR(20) NOT NULL,

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

    created_by NVARCHAR(120) NOT NULL,

    updated_by NVARCHAR(120) NOT NULL,

    version BIGINT NOT NULL
        CONSTRAINT DF_users_version
        DEFAULT 0,

    CONSTRAINT PK_users
        PRIMARY KEY (id),

    CONSTRAINT UQ_users_email
        UNIQUE (email),

    CONSTRAINT UQ_users_username
        UNIQUE (username),

    CONSTRAINT FK_users_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id),

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
GO

CREATE INDEX IX_users_status
ON users(status);
GO

CREATE INDEX IX_users_role_id
ON users(role_id);
GO

CREATE INDEX IX_users_name
ON users(last_name, first_name);
GO
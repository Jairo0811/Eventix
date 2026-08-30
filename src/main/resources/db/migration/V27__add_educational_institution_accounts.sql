/* ================================================================
   EVENTIX
   V27 - Cuentas y membresías de centros educativos
   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

ALTER TABLE school_institutions
    ADD status NVARCHAR(30) NOT NULL
        CONSTRAINT DF_school_institutions_status DEFAULT N'ACTIVE';

EXEC sp_executesql N'
    UPDATE school_institutions
    SET status = N''SUSPENDED''
    WHERE active = 0;
';

EXEC sp_executesql N'
    ALTER TABLE school_institutions
        ADD CONSTRAINT CK_school_institutions_status CHECK (
            status IN (
                N''PENDING_VERIFICATION'',
                N''ACTIVE'',
                N''REJECTED'',
                N''SUSPENDED''
            )
        );
';

CREATE TABLE institution_memberships
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    institution_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role NVARCHAR(30) NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT N'ACTIVE',
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT N'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT N'flyway',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_institution_memberships PRIMARY KEY (id),
    CONSTRAINT FK_institution_memberships_institution FOREIGN KEY (institution_id)
        REFERENCES school_institutions(id),
    CONSTRAINT FK_institution_memberships_user FOREIGN KEY (user_id)
        REFERENCES users(id),
    CONSTRAINT UQ_institution_memberships_institution_user UNIQUE (institution_id, user_id),
    CONSTRAINT CK_institution_memberships_role CHECK (
        role IN ('OWNER', 'ADMIN', 'EVENT_MANAGER', 'ROSTER_MANAGER', 'FINANCE')
    ),
    CONSTRAINT CK_institution_memberships_status CHECK (
        status IN ('ACTIVE', 'SUSPENDED')
    )
);

CREATE INDEX IX_institution_memberships_user
    ON institution_memberships(user_id, status);

CREATE INDEX IX_institution_memberships_institution
    ON institution_memberships(institution_id, status);

/* ================================================================
   EVENTIX
   V18 - Núcleo genérico de Eligibility & Benefits

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

CREATE TABLE eligibility_groups
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    event_id BIGINT NOT NULL,
    name NVARCHAR(160) NOT NULL,
    group_type NVARCHAR(30) NOT NULL,
    max_related_people INT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_eligibility_groups PRIMARY KEY (id),
    CONSTRAINT FK_eligibility_groups_event FOREIGN KEY (event_id)
        REFERENCES events(id),
    CONSTRAINT CK_eligibility_groups_type CHECK (
        group_type IN ('PROMOTION_MEMBER', 'ALUMNI', 'FAMILY', 'STAFF', 'VIP', 'CUSTOM')
    ),
    CONSTRAINT CK_eligibility_groups_related_limit CHECK (
        max_related_people IS NULL OR max_related_people >= 0
    )
);

CREATE TABLE eligibility_memberships
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    sponsor_user_id BIGINT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    active BIT NOT NULL DEFAULT 1,
    verified_at DATETIME2(6) NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_eligibility_memberships PRIMARY KEY (id),
    CONSTRAINT FK_eligibility_memberships_group FOREIGN KEY (group_id)
        REFERENCES eligibility_groups(id),
    CONSTRAINT FK_eligibility_memberships_user FOREIGN KEY (user_id)
        REFERENCES users(id),
    CONSTRAINT FK_eligibility_memberships_sponsor FOREIGN KEY (sponsor_user_id)
        REFERENCES users(id),
    CONSTRAINT UQ_eligibility_memberships_group_user UNIQUE (group_id, user_id),
    CONSTRAINT CK_eligibility_memberships_status CHECK (
        status IN ('PENDING', 'VERIFIED', 'REJECTED', 'REVOKED')
    )
);

CREATE TABLE eligibility_benefits
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    group_id BIGINT NOT NULL,
    benefit_type NVARCHAR(30) NOT NULL,
    discount_value DECIMAL(12,2) NULL,
    max_tickets_per_purchase INT NULL,
    reserved_inventory INT NULL,
    ticket_type_id BIGINT NULL,
    early_access_at DATETIME2(6) NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_eligibility_benefits PRIMARY KEY (id),
    CONSTRAINT FK_eligibility_benefits_group FOREIGN KEY (group_id)
        REFERENCES eligibility_groups(id),
    CONSTRAINT FK_eligibility_benefits_ticket_type FOREIGN KEY (ticket_type_id)
        REFERENCES ticket_types(id),
    CONSTRAINT CK_eligibility_benefits_type CHECK (
        benefit_type IN ('PERCENTAGE_DISCOUNT', 'FIXED_DISCOUNT', 'FREE_ENTRY',
                         'EARLY_ACCESS', 'RESERVED_INVENTORY', 'EXCLUSIVE_TICKET',
                         'PURCHASE_LIMIT', 'PRIORITY_ACCESS')
    ),
    CONSTRAINT CK_eligibility_benefits_limits CHECK (
        (discount_value IS NULL OR discount_value >= 0)
        AND (max_tickets_per_purchase IS NULL OR max_tickets_per_purchase > 0)
        AND (reserved_inventory IS NULL OR reserved_inventory >= 0)
    )
);

CREATE INDEX IX_eligibility_groups_event_active
    ON eligibility_groups(event_id, active);
CREATE INDEX IX_eligibility_memberships_user_status
    ON eligibility_memberships(user_id, status, active);
CREATE INDEX IX_eligibility_benefits_group_active
    ON eligibility_benefits(group_id, active);

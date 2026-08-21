/* ================================================================
   EVENTIX
   V16 - Elegibilidad para escuelas y colegios

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

CREATE TABLE school_institutions
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(180) NOT NULL,
    code NVARCHAR(50) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_school_institutions PRIMARY KEY (id),
    CONSTRAINT UQ_school_institutions_code UNIQUE (code)
);

CREATE TABLE school_promotions
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    institution_id BIGINT NOT NULL,
    name NVARCHAR(120) NOT NULL,
    graduation_year INT NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_school_promotions PRIMARY KEY (id),
    CONSTRAINT FK_school_promotions_institution FOREIGN KEY (institution_id)
        REFERENCES school_institutions(id),
    CONSTRAINT UQ_school_promotions_institution_year UNIQUE (institution_id, graduation_year),
    CONSTRAINT CK_school_promotions_year CHECK (graduation_year BETWEEN 1900 AND 2200)
);

CREATE TABLE promotion_members
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    promotion_id BIGINT NOT NULL,
    full_name NVARCHAR(180) NOT NULL,
    student_code NVARCHAR(80) NULL,
    national_id_lookup CHAR(64) NOT NULL,
    national_id_last4 CHAR(4) NOT NULL,
    source_reference NVARCHAR(240) NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_promotion_members PRIMARY KEY (id),
    CONSTRAINT FK_promotion_members_promotion FOREIGN KEY (promotion_id)
        REFERENCES school_promotions(id),
    CONSTRAINT UQ_promotion_members_identity UNIQUE (promotion_id, national_id_lookup)
);

CREATE TABLE school_roster_imports
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    promotion_id BIGINT NOT NULL,
    source_name NVARCHAR(240) NOT NULL,
    file_checksum CHAR(64) NOT NULL,
    imported_by BIGINT NOT NULL,
    imported_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_rows INT NOT NULL,
    accepted_rows INT NOT NULL,
    rejected_rows INT NOT NULL,
    CONSTRAINT PK_school_roster_imports PRIMARY KEY (id),
    CONSTRAINT FK_school_roster_imports_promotion FOREIGN KEY (promotion_id)
        REFERENCES school_promotions(id),
    CONSTRAINT FK_school_roster_imports_user FOREIGN KEY (imported_by)
        REFERENCES users(id),
    CONSTRAINT UQ_school_roster_imports_checksum UNIQUE (promotion_id, file_checksum),
    CONSTRAINT CK_school_roster_imports_counts CHECK (
        total_rows >= 0 AND accepted_rows >= 0 AND rejected_rows >= 0
        AND accepted_rows + rejected_rows = total_rows
    )
);

CREATE TABLE eligibility_verifications
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NOT NULL,
    promotion_member_id BIGINT NOT NULL,
    status NVARCHAR(20) NOT NULL,
    verification_method NVARCHAR(30) NOT NULL,
    verified_by BIGINT NULL,
    verified_at DATETIME2(6) NULL,
    reason NVARCHAR(500) NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_eligibility_verifications PRIMARY KEY (id),
    CONSTRAINT FK_eligibility_verifications_user FOREIGN KEY (user_id)
        REFERENCES users(id),
    CONSTRAINT FK_eligibility_verifications_member FOREIGN KEY (promotion_member_id)
        REFERENCES promotion_members(id),
    CONSTRAINT FK_eligibility_verifications_verified_by FOREIGN KEY (verified_by)
        REFERENCES users(id),
    CONSTRAINT UQ_eligibility_verifications_user_member UNIQUE (user_id, promotion_member_id),
    CONSTRAINT CK_eligibility_verifications_status CHECK (
        status IN ('PENDING', 'VERIFIED', 'REJECTED', 'MANUAL_REVIEW', 'REVOKED')
    ),
    CONSTRAINT CK_eligibility_verifications_method CHECK (
        verification_method IN ('NATIONAL_ID', 'MANUAL_REVIEW', 'INSTITUTIONAL')
    )
);

CREATE TABLE relationship_verifications
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    sponsor_verification_id BIGINT NOT NULL,
    related_user_id BIGINT NOT NULL,
    relationship_type NVARCHAR(30) NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME2(6) NULL,
    reason NVARCHAR(500) NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_relationship_verifications PRIMARY KEY (id),
    CONSTRAINT FK_relationship_verifications_sponsor FOREIGN KEY (sponsor_verification_id)
        REFERENCES eligibility_verifications(id),
    CONSTRAINT FK_relationship_verifications_related_user FOREIGN KEY (related_user_id)
        REFERENCES users(id),
    CONSTRAINT FK_relationship_verifications_reviewed_by FOREIGN KEY (reviewed_by)
        REFERENCES users(id),
    CONSTRAINT UQ_relationship_verifications UNIQUE (sponsor_verification_id, related_user_id),
    CONSTRAINT CK_relationship_verifications_status CHECK (
        status IN ('PENDING', 'VERIFIED', 'REJECTED', 'REVOKED')
    )
);

CREATE TABLE event_eligibility_policies
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    event_id BIGINT NOT NULL,
    promotion_id BIGINT NULL,
    audience_type NVARCHAR(30) NOT NULL,
    discount_type NVARCHAR(20) NULL,
    discount_value DECIMAL(12,2) NULL,
    max_tickets_per_member INT NULL,
    max_related_people INT NULL,
    reserved_inventory INT NULL,
    early_access_at DATETIME2(6) NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_event_eligibility_policies PRIMARY KEY (id),
    CONSTRAINT FK_event_eligibility_policies_event FOREIGN KEY (event_id)
        REFERENCES events(id),
    CONSTRAINT FK_event_eligibility_policies_promotion FOREIGN KEY (promotion_id)
        REFERENCES school_promotions(id),
    CONSTRAINT CK_event_eligibility_policies_audience CHECK (
        audience_type IN ('PROMOTION_MEMBER', 'ALUMNI', 'FAMILY', 'STAFF', 'VIP')
    ),
    CONSTRAINT CK_event_eligibility_policies_discount_type CHECK (
        discount_type IS NULL OR discount_type IN ('PERCENTAGE', 'FIXED', 'FREE')
    ),
    CONSTRAINT CK_event_eligibility_policies_limits CHECK (
        (max_tickets_per_member IS NULL OR max_tickets_per_member > 0)
        AND (max_related_people IS NULL OR max_related_people >= 0)
        AND (reserved_inventory IS NULL OR reserved_inventory >= 0)
    )
);

ALTER TABLE events ADD access_mode NVARCHAR(24) NOT NULL
    CONSTRAINT DF_events_access_mode DEFAULT 'PUBLIC';

ALTER TABLE events ADD CONSTRAINT CK_events_access_mode CHECK (
    access_mode IN ('PUBLIC', 'PRIVATE', 'CONTROLLED_ACCESS')
);

CREATE INDEX IX_school_promotions_institution_year
    ON school_promotions(institution_id, graduation_year);
CREATE INDEX IX_promotion_members_lookup
    ON promotion_members(promotion_id, national_id_lookup, active);
CREATE INDEX IX_eligibility_verifications_user_status
    ON eligibility_verifications(user_id, status);
CREATE INDEX IX_relationship_verifications_related_status
    ON relationship_verifications(related_user_id, status);
CREATE INDEX IX_event_eligibility_policies_event_active
    ON event_eligibility_policies(event_id, active);
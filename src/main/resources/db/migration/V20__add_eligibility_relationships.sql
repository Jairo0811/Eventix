/* ================================================================
   EVENTIX
   V20 - Solicitudes de relaciones familiares verificables

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

CREATE TABLE eligibility_relationships
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    group_id BIGINT NOT NULL,
    sponsor_user_id BIGINT NOT NULL,
    related_user_id BIGINT NOT NULL,
    relationship_type NVARCHAR(30) NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    request_note NVARCHAR(500) NULL,
    decision_reason NVARCHAR(500) NULL,
    decided_by_id BIGINT NULL,
    decided_at DATETIME2(6) NULL,
    revoked_at DATETIME2(6) NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT PK_eligibility_relationships PRIMARY KEY (id),
    CONSTRAINT FK_eligibility_relationships_group FOREIGN KEY (group_id)
        REFERENCES eligibility_groups(id),
    CONSTRAINT FK_eligibility_relationships_sponsor FOREIGN KEY (sponsor_user_id)
        REFERENCES users(id),
    CONSTRAINT FK_eligibility_relationships_related FOREIGN KEY (related_user_id)
        REFERENCES users(id),
    CONSTRAINT FK_eligibility_relationships_decided_by FOREIGN KEY (decided_by_id)
        REFERENCES users(id),
    CONSTRAINT CK_eligibility_relationships_distinct_users CHECK (
        sponsor_user_id <> related_user_id
    ),
    CONSTRAINT CK_eligibility_relationships_type CHECK (
        relationship_type IN ('PARENT', 'CHILD', 'SIBLING', 'SPOUSE', 'GUARDIAN', 'OTHER')
    ),
    CONSTRAINT CK_eligibility_relationships_status CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED', 'REVOKED')
    )
);

CREATE INDEX IX_eligibility_relationships_group_sponsor_status
    ON eligibility_relationships(group_id, sponsor_user_id, status);

CREATE INDEX IX_eligibility_relationships_related_status
    ON eligibility_relationships(related_user_id, status);

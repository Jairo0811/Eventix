/* ================================================================
   EVENTIX
   V25 - Padrón escolar basado en nombres, sin cédula

   La cédula pasa a ser únicamente una evidencia transitoria para consultar
   una fuente de identidad. El padrón conserva nombres y metadatos escolares.
   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_promotion_members_lookup'
      AND object_id = OBJECT_ID('promotion_members')
)
BEGIN
    DROP INDEX IX_promotion_members_lookup ON promotion_members;
END;

IF EXISTS (
    SELECT 1
    FROM sys.key_constraints
    WHERE name = 'UQ_promotion_members_identity'
      AND parent_object_id = OBJECT_ID('promotion_members')
)
BEGIN
    ALTER TABLE promotion_members DROP CONSTRAINT UQ_promotion_members_identity;
END;

IF COL_LENGTH('promotion_members', 'national_id_lookup') IS NOT NULL
BEGIN
    ALTER TABLE promotion_members DROP COLUMN national_id_lookup;
END;

IF COL_LENGTH('promotion_members', 'national_id_last4') IS NOT NULL
BEGIN
    ALTER TABLE promotion_members DROP COLUMN national_id_last4;
END;

IF EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'CK_eligibility_verification_attempts_result'
      AND parent_object_id = OBJECT_ID('eligibility_verification_attempts')
)
BEGIN
    ALTER TABLE eligibility_verification_attempts
        DROP CONSTRAINT CK_eligibility_verification_attempts_result;
END;

ALTER TABLE eligibility_verification_attempts
    ADD CONSTRAINT CK_eligibility_verification_attempts_result CHECK (
        result IN (
            'VERIFIED',
            'MANUAL_REVIEW',
            'NO_MATCH',
            'IDENTITY_NOT_FOUND',
            'PROVIDER_UNAVAILABLE',
            'REJECTED'
        )
    );

CREATE INDEX IX_promotion_members_promotion_active_name
    ON promotion_members(promotion_id, active, full_name);

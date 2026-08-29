/* ================================================================
   EVENTIX
   V25 - Padrón escolar sin cédula + verificación por nombre oficial

   El padrón deja de usar la cédula como identificador. La cédula se utiliza
   únicamente durante la verificación de identidad y el nombre obtenido del
   proveedor autorizado se compara contra el padrón.

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

DROP INDEX IX_promotion_members_lookup ON promotion_members;
ALTER TABLE promotion_members DROP CONSTRAINT UQ_promotion_members_identity;

ALTER TABLE promotion_members ALTER COLUMN national_id_lookup VARCHAR(64) NULL;
ALTER TABLE promotion_members ALTER COLUMN national_id_last4 VARCHAR(4) NULL;

-- El padrón ya no conserva identificadores nacionales, ni siquiera protegidos.
UPDATE promotion_members
SET national_id_lookup = NULL,
    national_id_last4 = NULL;

ALTER TABLE promotion_members ADD normalized_full_name NVARCHAR(180) NULL;

UPDATE promotion_members
SET normalized_full_name = UPPER(LTRIM(RTRIM(full_name)));

WHILE EXISTS (SELECT 1 FROM promotion_members WHERE normalized_full_name LIKE N'%  %')
BEGIN
    UPDATE promotion_members
    SET normalized_full_name = REPLACE(normalized_full_name, N'  ', N' ')
    WHERE normalized_full_name LIKE N'%  %';
END;

UPDATE promotion_members
SET normalized_full_name = TRANSLATE(
        normalized_full_name,
        N'ÁÉÍÓÚÜÑÀÈÌÒÙÂÊÎÔÛ',
        N'AEIOUUNAEIOUAEIOU');

ALTER TABLE promotion_members ALTER COLUMN normalized_full_name NVARCHAR(180) NOT NULL;

CREATE INDEX IX_promotion_members_name
    ON promotion_members(promotion_id, normalized_full_name, active);

ALTER TABLE eligibility_verification_attempts
    DROP CONSTRAINT CK_eligibility_verification_attempts_result;

ALTER TABLE eligibility_verification_attempts
    ADD CONSTRAINT CK_eligibility_verification_attempts_result CHECK (
        result IN (
            'VERIFIED',
            'MANUAL_REVIEW',
            'NO_MATCH',
            'REJECTED',
            'IDENTITY_NOT_FOUND',
            'PROVIDER_UNAVAILABLE',
            'AMBIGUOUS_MATCH'
        )
    );

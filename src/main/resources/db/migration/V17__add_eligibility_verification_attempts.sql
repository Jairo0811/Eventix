/* ================================================================
   EVENTIX
   V17 - Auditoría de intentos de verificación de elegibilidad

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

CREATE TABLE eligibility_verification_attempts
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NOT NULL,
    promotion_id BIGINT NOT NULL,
    national_id_lookup CHAR(64) NOT NULL,
    national_id_last4 CHAR(4) NOT NULL,
    result NVARCHAR(30) NOT NULL,
    reason NVARCHAR(500) NULL,
    attempted_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT PK_eligibility_verification_attempts PRIMARY KEY (id),
    CONSTRAINT FK_eligibility_verification_attempts_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT FK_eligibility_verification_attempts_promotion
        FOREIGN KEY (promotion_id) REFERENCES school_promotions(id),
    CONSTRAINT CK_eligibility_verification_attempts_result CHECK (
        result IN ('VERIFIED', 'MANUAL_REVIEW', 'NO_MATCH', 'REJECTED')
    )
);

CREATE INDEX IX_eligibility_verification_attempts_user_time
    ON eligibility_verification_attempts(user_id, attempted_at DESC);

CREATE INDEX IX_eligibility_verification_attempts_lookup_time
    ON eligibility_verification_attempts(national_id_lookup, attempted_at DESC);

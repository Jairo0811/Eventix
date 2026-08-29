/* ================================================================
   EVENTIX
   V24 - Vincula promociones escolares con grupos de elegibilidad

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

ALTER TABLE eligibility_groups ADD school_promotion_id BIGINT NULL;
GO

ALTER TABLE eligibility_groups ADD CONSTRAINT FK_eligibility_groups_school_promotion
    FOREIGN KEY (school_promotion_id) REFERENCES school_promotions(id);
GO

CREATE INDEX IX_eligibility_groups_school_promotion_active
    ON eligibility_groups(school_promotion_id, active)
    WHERE school_promotion_id IS NOT NULL;

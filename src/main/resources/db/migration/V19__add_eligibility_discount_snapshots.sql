/* ================================================================
   EVENTIX
   V19 - Snapshots de descuentos por Eligibility & Benefits

   Compatible con Microsoft SQL Server 2022.
   Mantiene cupones y elegibilidad como fuentes de descuento separadas.
   ================================================================ */

ALTER TABLE dbo.sales DROP CONSTRAINT CK_sales_coupon_snapshot;

ALTER TABLE dbo.sales ADD
    coupon_discount_amount DECIMAL(12,2) NOT NULL
        CONSTRAINT DF_sales_coupon_discount_amount DEFAULT 0,
    eligibility_benefit_id BIGINT NULL,
    eligibility_benefit_type NVARCHAR(30) NULL,
    eligibility_discount_value DECIMAL(12,2) NULL,
    eligibility_discount_amount DECIMAL(12,2) NOT NULL
        CONSTRAINT DF_sales_eligibility_discount_amount DEFAULT 0;

UPDATE dbo.sales
SET coupon_discount_amount = discount_total
WHERE coupon_id IS NOT NULL;

ALTER TABLE dbo.sales ADD CONSTRAINT FK_sales_eligibility_benefit
    FOREIGN KEY (eligibility_benefit_id) REFERENCES eligibility_benefits(id);

ALTER TABLE dbo.sales ADD CONSTRAINT CK_sales_discount_snapshot CHECK (
    discount_total >= 0
    AND discount_total <= subtotal
    AND coupon_discount_amount >= 0
    AND eligibility_discount_amount >= 0
    AND discount_total = coupon_discount_amount + eligibility_discount_amount
    AND NOT (coupon_id IS NOT NULL AND eligibility_benefit_id IS NOT NULL)
    AND (
        (
            coupon_id IS NULL
            AND coupon_code IS NULL
            AND coupon_discount_type IS NULL
            AND coupon_discount_value IS NULL
            AND coupon_discount_amount = 0
        )
        OR
        (
            coupon_id IS NOT NULL
            AND coupon_code IS NOT NULL
            AND coupon_discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')
            AND coupon_discount_value > 0
            AND coupon_discount_amount > 0
        )
    )
    AND (
        (
            eligibility_benefit_id IS NULL
            AND eligibility_benefit_type IS NULL
            AND eligibility_discount_value IS NULL
            AND eligibility_discount_amount = 0
        )
        OR
        (
            eligibility_benefit_id IS NOT NULL
            AND eligibility_benefit_type IN (
                'PERCENTAGE_DISCOUNT',
                'FIXED_DISCOUNT',
                'FREE_ENTRY'
            )
            AND eligibility_discount_amount > 0
            AND (
                (
                    eligibility_benefit_type = 'FREE_ENTRY'
                    AND eligibility_discount_value IS NULL
                )
                OR
                (
                    eligibility_benefit_type = 'PERCENTAGE_DISCOUNT'
                    AND eligibility_discount_value > 0
                    AND eligibility_discount_value <= 100
                )
                OR
                (
                    eligibility_benefit_type = 'FIXED_DISCOUNT'
                    AND eligibility_discount_value > 0
                )
            )
        )
    )
);

CREATE INDEX IX_sales_eligibility_benefit
    ON dbo.sales(eligibility_benefit_id)
    WHERE eligibility_benefit_id IS NOT NULL;

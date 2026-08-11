/* ================================================================
   EVENTIX
   V13 - Cupones, promociones y snapshots financieros de venta

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

CREATE TABLE coupons
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    code NVARCHAR(40) NOT NULL,
    description NVARCHAR(240) NOT NULL,
    discount_type NVARCHAR(20) NOT NULL,
    value DECIMAL(12,2) NOT NULL,
    starts_at DATETIME2(6) NOT NULL,
    expires_at DATETIME2(6) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    total_use_limit INT NULL,
    current_uses INT NOT NULL DEFAULT 0,
    per_user_limit INT NULL,
    minimum_subtotal DECIMAL(12,2) NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT PK_coupons PRIMARY KEY (id),
    CONSTRAINT UQ_coupons_code UNIQUE (code),
    CONSTRAINT CK_coupons_discount_type
        CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT CK_coupons_value CHECK (
        value > 0
        AND (discount_type <> 'PERCENTAGE' OR value <= 100)
    ),
    CONSTRAINT CK_coupons_dates CHECK (expires_at > starts_at),
    CONSTRAINT CK_coupons_limits CHECK (
        (total_use_limit IS NULL OR total_use_limit > 0)
        AND current_uses >= 0
        AND (total_use_limit IS NULL OR current_uses <= total_use_limit)
        AND (per_user_limit IS NULL OR per_user_limit > 0)
    ),
    CONSTRAINT CK_coupons_minimum_subtotal
        CHECK (minimum_subtotal IS NULL OR minimum_subtotal >= 0)
);

CREATE TABLE coupon_events
(
    coupon_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,

    CONSTRAINT PK_coupon_events PRIMARY KEY (coupon_id, event_id),
    CONSTRAINT FK_coupon_events_coupon
        FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    CONSTRAINT FK_coupon_events_event
        FOREIGN KEY (event_id) REFERENCES events(id)
);

ALTER TABLE sales ADD
    coupon_id BIGINT NULL,
    coupon_code NVARCHAR(40) NULL,
    coupon_discount_type NVARCHAR(20) NULL,
    coupon_discount_value DECIMAL(12,2) NULL;

ALTER TABLE sales ADD CONSTRAINT FK_sales_coupon
    FOREIGN KEY (coupon_id) REFERENCES coupons(id);

ALTER TABLE sales ADD CONSTRAINT CK_sales_coupon_snapshot CHECK (
    (
        coupon_id IS NULL
        AND coupon_code IS NULL
        AND coupon_discount_type IS NULL
        AND coupon_discount_value IS NULL
        AND discount_total = 0
    )
    OR
    (
        coupon_id IS NOT NULL
        AND coupon_code IS NOT NULL
        AND coupon_discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')
        AND coupon_discount_value > 0
        AND discount_total >= 0
    )
);

CREATE TABLE coupon_redemptions
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    coupon_id BIGINT NOT NULL,
    sale_id BIGINT NOT NULL,
    buyer_email NVARCHAR(160) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL,
    status NVARCHAR(20) NOT NULL,
    reserved_at DATETIME2(6) NOT NULL,
    consumed_at DATETIME2(6) NULL,
    released_at DATETIME2(6) NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT PK_coupon_redemptions PRIMARY KEY (id),
    CONSTRAINT UQ_coupon_redemptions_sale UNIQUE (sale_id),
    CONSTRAINT FK_coupon_redemptions_coupon
        FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    CONSTRAINT FK_coupon_redemptions_sale
        FOREIGN KEY (sale_id) REFERENCES sales(id),
    CONSTRAINT CK_coupon_redemptions_amount
        CHECK (discount_amount >= 0),
    CONSTRAINT CK_coupon_redemptions_status
        CHECK (status IN ('RESERVED', 'CONSUMED', 'RELEASED')),
    CONSTRAINT CK_coupon_redemptions_lifecycle CHECK (
        (status = 'RESERVED' AND consumed_at IS NULL AND released_at IS NULL)
        OR
        (status = 'CONSUMED' AND consumed_at IS NOT NULL AND released_at IS NULL)
        OR
        (status = 'RELEASED' AND consumed_at IS NULL AND released_at IS NOT NULL)
    )
);

CREATE INDEX IX_coupons_active_dates
    ON coupons(active, starts_at, expires_at);

CREATE INDEX IX_coupon_events_event
    ON coupon_events(event_id, coupon_id);

CREATE INDEX IX_sales_coupon
    ON sales(coupon_id);

CREATE INDEX IX_coupon_redemptions_coupon_buyer_status
    ON coupon_redemptions(coupon_id, buyer_email, status);

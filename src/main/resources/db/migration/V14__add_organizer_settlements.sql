/* ================================================================
   EVENTIX
   V14 - Liquidaciones persistentes a organizadores

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

CREATE TABLE organizer_settlements
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    organizer_id BIGINT NOT NULL,
    period_from DATE NOT NULL,
    period_to DATE NOT NULL,
    gross_sales DECIMAL(18,2) NOT NULL,
    discounts DECIMAL(18,2) NOT NULL,
    refunds DECIMAL(18,2) NOT NULL,
    platform_commission DECIMAL(18,2) NOT NULL,
    organizer_net DECIMAL(18,2) NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processed_at DATETIME2(6) NULL,
    paid_at DATETIME2(6) NULL,
    external_reference NVARCHAR(120) NULL,
    administrative_notes NVARCHAR(1000) NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT PK_organizer_settlements PRIMARY KEY (id),
    CONSTRAINT FK_organizer_settlements_organizer
        FOREIGN KEY (organizer_id) REFERENCES users(id),
    CONSTRAINT CK_organizer_settlements_period
        CHECK (period_to >= period_from),
    CONSTRAINT CK_organizer_settlements_amounts CHECK (
        gross_sales >= 0
        AND discounts >= 0
        AND discounts <= gross_sales
        AND refunds >= 0
        AND organizer_net =
            gross_sales - discounts - refunds - platform_commission
    ),
    CONSTRAINT CK_organizer_settlements_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'PAID', 'FAILED', 'CANCELLED')
    ),
    CONSTRAINT CK_organizer_settlements_processed CHECK (
        status NOT IN ('PROCESSING', 'PAID', 'FAILED')
        OR processed_at IS NOT NULL
    ),
    CONSTRAINT CK_organizer_settlements_paid CHECK (
        status <> 'PAID' OR paid_at IS NOT NULL
    )
);

CREATE TABLE organizer_settlement_lines
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    settlement_id BIGINT NOT NULL,
    sale_id BIGINT NOT NULL,
    line_type NVARCHAR(20) NOT NULL,
    gross_amount DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL,
    refund_amount DECIMAL(12,2) NOT NULL,
    platform_commission DECIMAL(12,2) NOT NULL,
    organizer_net DECIMAL(12,2) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT PK_organizer_settlement_lines PRIMARY KEY (id),
    CONSTRAINT FK_organizer_settlement_lines_settlement
        FOREIGN KEY (settlement_id) REFERENCES organizer_settlements(id),
    CONSTRAINT FK_organizer_settlement_lines_sale
        FOREIGN KEY (sale_id) REFERENCES sales(id),
    CONSTRAINT CK_organizer_settlement_lines_type
        CHECK (line_type IN ('SALE', 'REFUND')),
    CONSTRAINT CK_organizer_settlement_lines_identity CHECK (
        organizer_net =
            gross_amount - discount_amount - refund_amount
            - platform_commission
    ),
    CONSTRAINT CK_organizer_settlement_lines_values CHECK (
        (
            line_type = 'SALE'
            AND gross_amount >= 0
            AND discount_amount >= 0
            AND discount_amount <= gross_amount
            AND refund_amount = 0
            AND platform_commission >= 0
            AND organizer_net >= 0
        )
        OR
        (
            line_type = 'REFUND'
            AND gross_amount = 0
            AND discount_amount = 0
            AND refund_amount >= 0
            AND platform_commission <= 0
            AND organizer_net <= 0
        )
    )
);

CREATE INDEX IX_organizer_settlements_organizer_status_period
    ON organizer_settlements(organizer_id, status, period_to);

CREATE INDEX IX_organizer_settlements_created_at
    ON organizer_settlements(created_at);

CREATE INDEX IX_organizer_settlement_lines_settlement
    ON organizer_settlement_lines(settlement_id, active);

CREATE UNIQUE INDEX UX_organizer_settlement_lines_active_effect
    ON organizer_settlement_lines(sale_id, line_type)
    WHERE active = 1;

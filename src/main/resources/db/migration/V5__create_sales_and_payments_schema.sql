/* ================================================================
   EVENTIX
   V5 - Venta de entradas, tipos de entrada y pagos

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

CREATE TABLE ticket_types
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    event_id BIGINT NOT NULL,
    category NVARCHAR(20) NOT NULL,
    name NVARCHAR(80) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    capacity INT NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT PK_ticket_types PRIMARY KEY (id),
    CONSTRAINT FK_ticket_types_event
        FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT UQ_ticket_types_event_name UNIQUE (event_id, name),
    CONSTRAINT CK_ticket_types_category
        CHECK (category IN (
            'GENERAL', 'VIP', 'PREFERENTIAL', 'STUDENT',
            'COMPLIMENTARY', 'CUSTOM'
        )),
    CONSTRAINT CK_ticket_types_price CHECK (price >= 0),
    CONSTRAINT CK_ticket_types_capacity CHECK (capacity > 0),
    CONSTRAINT CK_ticket_types_complimentary_price
        CHECK (category <> 'COMPLIMENTARY' OR price = 0)
);

CREATE TABLE sales
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    reference_code NVARCHAR(24) NOT NULL,
    reservation_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    buyer_name NVARCHAR(161) NOT NULL,
    buyer_email NVARCHAR(160) NOT NULL,
    buyer_phone NVARCHAR(30) NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    currency NVARCHAR(3) NOT NULL DEFAULT 'DOP',
    subtotal DECIMAL(12,2) NOT NULL,
    discount_total DECIMAL(12,2) NOT NULL DEFAULT 0,
    total DECIMAL(12,2) NOT NULL,
    paid_at DATETIME2(6) NULL,
    refunded_at DATETIME2(6) NULL,
    refund_reason NVARCHAR(500) NULL,
    cancelled_at DATETIME2(6) NULL,
    cancellation_reason NVARCHAR(500) NULL,
    sold_by_id BIGINT NOT NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT PK_sales PRIMARY KEY (id),
    CONSTRAINT UQ_sales_reference_code UNIQUE (reference_code),
    CONSTRAINT UQ_sales_reservation UNIQUE (reservation_id),
    CONSTRAINT FK_sales_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations(id),
    CONSTRAINT FK_sales_event
        FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT FK_sales_sold_by
        FOREIGN KEY (sold_by_id) REFERENCES users(id),
    CONSTRAINT CK_sales_status
        CHECK (status IN ('PENDING', 'PAID', 'REFUNDED', 'CANCELLED')),
    CONSTRAINT CK_sales_currency CHECK (LEN(currency) = 3),
    CONSTRAINT CK_sales_amounts CHECK (
        subtotal >= 0
        AND discount_total >= 0
        AND discount_total <= subtotal
        AND total = subtotal - discount_total
    ),
    CONSTRAINT CK_sales_paid
        CHECK (status <> 'PAID' OR paid_at IS NOT NULL),
    CONSTRAINT CK_sales_refunded
        CHECK (
            status <> 'REFUNDED'
            OR (
                refunded_at IS NOT NULL
                AND refund_reason IS NOT NULL
                AND LEN(LTRIM(RTRIM(refund_reason))) > 0
            )
        ),
    CONSTRAINT CK_sales_cancelled
        CHECK (
            status <> 'CANCELLED'
            OR (
                cancelled_at IS NOT NULL
                AND cancellation_reason IS NOT NULL
                AND LEN(LTRIM(RTRIM(cancellation_reason))) > 0
            )
        )
);

CREATE TABLE sale_items
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    sale_id BIGINT NOT NULL,
    ticket_type_id BIGINT NOT NULL,
    ticket_type_name NVARCHAR(80) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,

    CONSTRAINT PK_sale_items PRIMARY KEY (id),
    CONSTRAINT FK_sale_items_sale
        FOREIGN KEY (sale_id) REFERENCES sales(id),
    CONSTRAINT FK_sale_items_ticket_type
        FOREIGN KEY (ticket_type_id) REFERENCES ticket_types(id),
    CONSTRAINT UQ_sale_items_sale_ticket_type
        UNIQUE (sale_id, ticket_type_id),
    CONSTRAINT CK_sale_items_quantity CHECK (quantity > 0),
    CONSTRAINT CK_sale_items_price CHECK (unit_price >= 0),
    CONSTRAINT CK_sale_items_subtotal
        CHECK (subtotal = unit_price * quantity)
);

CREATE TABLE payment_transactions
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    sale_id BIGINT NOT NULL,
    transaction_reference NVARCHAR(40) NOT NULL,
    provider NVARCHAR(20) NOT NULL,
    transaction_type NVARCHAR(20) NOT NULL,
    status NVARCHAR(20) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency NVARCHAR(3) NOT NULL,
    external_reference NVARCHAR(120) NULL,
    response_message NVARCHAR(300) NULL,
    processed_at DATETIME2(6) NOT NULL,
    processed_by_id BIGINT NOT NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    updated_by NVARCHAR(120) NOT NULL DEFAULT 'flyway',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT PK_payment_transactions PRIMARY KEY (id),
    CONSTRAINT UQ_payment_transactions_reference
        UNIQUE (transaction_reference),
    CONSTRAINT FK_payment_transactions_sale
        FOREIGN KEY (sale_id) REFERENCES sales(id),
    CONSTRAINT FK_payment_transactions_processed_by
        FOREIGN KEY (processed_by_id) REFERENCES users(id),
    CONSTRAINT CK_payment_transactions_provider
        CHECK (provider IN (
            'STRIPE', 'PAYPAL', 'AZUL', 'CARDNET', 'QIK',
            'BANK_TRANSFER'
        )),
    CONSTRAINT CK_payment_transactions_type
        CHECK (transaction_type IN ('CHARGE', 'REFUND')),
    CONSTRAINT CK_payment_transactions_status
        CHECK (status IN ('APPROVED', 'DECLINED')),
    CONSTRAINT CK_payment_transactions_amount CHECK (amount >= 0),
    CONSTRAINT CK_payment_transactions_currency CHECK (LEN(currency) = 3)
);

CREATE INDEX IX_ticket_types_event_active
    ON ticket_types(event_id, active);

CREATE INDEX IX_sales_event_status
    ON sales(event_id, status);

CREATE INDEX IX_sales_buyer_email
    ON sales(buyer_email);

CREATE INDEX IX_sales_created_at
    ON sales(created_at);

CREATE INDEX IX_sale_items_ticket_type
    ON sale_items(ticket_type_id);

CREATE INDEX IX_payment_transactions_sale_processed_at
    ON payment_transactions(sale_id, processed_at);

INSERT INTO ticket_types
    (event_id, category, name, price, capacity, active)
SELECT
    e.id,
    'GENERAL',
    N'General',
    e.base_price,
    e.capacity,
    1
FROM events e;

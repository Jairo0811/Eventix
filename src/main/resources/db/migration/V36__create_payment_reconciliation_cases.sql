/* Eventix v1.4 — independent read-only bank-vs-ledger reconciliation.
   No webhook endpoint, payment approval, charge, refund or ticket issuance.
   Never store raw request bodies, PAN/CVC or full wallet tokens here.
*/
CREATE TABLE dbo.payment_reconciliation_cases
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    provider NVARCHAR(20) NOT NULL,
    external_event_id NVARCHAR(160) NOT NULL,
    sale_reference NVARCHAR(24) NOT NULL,
    transaction_type NVARCHAR(20) NOT NULL,
    external_reference NVARCHAR(120) NOT NULL,
    reported_status NVARCHAR(20) NOT NULL,
    reported_amount DECIMAL(12,2) NOT NULL,
    currency NVARCHAR(3) NOT NULL,
    outcome NVARCHAR(20) NOT NULL,
    reason_code NVARCHAR(40) NOT NULL,
    reviewed_at DATETIME2(6) NOT NULL,
    CONSTRAINT PK_payment_reconciliation_cases PRIMARY KEY (id),
    CONSTRAINT UQ_payment_reconciliation_provider_event
        UNIQUE (provider, external_event_id),
    CONSTRAINT CK_payment_reconciliation_status
        CHECK (reported_status IN ('APPROVED', 'DECLINED')),
    CONSTRAINT CK_payment_reconciliation_type
        CHECK (transaction_type IN ('CHARGE', 'REFUND')),
    CONSTRAINT CK_payment_reconciliation_amount CHECK (reported_amount > 0),
    CONSTRAINT CK_payment_reconciliation_outcome
        CHECK (outcome IN ('MATCHED', 'REVIEW_REQUIRED')),
    CONSTRAINT CK_payment_reconciliation_reason
        CHECK (reason_code IN (
            'MATCHED', 'BANK_DECLINED', 'NO_LOCAL_APPROVAL',
            'REFERENCE_MISMATCH', 'AMOUNT_MISMATCH', 'CURRENCY_MISMATCH'))
);

CREATE INDEX IX_payment_reconciliation_reviewed
    ON dbo.payment_reconciliation_cases(outcome, reviewed_at);
CREATE INDEX IX_payment_reconciliation_sale
    ON dbo.payment_reconciliation_cases(sale_reference, reviewed_at);

/* ================================================================
   EVENTIX
   V22 - Trazabilidad de reembolsos parciales en liquidaciones

   Compatible con Microsoft SQL Server 2022.
   ================================================================ */

DROP INDEX UX_organizer_settlement_lines_active_effect
    ON dbo.organizer_settlement_lines;

ALTER TABLE dbo.organizer_settlement_lines
ADD payment_transaction_id BIGINT NULL;

GO

ALTER TABLE dbo.organizer_settlement_lines
ADD CONSTRAINT FK_organizer_settlement_lines_payment_transaction
    FOREIGN KEY (payment_transaction_id)
    REFERENCES dbo.payment_transactions(id);

CREATE UNIQUE INDEX UX_organizer_settlement_lines_active_sale
    ON dbo.organizer_settlement_lines(sale_id, line_type)
    WHERE active = 1 AND line_type = 'SALE';

CREATE UNIQUE INDEX UX_organizer_settlement_lines_active_refund_transaction
    ON dbo.organizer_settlement_lines(payment_transaction_id)
    WHERE active = 1 AND payment_transaction_id IS NOT NULL;

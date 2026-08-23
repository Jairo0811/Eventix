ALTER TABLE dbo.sales
ADD refunded_amount DECIMAL(12,2) NOT NULL
    CONSTRAINT DF_sales_refunded_amount DEFAULT 0 WITH VALUES;

GO

ALTER TABLE dbo.sales
ADD CONSTRAINT CK_sales_refunded_amount
CHECK (refunded_amount >= 0 AND refunded_amount <= total);

ALTER TABLE dbo.sales DROP COLUMN platform_fee_amount;
ALTER TABLE dbo.sales DROP COLUMN organizer_net_amount;

EXEC(N'
ALTER TABLE dbo.sales
ADD platform_fee_amount AS
    CAST(ROUND((total - refunded_amount) * platform_fee_rate, 2) AS DECIMAL(12,2)) PERSISTED,
    organizer_net_amount AS
    CAST((total - refunded_amount) - ROUND((total - refunded_amount) * platform_fee_rate, 2) AS DECIMAL(12,2)) PERSISTED;
');

UPDATE dbo.sales
SET refunded_amount = total
WHERE status = 'REFUNDED';

ALTER TABLE dbo.sales
ADD platform_fee_rate DECIMAL(5,4) NULL;
GO

UPDATE dbo.sales
SET platform_fee_rate = 0
WHERE platform_fee_rate IS NULL;
GO

ALTER TABLE dbo.sales
ALTER COLUMN platform_fee_rate DECIMAL(5,4) NOT NULL;
GO

ALTER TABLE dbo.sales
ADD CONSTRAINT DF_sales_platform_fee_rate
DEFAULT 0.0500 FOR platform_fee_rate;
GO

ALTER TABLE dbo.sales
ADD platform_fee_amount AS
    CAST(ROUND(total * platform_fee_rate, 2) AS DECIMAL(12,2)) PERSISTED,
    organizer_net_amount AS
    CAST(total - ROUND(total * platform_fee_rate, 2) AS DECIMAL(12,2)) PERSISTED;
GO

ALTER TABLE dbo.sales
ADD CONSTRAINT CK_sales_platform_fee_rate
CHECK (platform_fee_rate >= 0 AND platform_fee_rate <= 0.25);

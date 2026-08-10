EXEC(N'
ALTER TABLE dbo.sales
ADD platform_fee_rate DECIMAL(5,4) NOT NULL
    CONSTRAINT DF_sales_platform_fee_rate DEFAULT 0 WITH VALUES;
');

EXEC(N'
ALTER TABLE dbo.sales
DROP CONSTRAINT DF_sales_platform_fee_rate;
');

EXEC(N'
ALTER TABLE dbo.sales
ADD CONSTRAINT DF_sales_platform_fee_rate
DEFAULT 0.0500 FOR platform_fee_rate;
');

EXEC(N'
ALTER TABLE dbo.sales
ADD platform_fee_amount AS
    CAST(ROUND(total * platform_fee_rate, 2) AS DECIMAL(12,2)) PERSISTED,
    organizer_net_amount AS
    CAST(total - ROUND(total * platform_fee_rate, 2) AS DECIMAL(12,2)) PERSISTED;
');

EXEC(N'
ALTER TABLE dbo.sales
ADD CONSTRAINT CK_sales_platform_fee_rate
CHECK (platform_fee_rate >= 0 AND platform_fee_rate <= 0.25);
');

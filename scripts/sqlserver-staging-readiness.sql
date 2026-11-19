/*
 * Eventix — SQL Server staging readiness inspection (read-only).
 * Run only against the explicitly approved STAGING SQL Server database.
 * Do not put passwords in this script or commit credentials to the repo.
 *
 * Example via sqlcmd -S <approved-host> -d <staging-db> -U <read-only-user>
 * This script performs SELECT queries only; it does not back up or migrate.
*/
SET NOCOUNT ON;

SELECT DB_NAME() AS database_name,
       d.state_desc,
       d.recovery_model_desc,
       d.is_read_only,
       d.collation_name
FROM sys.databases d
WHERE d.database_id = DB_ID();

SELECT s.version,
       s.description,
       s.installed_on,
       s.success
FROM dbo.flyway_schema_history s
WHERE s.version IN ('35', '36')
ORDER BY s.installed_rank;

SELECT i.name AS unique_index,
       t.name AS table_name,
       i.is_unique,
       i.is_disabled
FROM sys.indexes i
JOIN sys.tables t ON t.object_id = i.object_id
WHERE (t.name = 'payment_webhook_inbox'
       AND i.name = 'UQ_payment_webhook_inbox_provider_event')
   OR (t.name = 'payment_reconciliation_cases'
       AND i.name = 'UQ_payment_reconciliation_provider_event');

SELECT TOP (1)
       b.backup_finish_date,
       b.type AS backup_type,
       b.is_copy_only,
       b.has_backup_checksums
FROM msdb.dbo.backupset b
WHERE b.database_name = DB_NAME()
  AND b.type = 'D'
ORDER BY b.backup_finish_date DESC;

/* A recent backup record is NOT proof of restorability.
 * Always perform a separate isolated RESTORE/DBCC CHECKDB exercise.
 */

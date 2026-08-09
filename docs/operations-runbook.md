# Operación, respaldo y recuperación

## Verificación diaria

1. Consulta `/actuator/health/readiness`.
2. Confirma espacio en disco y crecimiento de SQL Server.
3. Revisa fallos de autenticación, errores y rechazos en Auditoría.
4. Verifica que Prometheus esté recopilando métricas.

## Respaldo SQL Server

Ejecuta con una cuenta autorizada y almacena el archivo fuera del host de la aplicación:

```sql
BACKUP DATABASE EventixDb
TO DISK = N'/var/opt/mssql/backup/EventixDb_full.bak'
WITH CHECKSUM, COMPRESSION, INIT, STATS = 10;

RESTORE VERIFYONLY
FROM DISK = N'/var/opt/mssql/backup/EventixDb_full.bak'
WITH CHECKSUM;
```

Programa un respaldo completo diario y respaldos de log según el RPO cuando producción use recuperación `FULL`.

## Recuperación

1. Detén la aplicación para evitar escrituras.
2. Restaura el último respaldo verificado en una instancia aislada.
3. Ejecuta `RESTORE VERIFYONLY` y validaciones funcionales.
4. Configura `eventix_migrator`, inicia Eventix y deja que Flyway aplique versiones posteriores.
5. Confirma readiness, login, reportes y escaneo antes de abrir tráfico.

## Rotación de claves de boletas

1. Conserva la clave pública anterior en `TICKETING_VERIFICATION_PUBLIC_KEYS`.
2. Define un `TICKETING_SIGNING_KEY_ID` nuevo y su par Ed25519.
3. Reinicia la aplicación.
4. Comprueba que una boleta antigua y una nueva validen correctamente.
5. Retira una clave histórica solo cuando todas sus boletas hayan vencido.

## Incidentes

- Usa `X-Correlation-ID` para cruzar respuesta, logs y `audit_logs`.
- Ante intentos de fuerza bruta, revisa `AUTHENTICATION_FAILURE` y ajusta temporalmente el límite de login.
- Ante sobreventa o duplicados, conserva la evidencia y no modifiques registros directamente.
- Rota inmediatamente cualquier secreto expuesto y revisa el historial de auditoría.

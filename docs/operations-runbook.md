# Operación, respaldo y recuperación

## Despliegue

1. Provisiona SQL Server 2022 y almacenamiento persistente cifrado.
2. Crea secretos distintos para `sa`, `eventix_migrator` y `eventix_app`.
3. Define `APP_PROFILE=prod`, `APP_BASE_URL=https://...` y las claves Ed25519.
4. Genera y conserva `EVENTIX_ELIGIBILITY_HMAC_SECRET` en el almacén de
   secretos.
5. Construye una imagen inmutable desde un commit cuyo CI esté verde.
6. Ejecuta Flyway con `eventix_migrator`; la aplicación usa `eventix_app`.
7. Espera `/actuator/health/readiness` antes de recibir tráfico.
8. Conserva la versión anterior para rollback; nunca reviertas una migración
   aplicada editando archivos Flyway históricos.

Eventix usa apagado graceful durante 30 segundos. El orquestador debe conceder
al menos 35 segundos antes de terminar el contenedor.

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

## Rotación de credenciales de base de datos

1. Actualiza `EVENTIX_DB_PASSWORD` y `EVENTIX_MIGRATOR_PASSWORD` en el
   almacén de secretos; conserva valores distintos para cada login.
2. Recrea el configurador y la aplicación sin eliminar el volumen:

   ```bash
   docker compose up --detach --force-recreate --wait --wait-timeout 300
   ```

3. Confirma que `sqlserver` y `app` estén saludables y que
   `sqlserver-configure` termine con código `0`.
4. Revoca las credenciales anteriores en cualquier almacén externo.

El configurador compara las claves actuales y ejecuta `ALTER LOGIN` solo
cuando cambiaron. La rotación de `sa` es una operación administrativa separada:
actualiza primero el login dentro de SQL Server y después
`MSSQL_SA_PASSWORD`. Nunca elimines el volumen para rotar credenciales.

## Secreto HMAC de elegibilidad

Genera una clave inicial con `openssl rand -hex 32` y guárdala fuera del
repositorio. La misma clave debe utilizarse en cada réplica y después de cada
reinicio o despliegue.

No la incluyas en respaldos de aplicación sin cifrar ni la registres en logs.
Una rotación cambia todas las huellas HMAC: antes de rotarla, conserva la fuente
autorizada de cada padrón, planifica una reimportación completa y valida las
membresías afectadas. No elimines la clave anterior hasta terminar y auditar la
transición.

## Liquidaciones a organizadores

Eventix no ejecuta todavía el desembolso. Una liquidación en `PROCESSING`
representa un pago externo en curso. Márcala como `PAID` únicamente después de
confirmar la transferencia y registra la referencia bancaria o del proveedor.
Ante un fallo externo, usa `FAILED`; no marques pagada una operación pendiente
de conciliación.

## SMTP y recordatorios

Configura `EVENTIX_EMAIL_ENABLED=true`, remitente, host, puerto y credenciales.
Activa TLS/autenticación según el proveedor. Los timeouts predeterminados son de
cinco segundos para que una indisponibilidad temporal no bloquee indefinidamente
los workers.

Activa `EVENTIX_REMINDERS_ENABLED=true` solo después de probar el envío. El
scheduler registra una entrega única por evento/correo, respeta preferencias y
reintenta fallos hasta `EVENTIX_REMINDER_MAX_ATTEMPTS`. No registra destinatarios,
tokens ni contenido del correo en logs.

La compra ya está confirmada cuando se envía el correo. Si SMTP falla, la venta
y sus boletas permanecen válidas y el fallo se diagnostica por correlation ID.

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

## Diagnóstico básico

- `GET /actuator/health/liveness`: proceso vivo.
- `GET /actuator/health/readiness`: dependencias listas para tráfico.
- `GET /actuator/prometheus`: métricas, restringidas a administradores.
- `docker compose ps`: estado y healthchecks.
- `docker compose logs app sqlserver`: logs sin incluir `.env`.
- `X-Correlation-ID`: úsalo para relacionar respuesta, JSON logs y auditoría.

No pegues volcados completos que contengan tokens de recuperación, credenciales,
datos de pago ni secretos de wallet en tickets de soporte.

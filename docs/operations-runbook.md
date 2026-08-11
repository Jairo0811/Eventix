# Operación, respaldo y recuperación

## Despliegue

1. Provisiona SQL Server 2022 y almacenamiento persistente cifrado.
2. Crea secretos distintos para `sa`, `eventix_migrator` y `eventix_app`.
3. Define `APP_PROFILE=prod`, `APP_BASE_URL=https://...` y las claves Ed25519.
4. Construye una imagen inmutable desde un commit cuyo CI esté verde.
5. Ejecuta Flyway con `eventix_migrator`; la aplicación usa `eventix_app`.
6. Espera `/actuator/health/readiness` antes de recibir tráfico.
7. Conserva la versión anterior para rollback; nunca reviertas una migración
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

# Manual técnico

## Toolchain

- Java 21.
- Maven 3.9 o superior.
- SQL Server 2022.
- Docker y Docker Compose para la plataforma completa y Testcontainers.

## Construcción

```bash
mvn clean verify
```

En CI se activa el perfil `ci`, que ejecuta Enforcer, Checkstyle, JaCoCo y la suite completa sobre SQL Server real.

## Configuración mínima

| Variable | Uso |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexión de ejecución. |
| `FLYWAY_DB_URL`, `FLYWAY_DB_USERNAME`, `FLYWAY_DB_PASSWORD` | Conexión exclusiva para migraciones. |
| `EVENTIX_BOOTSTRAP_ADMIN_PASSWORD` | Sustituye la clave inicial al arrancar producción por primera vez. |
| `EVENTIX_ELIGIBILITY_HMAC_SECRET` | Clave estable de al menos 32 bytes para proteger identificadores de elegibilidad. |
| `TICKETING_SIGNING_KEY_ID` | Identificador de la clave activa. |
| `TICKETING_SIGNING_PRIVATE_KEY` | Clave privada Ed25519 PKCS#8 en Base64. |
| `TICKETING_SIGNING_PUBLIC_KEY` | Clave pública Ed25519 X.509 en Base64. |
| `TICKETING_VERIFICATION_PUBLIC_KEYS` | Claves históricas `id=base64,id2=base64`. |

Genera el secreto de elegibilidad con `openssl rand -hex 32`. Docker Compose lo
exige explícitamente. No lo cambies sin reimportar los padrones desde su fuente
autorizada, ya que una nueva clave produce huellas de búsqueda diferentes.

Consulta `docs/phase-5-digital-ticketing.md` para Google Wallet y Apple Wallet.

## Perfiles

- `dev`: interfaz sin caché y posibilidad explícita de clave efímera.
- `test`: SQL Server Testcontainers, migraciones completas y rollback de pruebas.
- `prod`: cookies seguras, logs JSON Logstash, claves persistentes y bootstrap seguro obligatorios.

## Migraciones

Nunca edites una migración aplicada. Crea una versión nueva. Las migraciones
actuales llegan hasta V23; Hibernate mantiene `ddl-auto=validate` para detectar
divergencias.

## Pagos y liquidaciones

Los proveedores no-wallet continúan usando la estrategia simulada. El adaptador
AZUL existente se limita a Apple Pay y Google Pay cuando se suministran las
credenciales correspondientes. Una selección denominada PayPal, Stripe,
CardNET, Qik, AZUL o transferencia no debe interpretarse como integración
productiva hasta v1.4.0.

Las liquidaciones calculan y preservan comisión, reembolsos y neto del
organizador, pero el desembolso se realiza fuera de Eventix. El administrador
solo debe marcar una liquidación como pagada después de verificar la
transferencia externa y registrar su referencia.

## Observabilidad

- `GET /actuator/health/liveness`: proceso vivo.
- `GET /actuator/health/readiness`: aplicación y dependencias listas.
- `GET /actuator/prometheus`: métricas, limitado a administradores por Spring Security.
- `X-Correlation-ID`: presente en cada respuesta y en MDC.

## Pruebas

```bash
mvn clean test
mvn clean verify
```

Las pruebas de integración exigen Docker. No se usa H2: Flyway, Hibernate y las consultas de reportes se validan contra SQL Server 2022.

## Convenciones

- Controller → Service → Repository.
- DTO en las fronteras web.
- `@PreAuthorize` en operaciones sensibles, además de las reglas de ruta.
- Eventos de dominio para desacoplar ventas, boletas y Wallet.
- No registrar secretos, QR completos ni datos de tarjeta.

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
| `TICKETING_SIGNING_KEY_ID` | Identificador de la clave activa. |
| `TICKETING_SIGNING_PRIVATE_KEY` | Clave privada Ed25519 PKCS#8 en Base64. |
| `TICKETING_SIGNING_PUBLIC_KEY` | Clave pública Ed25519 X.509 en Base64. |
| `TICKETING_VERIFICATION_PUBLIC_KEYS` | Claves históricas `id=base64,id2=base64`. |

Consulta `docs/phase-5-digital-ticketing.md` para Google Wallet y Apple Wallet.

## Perfiles

- `dev`: interfaz sin caché y posibilidad explícita de clave efímera.
- `test`: SQL Server Testcontainers, migraciones completas y rollback de pruebas.
- `prod`: cookies seguras, logs JSON Logstash, claves persistentes y bootstrap seguro obligatorios.

## Migraciones

Nunca edites una migración aplicada. Crea una versión nueva. V7 agrega `audit_logs`; Hibernate mantiene `ddl-auto=validate` para detectar divergencias.

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

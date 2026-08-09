# Fase 6 — Reportes, auditoría y producción

## Entregado

- Dashboard ejecutivo con ingresos, ventas, entradas, disponibilidad, organizadores, asistencia y conversión.
- Reportes por evento, categoría, organizador y período.
- Rankings e ingresos mensuales.
- Exportaciones CSV, XLSX y PDF.
- Auditoría central de login, logout, fallos, CRUD, ventas, reservaciones, estados, escaneos, exportaciones y errores.
- Rate limiting, CSP, HSTS, Permissions Policy y correlación.
- Bootstrap seguro, usuarios SQL separados y rotación de claves Ed25519.
- Actuator, probes, Prometheus y logs JSON en producción.
- JaCoCo, Checkstyle, Dependency Review, Trivy, SBOM y artefactos de CI.
- Manual técnico, manual del usuario, arquitectura, ER, casos de uso y runbook.

## Criterios de aceptación

- Flyway V1–V7 y `ddl-auto=validate` aprueban sobre SQL Server 2022.
- La suite completa y el control de cobertura quedan en verde.
- Docker Compose alcanza readiness y publica `/login` con CSP y correlación.
- El contenedor no contiene vulnerabilidades HIGH/CRITICAL corregibles según Trivy.
- El PR se fusiona sin modificar migraciones anteriores.

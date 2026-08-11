# Changelog

Todos los cambios relevantes de Eventix se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y
el proyecto utiliza [Versionado Semántico](https://semver.org/lang/es/).

## [1.1.0] - 2026-08-11

### Añadido

- Recuperación de contraseña mediante tokens seguros, de un solo uso,
  revocables y con expiración.
- Perfil autenticado con edición de datos permitidos, cambio seguro de
  contraseña y preferencias de notificación.
- Cupones porcentuales y de monto fijo con vigencia, límites, alcance por
  evento y persistencia del snapshot financiero aplicado a cada venta.
- Liquidaciones persistentes a organizadores con prevención de doble
  liquidación, trazabilidad de ventas y reembolsos y estados transaccionales.
- Centro comercial privado para organizadores con métricas de ocupación,
  ventas, comisión, neto y liquidaciones.
- Notificaciones transaccionales posteriores al commit, entrega de boletas por
  correo y recordatorios persistentes con deduplicación y reintentos.
- Runbook operativo para despliegue, SMTP, respaldo, restauración, rotación de
  claves y diagnóstico mediante Correlation ID.

### Cambiado

- Se reforzó la autorización server-side por rol y propiedad del recurso.
- Docker Compose exige secretos explícitos para bootstrap y claves persistentes
  cuando la aplicación se ejecuta en producción.
- Dependency Review bloquea cambios con vulnerabilidades altas y el pipeline
  conserva verificaciones de Maven, SQL Server, Docker, headers, Trivy y SBOM.
- La versión del artefacto Maven se actualizó a `1.1.0`.

### Seguridad

- Las solicitudes de recuperación no revelan si una cuenta existe.
- Los descuentos se calculan únicamente en backend y nunca producen totales
  negativos.
- Las operaciones financieras críticas usan transacciones, bloqueos e índices
  únicos para preservar integridad e idempotencia.
- Los fallos temporales de SMTP no revierten ventas ya confirmadas.

## [1.0.0] - 2026-08-08

### Añadido

- Primera versión etiquetada de la reconstrucción profesional de Eventix.
- Gestión de eventos, categorías, reservaciones, ventas, pagos, boletas
  digitales, control de acceso, reportes, auditoría y observabilidad.
- Integraciones preparadas para AZUL, Apple Pay, Google Pay, Apple Wallet y
  Google Wallet mediante configuración externa segura.

[1.1.0]: https://github.com/Jairo0811/Eventix/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/Jairo0811/Eventix/releases/tag/v1.0.0

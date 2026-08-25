# Changelog

Todos los cambios relevantes de Eventix se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y
el proyecto utiliza [Versionado Semántico](https://semver.org/lang/es/).

## [1.3.1] - 2026-08-24

### Añadido

- Pruebas directas del checkout del comprador y contratos de configuración de
  despliegue.
- Licencia MIT del proyecto.
- JAR y SBOM verificados como activos de cada nueva release estable.

### Cambiado

- La versión expuesta por Actuator se obtiene de la versión Maven.
- La publicación de releases espera un `Eventix CI` exitoso sobre `main` y es
  idempotente cuando la versión ya fue publicada.
- README, manuales, modelo ER y casos de uso se sincronizaron con v1.3.x.

### Seguridad

- Docker Compose exige y transfiere `EVENTIX_ELIGIBILITY_HMAC_SECRET`.
- CI genera un secreto HMAC efímero de 256 bits para las pruebas de contenedor.
- La protección HMAC rechaza secretos menores de 32 bytes.

## [1.3.0] - 2026-08-23

### Añadido

- Checkout de autoservicio, historial postcompra y portal “Mis entradas”.
- Eligibility & Benefits con padrón escolar, HMAC de cédula, grupos genéricos,
  membresías, relaciones familiares y administración por evento.
- Descuentos porcentuales/fijos, entrada gratuita, límites de compra y tipos de
  entrada exclusivos aplicados en backend.
- Reembolsos completos y parciales por boleta con ledger de liquidaciones.
- Dashboard comercial del organizador y centro interno de notificaciones.

### Seguridad

- Revalidación de elegibilidad dentro del checkout transaccional.
- Bloqueos pesimistas e idempotencia local en operaciones financieras
  sensibles.
- Migraciones incrementales V16 a V23 verificadas sobre SQL Server 2022.

## [1.1.2] - 2026-08-12

### Corregido

- Docker Compose espera que una base persistente termine su recuperación antes
  de ejecutar la configuración.
- El configurador sincroniza de forma idempotente las contraseñas de
  `eventix_app` y `eventix_migrator` cuando cambian los secretos.
- Las credenciales con comillas simples se escapan antes de enviarse a
  `sqlcmd`.

### Pruebas

- CI recrea SQL Server sobre el volumen existente, rota ambas credenciales y
  comprueba los logins y la readiness de la aplicación.

## [1.1.1] - 2026-08-11

### Cambiado

- Publicación de mantenimiento sin cambios funcionales adicionales respecto a
  `1.1.0`.

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

[1.3.1]: https://github.com/Jairo0811/Eventix/compare/v1.3.0...v1.3.1
[1.3.0]: https://github.com/Jairo0811/Eventix/compare/v1.1.2...v1.3.0
[1.1.2]: https://github.com/Jairo0811/Eventix/compare/v1.1.1...v1.1.2
[1.1.1]: https://github.com/Jairo0811/Eventix/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/Jairo0811/Eventix/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/Jairo0811/Eventix/releases/tag/v1.0.0

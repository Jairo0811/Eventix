# Changelog

Todos los cambios relevantes de Eventix se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y
el proyecto utiliza [Versionado Semántico](https://semver.org/lang/es/).

## [1.4.0] - 2026-11-22

### Fases 5–6 — Conciliación y preproducción (sin despliegue)

- Flyway V36: ledger independiente de comparaciones entre eventos bancarios
  **previamente verificados** y transacciones de Eventix; un registro por
  proveedor e ID externo, sin almacenar datos de tarjetas.
- `PaymentReconciliationService`: coincidencia por referencia externa,
  importe exacto, moneda, proveedor y operación. Las discrepancias quedan
  `REVIEW_REQUIRED` y **nunca aprueban automáticamente ventas**.
- Tests de importe, moneda, evento repetido, hash incompatible y recibo ausente.
- `render.staging.yaml` preparado con SQL Server **aislado**, perfil `prod`,
  checkout cerrado y despliegue manual; no se creó infraestructura.
- Inspección SQL de solo lectura y guía de backup/restore a staging.
- Sigue pendiente la autenticación bancaria oficial y la conciliación de
  extractos productivos.

### Preparación v1.4 — pagos e infraestructura (sin habilitar)

- Bloqueada la pasarela simulada bajo el perfil `prod`; checkout de comprador
  y cobros de operador desactivados por defecto, con guardia en backend y aviso UI.
- Nuevo validador de arranque productivo: HTTPS, TLS SQL Server, roles distintos,
  HMAC estable, Ed25519 persistente y credenciales mínimas.
- Esquema Flyway V35 con índice único para notificaciones bancarias
  **previamente verificadas**, sin endpoint de webhooks ni activación comercial.
- Blueprint Render productivo explícitamente cerrado a operaciones reales
  hasta aprobación; pruebas y guía de preparación añadidas.


> Trabajo de la rama principal posterior a la release estable v1.3.4.
> No implica que exista una nueva release, despliegue público verificado ni
> habilitación comercial de pagos.

### Añadido

- Catálogo público y portada del comprador, navegación de `Mi Eventix` y
  presentación de boletas digitales 2.0 (PR #93–#97).
- Beneficio de egresados en checkout, Google Pay condicionado por
  configuración y mapa visual de asientos (PR #80, #82 y #90).
- Mejoras responsive y pruebas E2E de compra **simulada** (PR #103).
- Catálogo ficticio de solo lectura bajo perfil `render`, con tres eventos
  y datos efímeros en H2 (PR #104).
- Verificación automatizada opcional de las demos públicas en Render y Netlify,
  con comprobaciones de contenido, filtros, salud y 20 capturas en cinco
  resoluciones. La ejecución en hosting real queda pendiente.
- Tráiler oficial en `media/eventix-trailer/`: proyecto Remotion independiente
  que produce las tres piezas promocionales con capturas reales de la demo
  pública, recreaciones rotuladas de las vistas autenticadas y música original
  generada por código. No añade dependencias al build de Maven (PR #109).
- Evidencia responsive en `docs/evidence/responsive/`: portada pública en 360,
  390, 768, 1024 y 1440 px, con 0 px de desbordamiento horizontal medidos
  durante la captura (PR #109).

### Corregido

- Conservación de reembolsos rechazados y cotización basada en asientos
  retenidos (PR #89 y #102).
- DDL de cupones compatible con H2 del perfil demo (PR #105).
- Matriz de aceptación separada entre evidencias de CI y resultados todavía
  no comprobados de los despliegues públicos.
- Documentación de accesibilidad actualizada al tema visual híbrido vigente.

### Pendiente antes del cierre público

- Confirmar despliegues y versión `live` en Render/Netlify.
- Revisar manualmente capturas y accesibilidad con teclado, zoom y lector.
- Configuración persistente, proveedor de identidad autorizado y
  gateway bancario productivo, todos fuera de la demo gratuita.

## [1.3.4] - 2026-08-29

### Añadido

- Categoría activa `Promoción escolar` para clasificar graduaciones, reencuentros
  y actividades de una promoción o generación escolar.
- Migración Flyway V26 para incorporar esa categoría de forma idempotente.

### Cambiado

- El Home elimina las referencias residuales al antiguo selector de temas y
  conserva una única identidad visual híbrida.
- Las cuatro tarjetas de “Plataforma integral” adoptan superficies oscuras/teal
  con acentos verdes para mantener coherencia con la identidad visual.
- La prueba de categorías valida explícitamente las cinco categorías actuales
  por nombre, incluyendo `Promoción escolar`.

## [1.3.3] - 2026-08-29

### Añadido

- Contrato `CitizenIdentityProvider` para resolver de forma desacoplada una
  cédula hacia el nombre devuelto por una fuente de identidad autorizada.
- Proveedor configurable de identidades ficticias exclusivamente para el perfil
  `dev` mediante `EVENTIX_IDENTITY_DEV_RECORDS`.
- Estados de auditoría diferenciados cuando la identidad no existe o el
  proveedor no está disponible.
- Migración Flyway V25 para adaptar padrones y auditoría al nuevo modelo.

### Cambiado

- El padrón escolar deja de contener cédulas y utiliza el formato CSV
  `full_name,student_code,source_reference`.
- La elegibilidad escolar se decide comparando el nombre asociado a la cédula
  por el proveedor de identidad contra los nombres del padrón; el nombre del
  perfil Eventix ya no participa en esa decisión.
- Una coincidencia única verifica automáticamente; múltiples coincidencias
  exactas pasan a revisión manual.
- Eventix adopta una única identidad visual híbrida con navegación oscura y
  área de trabajo clara; se eliminan los modos claro/oscuro/sistema y su
  preferencia persistente.

### Seguridad

- La cédula completa no forma parte del padrón escolar ni de las vistas de
  verificaciones.
- Los intentos continúan registrando una huella HMAC-SHA-256 y los últimos
  cuatro dígitos para trazabilidad sin persistir la cédula completa.
- En perfiles distintos de `dev`, la verificación falla de forma cerrada hasta
  que exista un proveedor de identidad autorizado.

## [1.3.2] - 2026-08-29

### Añadido

- Administración web de instituciones y promociones escolares.
- Importación de padrones CSV desde la aplicación, historial de importaciones y
  plantilla descargable con el formato esperado.
- Flujo de autoservicio para que usuarios `USER` verifiquen su pertenencia a una
  promoción escolar contra un padrón autorizado.
- Pantalla administrativa para aprobar, rechazar o revocar verificaciones que
  requieren revisión manual.
- Vínculo persistente entre promociones escolares y grupos de elegibilidad
  `PROMOTION_MEMBER` mediante la migración Flyway V24.

### Cambiado

- Una verificación escolar `VERIFIED` sincroniza automáticamente la membresía de
  elegibilidad necesaria para que checkout aplique acceso, descuentos, entrada
  gratuita, límites y tipos de entrada exclusivos configurados para el grupo.
- Al crear o reactivar un grupo vinculado a una promoción se incorporan las
  verificaciones escolares ya aprobadas para evitar inconsistencias históricas.
- Los grupos de elegibilidad inactivos ya no conceden acceso ni beneficios.

### Seguridad

- La importación de padrones escolares exige rol `ADMINISTRATOR` tanto en Spring
  Security como en la capa de servicio.
- Rechazar o revocar una verificación escolar revoca las membresías derivadas de
  esa promoción.
- La UI continúa mostrando únicamente los últimos cuatro dígitos de la cédula;
  la búsqueda utiliza HMAC-SHA-256 y no persiste la cédula completa.

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

[1.3.4]: https://github.com/Jairo0811/Eventix/compare/v1.3.3...v1.3.4
[1.3.3]: https://github.com/Jairo0811/Eventix/compare/v1.3.2...v1.3.3
[1.3.2]: https://github.com/Jairo0811/Eventix/compare/v1.3.1...v1.3.2
[1.3.1]: https://github.com/Jairo0811/Eventix/compare/v1.3.0...v1.3.1
[1.3.0]: https://github.com/Jairo0811/Eventix/compare/v1.1.2...v1.3.0
[1.1.2]: https://github.com/Jairo0811/Eventix/compare/v1.1.1...v1.1.2
[1.1.1]: https://github.com/Jairo0811/Eventix/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/Jairo0811/Eventix/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/Jairo0811/Eventix/releases/tag/v1.0.0

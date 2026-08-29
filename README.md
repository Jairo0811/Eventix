<div align="center">

<p align="center">
  <img src="docs/images/eventix-logo.png" alt="Logo de Eventix" width="720" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/ITLA-2017--C2-0057B8?style=for-the-badge" alt="ITLA 2017-C2">
</p>

**Plataforma web modular para gestión integral de eventos, reservaciones, ventas, pagos, ticketing digital, elegibilidad, beneficios y control de acceso.**

[![Estado](https://img.shields.io/badge/Estado-v1.3.2%20estable-2563EB?style=for-the-badge)](https://github.com/Jairo0811/Eventix/releases/tag/v1.3.2)
[![Eventix CI](https://github.com/Jairo0811/Eventix/actions/workflows/ci.yml/badge.svg)](https://github.com/Jairo0811/Eventix/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![SQL Server](https://img.shields.io/badge/SQL_Server_2022-CC2927?style=for-the-badge&logo=microsoftsqlserver&logoColor=white)](https://www.microsoft.com/sql-server)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Bootstrap](https://img.shields.io/badge/Bootstrap_5-7952B3?style=for-the-badge&logo=bootstrap&logoColor=white)](https://getbootstrap.com/)

> **Estado actual:** **Eventix v1.3.2** es la release funcional estable. Fue validada por CI con Maven, SQL Server, Docker Compose, readiness, controles de seguridad, Trivy y SBOM. Los pagos comerciales reales y payouts automáticos permanecen planificados para v1.4.0.
>
> **Descargas verificadas:** [release v1.3.2](https://github.com/Jairo0811/Eventix/releases/tag/v1.3.2) · [eventix.jar](https://github.com/Jairo0811/Eventix/releases/download/v1.3.2/eventix.jar) · [SBOM SPDX](https://github.com/Jairo0811/Eventix/releases/download/v1.3.2/eventix-sbom.spdx.json)

</div>

---

## 🧭 Continuidad académica

**Eventix** nació como proyecto final de **Programación II (SOF-004)** durante el período **2017-C2** en el Instituto Tecnológico de Las Américas (**ITLA**), bajo la docencia del profesor **Raydelto Hernández Perera**.

Forma parte de una secuencia académica de tres proyectos desarrollados posteriormente con el mismo profesor. La relación entre ellos es formativa y cronológica; no constituyen dependencias técnicas ni versiones de una misma aplicación.

| Orden | Código | Asignatura | Proyecto | Período | Enfoque académico |
|---:|---|---|---|---|---|
| 1 | SOF-004 | Programación II | **Eventix** | 2017-C2 | POO, lógica de negocio y aplicación completa |
| 2 | SOF-012 | Estructuras de Datos | [**Aerolinea**](https://github.com/Jairo0811/Aerolinea) | 2018-C1 | Estructuras de datos, relaciones y rutas |
| 3 | SOF-011 | Programación WEB | [**ITLA Crush**](https://github.com/Jairo0811/ITLAcrushReact) | 2018-C2 | Desarrollo web, JavaScript, React y Firebase |

La versión moderna de Eventix conserva esa identidad académica, pero ha sido reconstruida con criterios de arquitectura, seguridad, accesibilidad, pruebas, observabilidad y despliegue propios de una aplicación profesional.

---

## 📌 Descripción

Eventix administra el ciclo operativo y comercial de un evento desde su publicación hasta el control de acceso.

Incluye:

- descubrimiento público y gestión de eventos;
- reservaciones y control de cupos;
- ventas, checkout, cupones y reembolsos;
- boletas digitales PDF/QR;
- control de acceso y auditoría;
- liquidaciones y métricas para organizadores;
- notificaciones internas y transaccionales;
- Eligibility & Benefits para audiencias controladas;
- promociones escolares con padrón autorizado y verificación de identidad.

La solución utiliza un **monolito modular por dominio**, con reglas críticas ejecutadas en backend y persistencia versionada mediante Flyway.

---

## 🏗️ Arquitectura

```text
src/main/java/com/jairomatias/eventix/
├── access/             # Control de acceso y escaneo
├── category/           # Categorías
├── checkout/           # Checkout del comprador
├── commerce/           # Casos comerciales compartidos
├── eligibility/        # Elegibilidad, promociones y beneficios
├── event/              # Eventos
├── notification/       # Notificaciones
├── payment/            # Contratos y adaptadores de pago
├── promotion/          # Cupones
├── reporting/          # Reportes y analítica
├── reservation/        # Reservaciones
├── sale/               # Ventas y tipos de entrada
├── settlement/         # Liquidaciones
├── ticket/             # Ticketing, PDF, QR y wallets
├── user/               # Usuarios y perfiles
└── shared/             # Infraestructura transversal
```

### Principios

- Clean Code, SOLID, DRY y KISS;
- separación de responsabilidades;
- autorización por rol y propiedad;
- transacciones para operaciones críticas;
- migraciones incrementales;
- pruebas automatizadas e integración continua;
- secretos externos al repositorio.

---

## 🆕 v1.3.2 — Promociones escolares de extremo a extremo

v1.3.2 completa la experiencia escolar que había quedado disponible principalmente a nivel de dominio/backend en v1.3.0.

### 🏫 Administración escolar

- instituciones educativas y promociones;
- activación/desactivación controlada;
- importación de padrón CSV;
- checksum y prevención de reimportaciones duplicadas;
- plantilla CSV descargable;
- historial de importaciones;
- consulta del padrón sin exponer la cédula completa;
- revisión manual administrativa con aprobación, rechazo y revocación justificadas.

El CSV utiliza el encabezado exacto:

```csv
full_name,student_code,national_id,source_reference
```

### 🪪 Verificación del usuario

Los usuarios `USER` disponen del flujo **Mi promoción escolar**. La verificación compara la identidad contra el padrón autorizado y usa **HMAC-SHA-256** para generar la clave de búsqueda.

- coincidencia de cédula + nombre → `VERIFIED`;
- cédula encontrada con discrepancia de nombre → `MANUAL_REVIEW`;
- identidad no encontrada → sin elegibilidad;
- la cédula completa no se persiste como clave de consulta ni debe aparecer en logs o respuestas HTTP.

```text
EVENTIX_ELIGIBILITY_HMAC_SECRET=<secreto-fuerte-y-estable>
```

### 🎫 Integración con Eligibility & Benefits

La verificación escolar ya está conectada directamente con el modelo genérico utilizado por el checkout:

```text
Padrón escolar
      ↓
Verificación de identidad
      ↓
VERIFIED
      ↓
eligibility_memberships
      ↓
Grupo PROMOTION_MEMBER
      ↓
Beneficios / acceso
      ↓
Checkout
```

Los grupos `PROMOTION_MEMBER` pueden asociarse a una promoción concreta. Una verificación aprobada sincroniza la membresía correspondiente; rechazo o revocación retiran las membresías derivadas.

Los beneficios soportados incluyen:

- descuentos porcentuales;
- descuentos fijos;
- entrada gratuita;
- límites de compra;
- tipos de entrada exclusivos;
- acceso a eventos `PRIVATE` o `CONTROLLED_ACCESS`.

Los grupos desactivados no conceden acceso ni beneficios.

---

## ✨ Funcionalidades principales

### 🔐 Seguridad y usuarios

- Spring Security;
- login por correo o usuario;
- BCrypt;
- recuperación y cambio obligatorio de contraseña temporal;
- CSRF;
- RBAC y autorización por propiedad;
- roles `ADMINISTRATOR`, `OPERATOR`, `ORGANIZER`, `ACCESS_STAFF` y `USER`;
- rate limiting;
- CSP, HSTS y Permissions Policy;
- Correlation ID y manejo diferenciado de errores.

### 📅 Eventos

- CRUD y categorías;
- borrador, publicado, cancelado y finalizado;
- eventos gratuitos o de pago;
- capacidad y disponibilidad;
- tipos de entrada;
- portadas y Google Maps;
- búsqueda, filtros y paginación;
- acceso `PUBLIC`, `PRIVATE` y `CONTROLLED_ACCESS`.

### 🎟️ Reservaciones, checkout y ventas

- reservaciones con expiración y liberación de cupos;
- bloqueo transaccional para reducir sobreventa;
- ventas con precio histórico;
- cupones porcentuales y fijos;
- beneficios monetarios de elegibilidad calculados en backend;
- reembolsos completos y parciales por boleta;
- comprobantes PDF.

### 📱 Ticketing y acceso

- emisión idempotente de entradas;
- PDF y QR individual;
- Ed25519 + SHA-256;
- revocación ante reembolso/cancelación;
- escáner web por cámara y entrada manual;
- control de primer ingreso, reingreso, duplicados e inválidos;
- auditoría de accesos.

### 💸 Liquidaciones y reporting

- comisión de Eventix y neto del organizador;
- estados de liquidación;
- prevención de doble liquidación;
- métricas de ventas, ingresos, ocupación y asistencia;
- centro interno de notificaciones.

### 💳 Pagos

El dominio de pagos usa contratos desacoplados y Strategy. En v1.3.2 **no existe todavía un gateway comercial productivo**; los proveedores comerciales reales, webhooks y payouts corresponden a v1.4.0.

---

## 🧰 Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Java 21 |
| Framework | Spring Boot 3.5 |
| Seguridad | Spring Security |
| Persistencia | Spring Data JPA / Hibernate |
| Base de datos | SQL Server 2022 |
| Migraciones | Flyway |
| Frontend | Thymeleaf + Bootstrap 5 + JavaScript |
| PDF | Apache PDFBox |
| QR | ZXing |
| Build | Maven |
| Testing | JUnit 5, Mockito, Spring Test, Testcontainers |
| Contenedores | Docker / Docker Compose |
| CI/CD | GitHub Actions |
| Seguridad CI | Dependency Review + Trivy |
| Supply Chain | SBOM SPDX |

---

## 🗄️ Base de datos

SQL Server 2022 es la base principal y el esquema se administra mediante Flyway:

```text
src/main/resources/db/migration/
```

La versión estable v1.3.2 llega hasta:

```text
V24__link_school_promotions_to_eligibility_groups.sql
```

V24 enlaza `eligibility_groups.school_promotion_id` con `school_promotions`, permitiendo que una promoción verificada participe directamente en las reglas de acceso y beneficios del checkout.

---

## ⚙️ Configuración local

### Requisitos

- Java 21
- Maven 3.9+
- Docker Desktop o SQL Server 2022
- Git

### Clonar y probar

```bash
git clone https://github.com/Jairo0811/Eventix.git
cd Eventix
mvn clean verify
```

### Docker Compose

Configura las variables del `.env` y ejecuta:

```bash
docker compose up --build
```

Para detener sin eliminar los datos:

```bash
docker compose down
```

Readiness:

```text
http://localhost:8080/actuator/health/readiness
```

Aplicación:

```text
http://localhost:8080
```

### 📱 Acceso desde móvil en la LAN

Con el perfil `dev`, abre desde un dispositivo conectado a la misma red:

```text
http://<IP-DE-LA-PC>:8080
```

Obtén la IPv4 con `ipconfig` y, si Windows lo solicita, habilita Java únicamente para redes privadas.

---

## 🔒 Variables sensibles

Los secretos no deben persistirse en Git.

```text
DB_USERNAME
DB_PASSWORD
FLYWAY_DB_USERNAME
FLYWAY_DB_PASSWORD
EVENTIX_ELIGIBILITY_HMAC_SECRET
GOOGLE_MAPS_EMBED_API_KEY
```

`EVENTIX_ELIGIBILITY_HMAC_SECRET` debe contener al menos 32 bytes y mantenerse estable. Rotarlo sin un procedimiento controlado invalida las claves HMAC usadas para consultar padrones previamente importados.

---

## 🧪 Calidad y CI

El pipeline principal verifica:

1. Java 21 y toolchain;
2. `mvn clean verify`;
3. pruebas y evidencia;
4. SQL Server + Docker Compose;
5. readiness, login y headers de seguridad;
6. reinicio persistente y rotación de credenciales;
7. Trivy;
8. SBOM;
9. Dependency Review en Pull Requests.

La publicación estable reutiliza los artefactos del CI exitoso y adjunta `eventix.jar` y `eventix-sbom.spdx.json` a la GitHub Release.

---

## ♿ Accesibilidad

Eventix adopta **NORTIC B2:2017 / WCAG 2.0 Nivel AA** como objetivo técnico interno. La interfaz incluye landmarks, navegación por teclado, atributos ARIA, foco visible, `prefers-reduced-motion`, `forced-colors` y protecciones para zoom elevado.

Consulta [`docs/accessibility-nortic-b2.md`](docs/accessibility-nortic-b2.md).

> La implementación técnica y las pruebas internas no equivalen a certificación oficial de OGTIC. El cierre manual con teclado, zoom, contraste y lector de pantalla continúa pendiente.

---

## 📈 Roadmap

### v1.3.2 — ✅ Estable

- [x] administración de instituciones/promociones;
- [x] importación e historial de padrones CSV;
- [x] verificación escolar de autoservicio;
- [x] revisión manual;
- [x] puente `school_promotions` → `eligibility_memberships`;
- [x] beneficios escolares aplicados por checkout;
- [x] Flyway V24;
- [x] release con JAR y SBOM verificados.

### v1.4.0 — Pagos productivos

- gateway comercial real;
- webhooks e idempotencia externa;
- reconciliación;
- reembolsos externos y disputas;
- payouts reales a organizadores;
- conciliación de liquidaciones.

### v1.5.0 — Acceso avanzado y escalabilidad

- early access operativo;
- inventario reservado transaccional;
- prioridad y colas de acceso;
- pruebas E2E de navegador;
- carga y concurrencia ampliadas.

---

## 📌 Estado del proyecto

| Área | Estado |
|---|---|
| Arquitectura modular | ✅ Implementada |
| Seguridad base | ✅ Implementada |
| Eventos y reservaciones | ✅ Implementados |
| Ventas y checkout | ✅ Implementados |
| Cupones | ✅ Implementados |
| Ticketing QR/PDF | ✅ Implementado |
| Control de acceso | ✅ Implementado |
| Liquidaciones y reporting | ✅ Implementados |
| Elegibilidad escolar E2E | ✅ Implementada |
| Elegibilidad genérica | ✅ Implementada |
| Relaciones familiares | ✅ Implementadas |
| Beneficios monetarios | ✅ Implementados |
| Centro de notificaciones | ✅ Implementado |
| Gateway real de producción | 🔵 v1.4.0 |
| Payouts automáticos | 🔵 v1.4.0 |
| QA manual de accesibilidad | 🟡 Pendiente de cierre |

---

## 🤝 Contribución

Flujo recomendado:

```text
main
  └── feature/<nombre>
        └── Pull Request
              └── CI verde
                    └── merge
```

No deben fusionarse cambios que rompan migraciones, pruebas, seguridad o compatibilidad con datos existentes.

---

## 👨‍💻 Autor

**Francis Jairo Matías Rosario**  
Proyecto académico original desarrollado en ITLA y posteriormente reconstruido como proyecto de portafolio profesional.

GitHub: [@Jairo0811](https://github.com/Jairo0811)

---

## 📄 Licencia

Consulta [`LICENSE`](LICENSE).
<div align="center">

<p align="center">
  <img src="docs/images/eventix-logo.png" alt="Logo de Eventix" width="720" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/ITLA-2017--C2-0057B8?style=for-the-badge" alt="ITLA 2017-C2">
</p>

**Plataforma web modular para gestión integral de eventos, reservaciones, ventas, ticketing digital, elegibilidad, promociones escolares, cuentas institucionales y control de acceso.**

[![Estado](https://img.shields.io/badge/Release-v1.3.4%20estable-2563EB?style=for-the-badge)](https://github.com/Jairo0811/Eventix/releases/tag/v1.3.4)
[![Eventix CI](https://github.com/Jairo0811/Eventix/actions/workflows/ci.yml/badge.svg)](https://github.com/Jairo0811/Eventix/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![SQL Server](https://img.shields.io/badge/SQL_Server_2022-CC2927?style=for-the-badge&logo=microsoftsqlserver&logoColor=white)](https://www.microsoft.com/sql-server)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Bootstrap](https://img.shields.io/badge/Bootstrap_5-7952B3?style=for-the-badge&logo=bootstrap&logoColor=white)](https://getbootstrap.com/)

> **Release estable publicada:** **Eventix v1.3.4**. Incluye la identidad visual híbrida unificada y la categoría `Promoción escolar`, además de las correcciones del flujo de identidad escolar incorporadas en v1.3.3.
>
> **Estado de `main`:** contiene trabajo posterior a v1.3.4, incluyendo cuentas organizacionales para centros educativos con membresías y permisos scoped. El `pom.xml` continúa en `1.3.4` hasta completar la alineación formal de la próxima release.
>
> **Descargas verificadas:** [release v1.3.4](https://github.com/Jairo0811/Eventix/releases/tag/v1.3.4) · [eventix.jar](https://github.com/Jairo0811/Eventix/releases/download/v1.3.4/eventix.jar) · [SBOM SPDX](https://github.com/Jairo0811/Eventix/releases/download/v1.3.4/eventix-sbom.spdx.json)

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

La versión moderna conserva esa identidad académica, pero ha sido reconstruida con arquitectura modular, seguridad, accesibilidad, pruebas automatizadas, observabilidad y despliegue reproducible.

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
- promociones escolares con padrón autorizado;
- verificación escolar por identidad oficial;
- cuentas organizacionales para centros educativos;
- membresías y permisos institucionales aislados por centro.

La solución utiliza un **monolito modular por dominio**, con reglas críticas ejecutadas en backend y esquema versionado mediante Flyway.

---

## 🏗️ Arquitectura

```text
src/main/java/com/jairomatias/eventix/
├── access/             # Control de acceso y escaneo
├── category/           # Categorías
├── checkout/           # Checkout del comprador
├── commerce/           # Casos comerciales compartidos
├── eligibility/        # Elegibilidad, promociones, padrones y beneficios
├── event/              # Eventos
├── institution/        # Cuentas y RBAC de centros educativos
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
- autorización por rol, propiedad y tenant institucional;
- transacciones para operaciones críticas;
- migraciones incrementales y compatibles con SQL Server;
- pruebas automatizadas e integración continua;
- secretos externos al repositorio;
- fail-closed en integraciones sensibles no configuradas.

---

## 🏫 Promociones escolares e identidad

### Padrón institucional sin cédulas

El padrón escolar **no contiene cédulas**. El CSV autorizado utiliza exactamente:

```csv
full_name,student_code,source_reference
```

Cada miembro puede incluir:

- nombre completo;
- código estudiantil opcional;
- referencia institucional opcional;
- estado activo/inactivo.

La cédula del usuario nunca se incorpora al padrón.

### Verificación por identidad oficial

El flujo vigente es:

```text
Cédula digitada por el usuario
        ↓
CitizenIdentityProvider
        ↓
Nombre legal/oficial
        ↓
Normalización determinística
        ↓
Padrón de la promoción seleccionada
        ↓
Resultado de elegibilidad
```

Reglas:

- 1 coincidencia exacta normalizada → `VERIFIED`;
- 0 coincidencias en el padrón → `NOT_FOUND`;
- 2 o más coincidencias idénticas → `MANUAL_REVIEW`;
- identidad inexistente en el proveedor → `IDENTITY_NOT_FOUND`;
- proveedor no disponible → `IDENTITY_PROVIDER_UNAVAILABLE`.

El nombre del perfil de Eventix **no decide la elegibilidad**.

La normalización usa mayúsculas, eliminación determinística de diacríticos y colapso de espacios. No se utiliza fuzzy matching para aprobar automáticamente.

### Privacidad

- la cédula completa no se almacena en el padrón;
- no debe registrarse en logs;
- los intentos de verificación pueden auditarse mediante HMAC y últimos cuatro dígitos;
- el nombre oficial no se duplica innecesariamente cuando el miembro del padrón ya representa la coincidencia verificada;
- en producción, la verificación debe utilizar un proveedor de identidad autorizado;
- si no existe un proveedor autorizado configurado, el flujo falla de forma cerrada.

En desarrollo puede configurarse un fixture local con identidades ficticias:

```text
EVENTIX_IDENTITY_DEV_RECORDS=00100000001=Ana Perez Gomez
```

Nunca deben usarse datos reales en ese fixture.

### Resultado visible

Las verificaciones del usuario muestran la promoción, el **nombre verificado**, el estado y el detalle de la decisión.

---

## 🏢 Cuentas de centros educativos

`main` incorpora cuentas organizacionales sin crear nuevos roles globales de plataforma.

Un centro educativo se modela como una organización y sus usuarios se relacionan mediante `InstitutionMembership`.

### Estados del centro

```text
PENDING_VERIFICATION
ACTIVE
REJECTED
SUSPENDED
```

Flujo básico:

```text
Usuario autenticado
      ↓
Registrar centro educativo
      ↓
SchoolInstitution = PENDING_VERIFICATION
      ↓
InstitutionMembership = OWNER
      ↓
Revisión administrativa Eventix
      ↓
ACTIVE
      ↓
Operaciones institucionales habilitadas
```

### Roles institucionales scoped

Estos permisos existen **dentro de una institución concreta** y no sustituyen los roles globales de Eventix:

| Rol institucional | Responsabilidad |
|---|---|
| `OWNER` | Propietario de la cuenta institucional y control del equipo |
| `ADMIN` | Administración operativa del centro |
| `EVENT_MANAGER` | Gestión institucional relacionada con promociones/eventos |
| `ROSTER_MANAGER` | Gestión e importación de padrones autorizados |
| `FINANCE` | Rol reservado para operaciones financieras institucionales |

Los cinco roles globales continúan siendo:

```text
ADMINISTRATOR
OPERATOR
ORGANIZER
ACCESS_STAFF
USER
```

### Aislamiento y autorización

La autorización institucional valida en backend:

```text
usuario pertenece a la institución
AND membership está activa
AND institución está operativa
AND rol scoped posee el permiso requerido
```

Un miembro de una institución no puede acceder ni modificar datos de otra institución manipulando la URL.

El `OWNER` no puede degradarse ni suspenderse desde la gestión ordinaria de miembros.

### Operaciones institucionales disponibles

- registro autenticado de centros;
- revisión administrativa de solicitudes;
- portal institucional;
- gestión de miembros usando cuentas Eventix existentes;
- creación y consulta de promociones del centro según permisos;
- importación de padrones por usuarios autorizados;
- protección cross-tenant.

---

## 🎨 Identidad visual

Eventix utiliza una única identidad visual híbrida:

- sidebar y navegación oscuras;
- contenido principal, formularios y tablas claros;
- acentos verdes/teal de Eventix;
- sin selector claro/oscuro/sistema;
- sin preferencias de tema almacenadas en localStorage.

La categoría activa **`Promoción escolar`** se incorpora mediante Flyway V26.

---

## ✨ Funcionalidades principales

### 🔐 Seguridad y usuarios

- Spring Security;
- login por correo o usuario;
- BCrypt;
- recuperación y cambio obligatorio de contraseña temporal;
- CSRF;
- RBAC global y RBAC institucional scoped;
- autorización por propiedad y tenant;
- rate limiting;
- CSP, HSTS y Permissions Policy;
- Correlation ID y manejo diferenciado de errores;
- logout POST con CSRF.

### 📅 Eventos

- CRUD y categorías;
- borrador, publicado, cancelado y finalizado;
- eventos gratuitos o de pago;
- capacidad y disponibilidad;
- tipos de entrada;
- portadas y Google Maps;
- búsqueda, filtros y paginación;
- acceso `PUBLIC`, `PRIVATE` y `CONTROLLED_ACCESS`;
- categoría `Promoción escolar`.

### 🎟️ Reservaciones, checkout y ventas

- reservaciones con expiración y liberación de cupos;
- bloqueo transaccional para reducir sobreventa;
- ventas con precio histórico;
- cupones porcentuales y fijos;
- beneficios monetarios de elegibilidad calculados en backend;
- reembolsos completos y parciales por boleta;
- comprobantes PDF.

### 🎓 Eligibility & Benefits

- grupos de elegibilidad;
- membresías verificadas;
- beneficios porcentuales y fijos;
- entrada gratuita;
- límites de compra;
- tipos de entrada exclusivos;
- acceso privado/controlado;
- integración con promociones escolares;
- revisión manual para casos ambiguos.

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

El dominio de pagos usa contratos desacoplados y Strategy. La release estable actual **no incluye todavía un gateway comercial productivo**. Los proveedores reales, webhooks, reconciliación y payouts corresponden al siguiente ciclo mayor de pagos.

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

## 🗄️ Base de datos y migraciones

SQL Server 2022 es la base principal y el esquema se administra mediante:

```text
src/main/resources/db/migration/
```

La rama `main` llega actualmente hasta **Flyway V27**.

Migraciones recientes relevantes:

| Migración | Propósito |
|---|---|
| V24 | Vincula promociones escolares con grupos de elegibilidad |
| V25 | Elimina campos de cédula de `promotion_members` |
| V26 | Agrega la categoría `Promoción escolar` |
| V27 | Agrega estado institucional y `institution_memberships` |

Las migraciones se aplican incrementalmente; no debe borrarse una base persistente para actualizar Eventix salvo que se trate explícitamente de un entorno descartable.

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
docker compose up --build -d
```

Estado de servicios:

```bash
docker compose ps
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

Entre las variables de configuración utilizadas por el proyecto se encuentran:

```text
DB_USERNAME
DB_PASSWORD
FLYWAY_DB_USERNAME
FLYWAY_DB_PASSWORD
EVENTIX_ELIGIBILITY_HMAC_SECRET
GOOGLE_MAPS_EMBED_API_KEY
```

En desarrollo, `EVENTIX_IDENTITY_DEV_RECORDS` puede contener únicamente identidades ficticias para probar el adaptador local.

Las credenciales de un futuro proveedor real de identidad o pagos deberán inyectarse mediante variables de entorno o un gestor de secretos; nunca mediante código fuente, fixtures o archivos versionados.

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

Los cambios de cuentas institucionales fueron validados con el mismo pipeline antes de integrarse a `main`, incluyendo pruebas de aislamiento cross-tenant y migración V27.

---

## ♿ Accesibilidad

Eventix adopta **NORTIC B2:2017 / WCAG 2.0 Nivel AA** como objetivo técnico interno. La interfaz incluye landmarks, navegación por teclado, atributos ARIA, foco visible, `prefers-reduced-motion`, `forced-colors` y protecciones para zoom elevado.

Consulta [`docs/accessibility-nortic-b2.md`](docs/accessibility-nortic-b2.md).

> La implementación técnica y las pruebas internas no equivalen a certificación oficial de OGTIC. El cierre manual con teclado, zoom, contraste y lector de pantalla continúa pendiente.

---

## 📈 Roadmap

### v1.3.4 — ✅ Release estable publicada

- [x] verificación escolar basada en nombre oficial y padrón sin cédulas;
- [x] Flyway V25;
- [x] identidad visual híbrida única;
- [x] categoría `Promoción escolar`;
- [x] Flyway V26;
- [x] JAR y SBOM publicados.

### Post-v1.3.4 en `main` — ✅ Integrado

- [x] nombre verificado visible en resultados escolares;
- [x] correcciones de logout/scripts en promociones escolares;
- [x] cuentas organizacionales para centros educativos;
- [x] `OWNER`, `ADMIN`, `EVENT_MANAGER`, `ROSTER_MANAGER`, `FINANCE` scoped por institución;
- [x] aprobación administrativa de centros;
- [x] gestión de equipo institucional;
- [x] aislamiento cross-tenant;
- [x] Flyway V27.

### Próximo bloque escolar

- descuento especial para egresados desde checkout;
- selección del padrón/promoción asociado al evento;
- verificación de cédula desde la compra;
- aplicación server-side del beneficio únicamente a egresados verificados.

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
| Verificación escolar por identidad | ✅ Implementada |
| Padrón escolar sin cédulas | ✅ Implementado |
| Nombre verificado visible | ✅ Implementado |
| Cuentas institucionales | ✅ Integradas en `main` |
| RBAC institucional scoped | ✅ Integrado |
| Aislamiento cross-tenant | ✅ Integrado |
| Beneficio de egresado en checkout | 🟡 Próximo bloque |
| Proveedor de identidad productivo | 🟡 Requiere fuente autorizada |
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

No deben fusionarse cambios que rompan migraciones, pruebas, seguridad, aislamiento de datos o compatibilidad con bases existentes.

---

## 👨‍💻 Autor

**Francis Jairo Matías Rosario**  
Proyecto académico original desarrollado en ITLA y posteriormente reconstruido como proyecto de portafolio profesional.

GitHub: [@Jairo0811](https://github.com/Jairo0811)

---

## 📄 Licencia

Consulta [`LICENSE`](LICENSE).

<div align="center">

<p align="center">
  <img src="docs/images/eventix-logo.png" alt="Logo de Eventix" width="720" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/ITLA-2017--C2-0057B8?style=for-the-badge" alt="ITLA 2017-C2">
</p>

**Plataforma web modular para gestión integral de eventos, reservaciones, ventas, pagos, ticketing digital, elegibilidad, beneficios y control de acceso.**

[![Estado](https://img.shields.io/badge/Estado-v1.3.0%20en%20evoluci%C3%B3n-2563EB?style=for-the-badge)](#-estado-del-proyecto)
[![Eventix CI](https://github.com/Jairo0811/Eventix/actions/workflows/ci.yml/badge.svg)](https://github.com/Jairo0811/Eventix/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![SQL Server](https://img.shields.io/badge/SQL_Server_2022-CC2927?style=for-the-badge&logo=microsoftsqlserver&logoColor=white)](https://www.microsoft.com/sql-server)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)](https://www.thymeleaf.org/)
[![Bootstrap](https://img.shields.io/badge/Bootstrap_5-7952B3?style=for-the-badge&logo=bootstrap&logoColor=white)](https://getbootstrap.com/)

> **Estado actual:** Eventix mantiene **v1.1.2** como última versión estable etiquetada, mientras `main` incorpora la evolución hacia **v1.3.0**. El núcleo comercial, la experiencia del organizador y el nuevo dominio genérico de **Eligibility & Benefits** continúan madurando de forma incremental con validación automática mediante CI.

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

La versión moderna de Eventix conserva esa identidad académica, pero ha sido reconstruida y ampliada con criterios de arquitectura, seguridad, accesibilidad, pruebas, observabilidad y despliegue propios de una aplicación profesional.

---

## 📌 Descripción

**Eventix** administra el ciclo operativo y comercial de un evento desde su publicación hasta el control de acceso.

Incluye capacidades para:

- descubrimiento público de eventos;
- gestión de eventos y categorías;
- reservaciones y control de cupos;
- venta y checkout;
- promociones y cupones;
- ticketing digital con PDF y QR;
- control de acceso;
- liquidaciones a organizadores;
- reportes y métricas;
- notificaciones;
- elegibilidad y beneficios para audiencias controladas;
- auditoría y trazabilidad de operaciones sensibles.

La aplicación utiliza un **monolito modular por dominio**, evitando concentrar toda la lógica en controladores o servicios globales.

---

## 🏗️ Arquitectura

```text
src/main/java/com/jairomatias/eventix/
├── access/             # Control de acceso y escaneo
├── category/           # Categorías de eventos
├── checkout/           # Checkout del comprador
├── commerce/           # Casos comerciales compartidos
├── eligibility/        # Elegibilidad, verificaciones y beneficios
├── event/              # Gestión de eventos
├── notification/       # Notificaciones
├── payment/            # Contratos y adaptadores de pago
├── promotion/          # Cupones y promociones
├── reporting/          # Reportes y analítica
├── reservation/        # Reservaciones y ocupación
├── sale/               # Ventas y tipos de entrada
├── settlement/         # Liquidaciones a organizadores
├── ticket/             # Ticketing, PDF, QR y wallets
├── user/               # Usuarios y perfiles
└── shared/             # Infraestructura transversal
```

### Principios aplicados

- Clean Code
- SOLID
- DRY
- KISS
- separación de responsabilidades;
- reglas de negocio ejecutadas en backend;
- autorización por rol y propiedad del recurso;
- transacciones para operaciones críticas;
- migraciones incrementales con Flyway;
- pruebas automatizadas e integración continua.

---

## 🆕 v1.3.0 — Evolución actual

### 🪪 Eligibility & Benefits

Eventix incorpora un dominio genérico para controlar acceso y beneficios sin acoplar el sistema exclusivamente a promociones escolares.

#### Grupos soportados por diseño

- miembros de promociones;
- egresados;
- familiares;
- personal institucional;
- VIP;
- comunidades privadas;
- grupos personalizados definidos por el organizador.

#### Seguridad de elegibilidad

- ningún beneficio se concede por simple autodeclaración;
- la elegibilidad debe proceder de una fuente autorizada o de una aprobación explícita;
- estados pendientes o en revisión manual no otorgan privilegios;
- las decisiones se revalidan en backend durante disponibilidad y compra;
- eventos admiten modos `PUBLIC`, `PRIVATE` y `CONTROLLED_ACCESS`;
- grupos, membresías y beneficios se modelan separadamente del mecanismo que demuestra la identidad.

### 🏫 Verificación para colegios y promociones

El primer proveedor concreto de evidencias de elegibilidad está orientado a promociones escolares.

- instituciones y promociones registradas;
- padrón autorizado de miembros;
- importación CSV con validación, checksum y deduplicación;
- verificación por identidad + nombre;
- revisión manual ante discrepancias;
- auditoría de intentos exitosos, fallidos y enviados a revisión;
- aprobación, rechazo y revocación con justificación.

#### Protección de cédula

Eventix evita persistir la cédula completa como dato de consulta.

- normalización únicamente durante el proceso de verificación;
- clave de búsqueda mediante **HMAC-SHA-256**;
- persistencia únicamente de la huella de búsqueda y últimos cuatro dígitos cuando corresponde;
- secreto HMAC suministrado mediante variable de entorno;
- la cédula completa no debe aparecer en logs, auditoría ni respuestas HTTP.

```text
EVENTIX_ELIGIBILITY_HMAC_SECRET=<secreto-fuerte>
```

### 🎫 Checkout con autorización de elegibilidad

El checkout ya integra la autorización de elegibilidad del lado del servidor.

- eventos públicos continúan disponibles para cualquier comprador autorizado;
- eventos privados o controlados requieren membresía verificada;
- tipos de entrada exclusivos pueden restringirse a determinados grupos;
- límites de compra pueden aplicarse por beneficio;
- la autorización se revalida nuevamente dentro de la operación de compra;
- ocultar una entrada en la UI nunca sustituye la validación de backend.

> Los cupones y la elegibilidad permanecen como dominios distintos: un cupón valida un código promocional; Eligibility valida identidad, pertenencia o relación.

### 👨‍👩‍👧 Familiares y relaciones

El modelo está preparado para relaciones familiares verificadas. La relación no debe considerarse válida solo porque una persona declare ser familiar de otra.

La evolución prevista requiere:

- solicitud de vínculo;
- patrocinador/miembro previamente verificado o revisión administrativa;
- aprobación o rechazo explícito;
- límites de familiares/invitados por miembro;
- trazabilidad de la decisión.

---

## ✨ Funcionalidades

### 🔐 Seguridad y usuarios

- autenticación con Spring Security;
- login mediante correo o nombre de usuario;
- contraseñas BCrypt;
- cambio obligatorio de contraseña temporal;
- recuperación de contraseña con tokens de un solo uso;
- protección CSRF;
- autorización por rutas, servicios, roles y propiedad;
- perfiles y preferencias de notificación;
- roles `ADMINISTRATOR`, `OPERATOR`, `ORGANIZER`, `ACCESS_STAFF` y `USER`;
- rate limiting;
- CSP, HSTS y Permissions Policy;
- identificadores de correlación;
- manejo diferenciado de errores HTTP `403`, `404`, `405` y `500`.

### 📅 Eventos

- CRUD de eventos y categorías;
- estados borrador, publicado, cancelado y finalizado;
- capacidad y reglas de disponibilidad;
- eventos gratuitos o de pago;
- tipos de entrada configurables;
- carga persistente de portadas;
- integración de ubicación con Google Maps;
- búsqueda, filtros y paginación;
- descubrimiento público;
- modos de acceso público, privado y controlado.

### 🎟️ Reservaciones y ventas

- reservaciones con historial permanente;
- estados pendiente, confirmada, cancelada y expirada;
- expiración de reservas y liberación de cupos;
- bloqueo transaccional para reducir riesgo de sobreventa;
- prevención de reservaciones activas duplicadas;
- ventas vinculadas a reservaciones;
- distribución de entradas con precio histórico;
- ventas pendientes, pagadas, reembolsadas y canceladas;
- comprobantes PDF;
- cupones por porcentaje o monto fijo;
- límites globales y por comprador;
- cálculo de descuentos exclusivamente en backend.

### 💰 Pagos

El módulo de pagos utiliza contratos desacoplados para evitar acoplar el dominio comercial a un proveedor específico.

- patrón Strategy para proveedores;
- registro de intentos y estados de pago;
- soporte conceptual para cargos y reembolsos;
- adaptadores preparados para evolución posterior;
- checkout actualmente validado sin asumir un gateway comercial real en producción.

> La integración productiva con proveedores de pago reales se reserva para la evolución de **v1.4.0**.

### 📱 Ticketing digital

- emisión idempotente de entradas por venta pagada;
- PDF descargable;
- QR individual;
- Apache PDFBox;
- ZXing;
- firma Ed25519;
- huella SHA-256;
- código antifraude;
- estados activa, utilizada, cancelada y vencida;
- revocación asociada a reembolsos o cancelaciones.

### 🚪 Control de acceso

- escáner web mediante cámara;
- entrada manual de respaldo;
- validación transaccional;
- control de primer acceso y reingreso;
- detección de duplicados y entradas inválidas;
- auditoría de intentos;
- dispositivo, fecha e IP cuando corresponde;
- almacenamiento de huellas en lugar del contenido QR sensible cuando aplica.

### 💸 Liquidaciones

- cálculo de comisión de Eventix;
- neto del organizador;
- estados pendiente, procesando, pagada, fallida y cancelada;
- prevención de doble liquidación;
- confirmaciones explícitas en operaciones financieras;
- trazabilidad administrativa.

### 📊 Reportes y dashboard

- métricas de ventas e ingresos;
- eventos y entradas;
- ocupación y asistencia;
- información para organizadores;
- reportes operativos y financieros.

### 🔔 Notificaciones

- infraestructura para notificaciones transaccionales;
- preferencias de usuario;
- recordatorios y eventos operativos;
- evolución prevista hacia un centro interno de notificaciones en v1.3.0.

---

## ♿ Accesibilidad

Eventix adopta **NORTIC B2:2017 / WCAG 2.0 Nivel AA** como objetivo técnico interno.

La interfaz incorpora, entre otros:

- `lang="es"`;
- landmarks;
- navegación con teclado;
- `aria-current`;
- `aria-describedby` y `aria-invalid`;
- regiones `aria-live`;
- `role="status"` y `role="alert"`;
- estados de foco visibles;
- soporte de `prefers-reduced-motion`;
- soporte de `forced-colors`;
- controles de contraste para temas claro y oscuro;
- protecciones de layout para zoom elevado.

La documentación de accesibilidad se encuentra en:

[`docs/accessibility-nortic-b2.md`](docs/accessibility-nortic-b2.md)

> La implementación técnica y las pruebas internas no equivalen a una certificación oficial de OGTIC.

---

## 🌓 UI / UX

- interfaz responsive con Bootstrap 5;
- Thymeleaf para renderizado server-side;
- temas `light`, `dark` y preferencia del sistema;
- dashboard operativo;
- formularios accesibles;
- feedback de errores y estados;
- landing pública;
- mapas y portadas de eventos;
- vistas específicas según rol.

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
| Build | Maven |
| PDF | Apache PDFBox |
| QR | ZXing |
| Testing | JUnit 5, Mockito, Spring Test, Testcontainers |
| Contenedores | Docker / Docker Compose |
| CI/CD | GitHub Actions |
| Seguridad CI | Dependency Review + Trivy |
| Supply Chain | SBOM |

---

## 🗄️ Base de datos

Eventix utiliza **SQL Server 2022** y administra el esquema exclusivamente mediante **Flyway**.

Las migraciones se encuentran en:

```text
src/main/resources/db/migration/
```

La evolución actual incluye migraciones hasta:

```text
V18__add_generic_eligibility_groups.sql
```

Entre los dominios persistidos se encuentran:

- usuarios y roles;
- eventos y categorías;
- reservaciones;
- ventas y tipos de entrada;
- pagos;
- tickets;
- cupones;
- liquidaciones;
- notificaciones;
- instituciones/promociones escolares;
- verificaciones de elegibilidad;
- intentos de verificación;
- grupos genéricos de elegibilidad;
- membresías;
- beneficios.

---

## ⚙️ Configuración local

### Requisitos

- Java 21
- Maven 3.9+
- Docker Desktop o SQL Server 2022
- Git

### Clonar

```bash
git clone https://github.com/Jairo0811/Eventix.git
cd Eventix
```

### Ejecutar pruebas

```bash
mvn clean verify
```

### Ejecutar con Docker Compose

Revisa las variables de entorno del repositorio y luego ejecuta:

```bash
docker compose up --build
```

Para detener:

```bash
docker compose down
```

### 📱 Acceso desde un móvil en la red local

Con el perfil de desarrollo, Spring Boot escucha en `0.0.0.0:8080`. Para probar Eventix desde un teléfono conectado a la misma red que la PC:

1. Levanta la aplicación con el perfil `dev`.
2. Obtén la IPv4 de la PC con `ipconfig`.
3. Abre en el navegador móvil:

```text
http://<IP-DE-LA-PC>:8080
```

Ejemplo:

```text
http://192.168.1.50:8080
```

Si Windows solicita permiso de firewall para Java, permite únicamente redes privadas.

---

## 🔒 Variables sensibles

Los secretos no deben persistirse en el repositorio.

Ejemplos de configuración sensible:

```text
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
EVENTIX_ELIGIBILITY_HMAC_SECRET
GOOGLE_MAPS_EMBED_API_KEY
```

En producción deben suministrarse mediante variables de entorno o un gestor de secretos.

---

## 🧪 Calidad y CI

El workflow principal valida el proyecto antes de considerar una integración segura.

### Pipeline

1. checkout del repositorio;
2. Java 21;
3. credenciales Docker efímeras;
4. `mvn clean verify`;
5. pruebas y evidencia;
6. arranque con Docker Compose;
7. readiness y login;
8. headers de seguridad;
9. reinicio persistente y rotación de credenciales;
10. análisis de imagen con Trivy;
11. generación de SBOM;
12. Dependency Review.

El bloque de Eligibility & Benefits integrado en `main` fue validado con este pipeline completo antes de su fusión.

---

## 🛡️ Seguridad

Entre las decisiones de seguridad del proyecto se encuentran:

- BCrypt para contraseñas;
- CSRF habilitado;
- autorización por servicio además de rutas;
- validación backend para reglas comerciales;
- HMAC-SHA-256 para búsquedas de identidad sensibles;
- prevención de almacenamiento innecesario de cédulas completas;
- auditoría de verificaciones;
- bloqueo transaccional en operaciones críticas;
- rate limiting;
- CSP y HSTS;
- Dependency Review;
- Trivy;
- generación de SBOM;
- secretos externos al código fuente.

---

## 📈 Roadmap

### v1.3.0 — Customer & Organizer Experience

**Customer Experience**
- [x] checkout transaccional existente
- [x] ticketing digital
- [ ] consolidar “Mis entradas”
- [ ] historial de compras y detalle de órdenes
- [ ] experiencia postcompra completa

**Commerce Core**
- [x] reservaciones y ventas
- [x] cupones
- [x] liquidaciones
- [ ] formalizar refund parcial/completo
- [ ] recalcular comisión/neto ante reembolsos
- [ ] ampliar pruebas de concurrencia e inventario

**Eligibility & Benefits**
- [x] verificación escolar contra padrón autorizado
- [x] HMAC de cédula
- [x] importación CSV
- [x] revisión manual y auditoría
- [x] grupos genéricos
- [x] membresías verificadas
- [x] modos `PUBLIC`, `PRIVATE`, `CONTROLLED_ACCESS`
- [x] autorización backend en checkout
- [x] límite de compra por beneficio
- [x] entrada exclusiva por grupo
- [ ] flujo completo de relaciones familiares
- [ ] inventario reservado
- [ ] descuento porcentual/fijo aplicado al checkout
- [ ] entrada gratuita por beneficio
- [ ] early access con ventana comercial explícita
- [ ] panel administrativo completo de Eligibility & Benefits

**Organizer Experience**
- [x] dashboard y liquidaciones base
- [ ] Dashboard 2.0
- [ ] ventas por día
- [ ] ticket promedio
- [ ] rendimiento por tipo de entrada/promoción
- [ ] próximos settlements
- [ ] administración de grupos y beneficios

**Engagement, Audit & QA**
- [x] auditoría de verificaciones
- [x] pipeline de seguridad
- [ ] centro interno de notificaciones
- [ ] pruebas adicionales de abuso de elegibilidad
- [ ] cierre de QA manual de accesibilidad

### v1.4.0 — Pagos productivos

- integración de gateway comercial real;
- contratos/adaptadores de pagos productivos;
- webhooks y reconciliación;
- reembolsos externos;
- estrategia para wallets digitales según proveedor y mercado objetivo.

---

## 📌 Estado del proyecto

| Área | Estado |
|---|---|
| Arquitectura modular | ✅ Implementada |
| Seguridad base | ✅ Implementada |
| Usuarios y roles | ✅ Implementado |
| Eventos | ✅ Implementado |
| Reservaciones | ✅ Implementado |
| Ventas | ✅ Implementado |
| Cupones | ✅ Implementado |
| Ticketing QR/PDF | ✅ Implementado |
| Control de acceso | ✅ Implementado |
| Liquidaciones | ✅ Implementado |
| Elegibilidad escolar | ✅ Implementada |
| Elegibilidad genérica | 🟡 En evolución |
| Experiencia de organizador 2.0 | 🟡 En evolución |
| Centro de notificaciones | 🟡 Pendiente |
| Gateway real de producción | 🔵 Planificado para v1.4.0 |
| QA manual de accesibilidad | 🟡 Pendiente de cierre |

---

## 🤝 Contribución

El repositorio utiliza ramas de trabajo y Pull Requests para cambios relevantes.

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

Consulta el archivo [`LICENSE`](LICENSE) del repositorio para los términos aplicables.
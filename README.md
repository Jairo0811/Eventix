<div align="center">

<p align="center">
  <img
    src="docs/images/eventix-logo.png"
    alt="Logo de Eventix"
    width="720"
  />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/ITLA-2017--C2-0057B8?style=for-the-badge" alt="ITLA 2017-C2">
</p>

**Eventix** es una plataforma web empresarial desarrollada con **Java 21, Spring Boot y Microsoft SQL Server**, diseñada para la administración de eventos, usuarios, reservaciones, ventas de entradas, boletas digitales y control de acceso.

[![Estado](https://img.shields.io/badge/Estado-En%20desarrollo-2563EB?style=for-the-badge)](#-estado-del-proyecto)
[![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![SQL Server](https://img.shields.io/badge/SQL_Server_2022-CC2927?style=for-the-badge&logo=microsoftsqlserver&logoColor=white)](https://www.microsoft.com/sql-server)
[![Docker](https://img.shields.io/badge/Docker_Desktop-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/products/docker-desktop/)
[![Testcontainers](https://img.shields.io/badge/Testcontainers-SQL_Server-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://testcontainers.com/)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)](https://www.thymeleaf.org/)
[![Bootstrap](https://img.shields.io/badge/Bootstrap_5-7952B3?style=for-the-badge&logo=bootstrap&logoColor=white)](https://getbootstrap.com/)
[![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)

> Estado actual: **En desarrollo. Las fases 1 a 5 están completadas: seguridad, usuarios, eventos, reservaciones, ventas, boletas digitales y control de acceso.**

</div>

---

## 🧭 Continuidad académica

Programación II fue la primera de tres asignaturas cursadas con el profesor **Raydelto Hernández Perera**, dentro de una evolución progresiva en el desarrollo de software:

| Orden | Asignatura | Proyecto | Período |
|---:|---|---|---|
| 1 | Programación II | **Eventix** | 2017-C2 |
| 2 | Estructuras de Datos | [Aerolinea](https://github.com/Jairo0811/Aerolinea) | 2018-C1 |
| 3 | Programación WEB | [ITLA Crush](https://github.com/Jairo0811/ITLAcrushReact) | 2018-C2 |

Estos proyectos representan una secuencia académica enfocada en programación, estructuras de datos y desarrollo web. Actualmente están siendo preservados y modernizados como parte del portafolio profesional.

---

## 📌 Descripción

**Eventix** es una aplicación web creada como evolución profesional de un proyecto final de la asignatura **Programación II** del Instituto Tecnológico de Las Américas (ITLA).

Su objetivo es ofrecer una base sólida para administrar eventos, reservaciones, ventas, pagos, boletas digitales y control de acceso y, en fases posteriores, incorporar reportes y estadísticas avanzadas.

La solución fue construida como un **monolito modular por dominio**, con separación clara entre controladores, servicios, repositorios, entidades, DTO y vistas.

---

## ✨ Funcionalidades disponibles

- Inicio y cierre de sesión con Spring Security.
- Acceso mediante correo electrónico o nombre de usuario.
- Contraseñas cifradas con BCrypt.
- Cambio obligatorio de contraseña temporal.
- Protección CSRF y gestión segura de sesiones.
- Autorización por roles en rutas y servicios.
- Roles iniciales:
  - `ADMINISTRATOR`
  - `OPERATOR`
  - `ORGANIZER`
  - `ACCESS_STAFF`
- Dashboard adaptado al rol del usuario.
- CRUD completo de usuarios.
- Búsqueda, filtros y paginación.
- Activación y desactivación lógica.
- Cambio de rol y estado.
- Restablecimiento seguro de contraseña.
- Auditoría técnica mediante JPA Auditing.
- Migraciones de base de datos con Flyway.
- Microsoft SQL Server como motor principal.
- Pruebas de integración sobre SQL Server real mediante Testcontainers.
- Interfaz responsiva con identidad visual verde de Eventix.
- Pruebas automatizadas de autenticación, autorización y seguridad.
- CRUD completo de eventos.
- Categorías administrables con activación y desactivación.
- Estados de evento: borrador, publicado, cancelado y finalizado.
- Programación de inicio y finalización, lugar, dirección y capacidad.
- Asignación de organizador responsable.
- Portada mediante URL segura HTTP/HTTPS.
- Eventos gratuitos o de pago con precio base en pesos dominicanos.
- Reglas de transición de estado, fechas, precio y eliminación.
- Búsqueda, filtros por estado/categoría y paginación.
- Autorización por rol y propiedad del evento.
- Panel responsivo de eventos con identidad visual de Eventix.
- CRUD operativo de reservaciones con historial permanente.
- Estados de reservación: pendiente, confirmada, cancelada y expirada.
- Retención temporal de cupos con duración configurable.
- Liberación automática de inventario al vencer una reservación.
- Prevención transaccional de sobreventa mediante bloqueo pesimista.
- Prevención de reservaciones activas duplicadas por evento y correo.
- Confirmación y cancelación con motivo obligatorio.
- Búsqueda, filtros, paginación y detalle de reservaciones.
- Métricas de asistentes, pendientes, disponibilidad y ocupación por evento.
- Autorización diferenciada para administradores, operadores y organizadores.
- Configuración de tipos de entrada General, VIP, Preferencial, Estudiante, Cortesía y Personalizado.
- Precios, cupos y activación por tipo de entrada y evento.
- Ventas vinculadas de forma exclusiva a reservaciones confirmadas.
- Distribución multirrenglón de entradas con precio histórico por venta.
- Estados de venta: pendiente, pagada, reembolsada y cancelada.
- Prevención transaccional de sobreasignación por evento y tipo de entrada.
- Pagos y reembolsos simulados para Stripe, PayPal, Azul, CardNET, Qik y transferencia bancaria.
- Pasarelas desacopladas mediante el patrón Strategy para integrar proveedores reales sin alterar el dominio de ventas.
- Historial permanente de intentos aprobados y rechazados.
- Cancelaciones y reembolsos justificados con liberación automática de cupos.
- Dashboard comercial con estados e ingreso neto.
- Comprobantes de venta imprimibles y exportables a PDF desde el navegador.
- Autorización de ventas por rol y propiedad del evento.
- Emisión idempotente de una boleta digital por cada unidad de una venta pagada.
- PDF descargable y QR individual generado con Apache PDFBox y ZXing.
- Firma Ed25519, huella SHA-256 y código antifraude por boleta.
- Estados de boleta activa, utilizada, cancelada y vencida.
- Revocación automática al reembolsar la venta o cancelar el evento.
- Pases firmados para Apple Wallet y enlaces de guardado para Google Wallet cuando se configuran credenciales.
- Sincronización de cambios con Google Wallet y servicio web PassKit/APNs para Apple Wallet.
- Escáner web con cámara y entrada manual de respaldo.
- Detección transaccional de primer acceso, reingreso autorizado, duplicado, cancelación, falsificación y vencimiento.
- Bitácora de cada intento con fecha, usuario, dispositivo e IP, almacenando solo la huella del QR recibido.
- Dashboard de capacidad, emitidas, asistentes, pendientes, rechazadas, duplicados y reingresos.

---

# 🧱 Stack tecnológico

## ⚙️ Backend

<div align="center">

<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/java/java-original.svg" alt="Java" title="Java 21" width="52" height="52" />
<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/spring/spring-original.svg" alt="Spring" title="Spring Boot, Spring MVC, Spring Security y Spring Data JPA" width="52" height="52" />
<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/hibernate/hibernate-original.svg" alt="Hibernate" title="Hibernate ORM" width="52" height="52" />
<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/maven/maven-original.svg" alt="Maven" title="Apache Maven" width="52" height="52" />

</div>

| Área | Tecnología |
|---|---|
| Lenguaje | Java 21 LTS |
| Framework | Spring Boot 3.5.16 |
| Aplicación web | Spring MVC |
| Seguridad | Spring Security, BCrypt y CSRF |
| Persistencia | Spring Data JPA e Hibernate |
| Migraciones | Flyway |
| Mapeo | MapStruct |
| Construcción | Apache Maven |
| Documentos y QR | Apache PDFBox y ZXing |
| Firmas y pases | Ed25519 y Bouncy Castle |

## 🎨 Frontend

<div align="center">

<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/html5/html5-original.svg" alt="HTML5" title="HTML5" width="52" height="52" />
<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/css3/css3-original.svg" alt="CSS3" title="CSS3" width="52" height="52" />
<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/javascript/javascript-original.svg" alt="JavaScript" title="JavaScript" width="52" height="52" />
<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/bootstrap/bootstrap-original.svg" alt="Bootstrap" title="Bootstrap 5" width="52" height="52" />
<img src="https://www.thymeleaf.org/images/thymeleaf.png" alt="Thymeleaf" title="Thymeleaf" width="52" height="52" />

</div>

| Área | Tecnología |
|---|---|
| Motor de plantillas | Thymeleaf |
| Estructura | HTML5 |
| Estilos | CSS3 y Bootstrap 5 |
| Interactividad | JavaScript |
| Componentes visuales | Bootstrap Icons |
| Alertas | SweetAlert2 |
| Diseño | Interfaz responsiva con identidad verde de Eventix |

## 🗄️ Base de datos

<div align="center">

<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/microsoftsqlserver/microsoftsqlserver-plain.svg" alt="Microsoft SQL Server" title="Microsoft SQL Server 2022" width="52" height="52" />

</div>

| Área | Tecnología |
|---|---|
| Motor relacional | Microsoft SQL Server 2022 o SQL Server Express |
| Driver JDBC | Microsoft JDBC Driver for SQL Server |
| Migraciones | Flyway SQL Server |
| ORM | Hibernate |
| Base para pruebas | SQL Server 2022 en Testcontainers |

## 🧪 Pruebas y calidad

| Área | Tecnología |
|---|---|
| Pruebas unitarias | JUnit 5 |
| Pruebas de integración | Spring Boot Test y MockMvc |
| Seguridad | Spring Security Test |
| Base de datos de pruebas | Microsoft SQL Server 2022 mediante Testcontainers |
| Aislamiento de pruebas | Contenedor efímero por ejecución |
| Integración continua | GitHub Actions con Java 21 |

## 🛠️ Herramientas de desarrollo

<div align="center">

<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/git/git-original.svg" alt="Git" title="Git" width="52" height="52" />
<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/github/github-original.svg" alt="GitHub" title="GitHub" width="52" height="52" />
<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/docker/docker-original.svg" alt="Docker" title="Docker Desktop y Testcontainers" width="52" height="52" />

</div>

| Área | Tecnología |
|---|---|
| Control de versiones | Git |
| Repositorio y CI | GitHub y GitHub Actions |
| Gestión de dependencias | Maven |
| Contenedores de prueba | Docker Desktop y Testcontainers |

---

## 🏗️ Arquitectura

Eventix sigue una arquitectura por capas dentro de un monolito modular:

```mermaid
flowchart LR
    Browser["Navegador · Thymeleaf"] --> Security["Spring Security"]
    Security --> Controller["Controller"]
    Controller --> Service["Service Layer"]
    Service --> Repository["Repository"]
    Repository --> Database[(SQL Server 2022)]
    Flyway["Flyway"] --> Database
    Tests["JUnit · MockMvc"] --> Container["Testcontainers"]
    Container --> TestDatabase[(SQL Server 2022 efímero)]
    Flyway --> TestDatabase
```

Flujo principal:

```text
Controller → Service → Repository → Database
```

La autorización se aplica en dos niveles:

1. Las rutas web se protegen en `SecurityConfig`.
2. Las operaciones sensibles se protegen con `@PreAuthorize` en la capa de servicios.

---

## 🧩 Patrones de diseño

| Patrón | Ubicación | Propósito |
|---|---|---|
| MVC | Controladores y plantillas Thymeleaf | Separar entrada HTTP, modelo y presentación. |
| Repository | Repositorios de usuarios, categorías, eventos, reservaciones, ventas y pagos | Abstraer la persistencia JPA. |
| Service Layer | Servicios de usuarios, categorías, eventos, reservaciones, ventas, pagos y dashboard | Centralizar reglas, transacciones y permisos. |
| Mapper | Mapeadores de usuarios, eventos y reservaciones con MapStruct | Evitar exponer entidades JPA a las vistas. |
| Strategy | Pasarelas de pago bajo `PaymentGateway` | Sustituir la simulación por integraciones reales sin acoplar ventas al proveedor. |
| Domain Events | Eventos de venta, evento y pase | Emitir, revocar y sincronizar boletas sin acoplar los módulos. |

---

## 📁 Estructura principal

```text
src/
├── main/
│   ├── java/com/jairomatias/eventix/
│   │   ├── auth/
│   │   ├── category/
│   │   ├── config/
│   │   ├── dashboard/
│   │   ├── event/
│   │   ├── payment/
│   │   ├── role/
│   │   ├── reservation/
│   │   ├── sale/
│   │   ├── security/
│   │   ├── shared/
│   │   ├── ticket/
│   │   └── user/
│   └── resources/
│       ├── db/migration/
│       ├── static/
│       ├── templates/
│       ├── application.yml
│       ├── application-dev.yml
│       └── application-prod.yml
└── test/
    ├── java/com/jairomatias/eventix/
    │   └── config/TestcontainersConfiguration.java
    └── resources/application-test.yml
```

---

## 🚦 Estado del proyecto

**Estado general: 🚧 En desarrollo — Fases 1 a 5 completadas**

### Completado

- [x] Arquitectura base.
- [x] Seguridad y autenticación.
- [x] Gestión de usuarios y roles iniciales.
- [x] Dashboard por rol.
- [x] Migraciones con Flyway.
- [x] Integración con SQL Server.
- [x] Pruebas automatizadas y Testcontainers.
- [x] Gestión completa de eventos.
- [x] Categorías de eventos.
- [x] Estados y validaciones del ciclo de vida.
- [x] Búsqueda, filtros y paginación de eventos.
- [x] Autorización de eventos por rol y propiedad.
- [x] Ejecución reproducible con Docker Compose.
- [x] Gestión integral de reservaciones.
- [x] Estados, historial y cancelaciones justificadas.
- [x] Disponibilidad en tiempo real y prevención de sobreventa.
- [x] Retenciones temporales y expiración automática.
- [x] Vista de asistentes y ocupación por evento.
- [x] Autorización de reservaciones por rol y propiedad del evento.
- [x] Tipos de entrada, precios y cupos por evento.
- [x] Ventas vinculadas a reservaciones confirmadas.
- [x] Estados, historial, cancelaciones y reembolsos de ventas.
- [x] Pagos simulados con arquitectura extensible por proveedor.
- [x] Comprobantes y panel comercial.
- [x] Boletas digitales con PDF, QR, código único y firma Ed25519.
- [x] Google Wallet y Apple Wallet con actualización de pases.
- [x] Control de acceso, reingresos, duplicados y bitácora antifraude.
- [x] Dashboard operativo de asistencia y capacidad.

### Pendiente

- [ ] Reportes y estadísticas.

---

## 🚀 Clonar y probar Eventix

### 1. Requisitos previos

Instala y verifica:

- JDK 21.
- Maven 3.6.3 o superior.
- Microsoft SQL Server 2022 o SQL Server Express.
- SQL Server Management Studio o Azure Data Studio.
- Git.
- Docker Desktop para ejecutar las pruebas de integración con Testcontainers.
- Virtualización habilitada en BIOS/UEFI (`SVM Mode` en equipos AMD o `Intel Virtualization Technology` en equipos Intel).

```bash
java -version
mvn -version
git --version
docker version
docker ps
```

Para verificar SQL Server desde una terminal con `sqlcmd` instalado:

```bash
sqlcmd -?
```

> Docker Desktop no es obligatorio para iniciar la aplicación en desarrollo, pero sí para ejecutar la suite de integración basada en Testcontainers.

### 2. Clonar el repositorio

```bash
git clone https://github.com/Jairo0811/Eventix.git
cd Eventix
```

### 3. Crear la base de datos y el usuario técnico

Ejecuta [`EventixDb.sql`](EventixDb.sql) con una cuenta administradora de SQL Server. El script crea:

- la base de datos `EventixDb`;
- el inicio de sesión `eventix_app`;
- el usuario técnico;
- los permisos necesarios para JPA y Flyway.

El script usa modo SQLCMD y exige una contraseña técnica de al menos 12 caracteres. Desde una terminal:

```powershell
sqlcmd -S localhost -E `
  -v EVENTIX_DB_PASSWORD="define-una-clave-segura" `
  -i EventixDb.sql
```

En SQL Server Management Studio, activa **Query > SQLCMD Mode**, define `EVENTIX_DB_PASSWORD` en una consulta no guardada y ejecuta el script. No escribas la contraseña dentro de `EventixDb.sql`.

Las tablas y los datos iniciales se crean únicamente mediante las migraciones:

```text
V1__create_security_schema.sql
V2__seed_roles_and_administrator.sql
V3__create_event_management_schema.sql
V4__create_reservations_schema.sql
V5__create_sales_and_payments_schema.sql
V6__create_digital_ticketing_and_access_schema.sql
```

### 4. Configurar la conexión

La URL y el usuario predeterminados son exclusivamente para desarrollo local. `DB_PASSWORD` es obligatoria en todos los entornos y nunca debe guardarse en el repositorio:

#### PowerShell

```powershell
$env:DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=EventixDb;encrypt=true;trustServerCertificate=true"
$env:DB_USERNAME = "eventix_app"
$env:DB_PASSWORD = "tu_contraseña_segura"
```

#### Bash

```bash
export DB_URL='jdbc:sqlserver://localhost:1433;databaseName=EventixDb;encrypt=true;trustServerCertificate=true'
export DB_USERNAME='eventix_app'
export DB_PASSWORD='tu_contraseña_segura'
```

### 5. Ejecutar las pruebas

Docker debe estar iniciado porque Testcontainers levanta SQL Server 2022:

```bash
mvn clean test
```

Para ejecutar también todas las verificaciones del ciclo Maven:

```bash
mvn clean verify
```

El perfil `test` no utiliza H2 ni una emulación: conecta Flyway, Hibernate y las pruebas de integración a un contenedor efímero real de SQL Server.

### 6. Iniciar la aplicación con Maven

```bash
mvn spring-boot:run
```

Abre [http://localhost:8080](http://localhost:8080).

### 7. Iniciar toda la plataforma con Docker

El archivo `compose.yaml` levanta SQL Server 2022, prepara `EventixDb`, construye la aplicación con Java 21 y espera la salud de cada servicio:

#### PowerShell

```powershell
$env:MSSQL_SA_PASSWORD = "define-una-clave-segura"
$env:EVENTIX_DB_PASSWORD = "define-otra-clave-segura"
docker compose up --build
```

#### Bash

```bash
export MSSQL_SA_PASSWORD='define-una-clave-segura'
export EVENTIX_DB_PASSWORD='define-otra-clave-segura'
docker compose up --build
```

Las variables son obligatorias y no deben guardarse en el repositorio.

Para detener la plataforma:

```bash
docker compose down
```

Para eliminar también el volumen de desarrollo:

```bash
docker compose down -v
```

### 8. Credenciales iniciales

| Campo | Valor |
|---|---|
| Correo | `admin@eventix.local` |
| Usuario | `admin` |
| Contraseña temporal | Definida en la migración de datos iniciales para desarrollo local. |

Eventix solicita cambiar la contraseña en el primer acceso.

### 9. Ejecutar desde Apache NetBeans

1. Configura **JDK 21** como plataforma Java.
2. Abre `File > Open Project`.
3. Selecciona la carpeta que contiene `pom.xml`.
4. Espera a que NetBeans resuelva las dependencias Maven.
5. Inicia Docker Desktop si ejecutarás pruebas.
6. Usa `Run Project` para ejecutar `spring-boot:run`.
7. Usa `Test Project` para ejecutar la suite.

El archivo `nbactions.xml` incluido define las acciones de ejecutar, depurar, probar y reconstruir.

### 10. Roles y permisos de eventos

| Rol | Acceso |
|---|---|
| `ADMINISTRATOR` | Administra categorías y todos los eventos. |
| `ORGANIZER` | Crea y administra únicamente sus propios eventos. |
| `OPERATOR` | Consulta eventos publicados o finalizados. |
| `ACCESS_STAFF` | Consulta eventos publicados o finalizados. |

### 11. Roles y permisos de reservaciones

| Rol | Acceso |
|---|---|
| `ADMINISTRATOR` | Administra todas las reservaciones y consulta métricas globales. |
| `OPERATOR` | Crea, edita, confirma y cancela reservaciones. |
| `ORGANIZER` | Consulta asistentes, pendientes y ocupación de sus propios eventos. |
| `ACCESS_STAFF` | Opera el escáner y consulta la bitácora y el dashboard de acceso. |

La retención predeterminada es de 15 minutos. Puede configurarse sin modificar código:

#### PowerShell

```powershell
$env:RESERVATION_HOLD_DURATION = "PT15M"
$env:RESERVATION_EXPIRATION_SCAN_INTERVAL = "PT1M"
```

#### Bash

```bash
export RESERVATION_HOLD_DURATION='PT15M'
export RESERVATION_EXPIRATION_SCAN_INTERVAL='PT1M'
```

### 12. Configurar boletas y wallets

En desarrollo Eventix puede crear una clave Ed25519 efímera. En producción deben configurarse claves persistentes y deshabilitarse ese respaldo. Google Wallet y Apple Wallet son opcionales: si sus credenciales no están completas, la emisión PDF/QR continúa y los botones correspondientes no se muestran.

Las variables, el formato de claves, el servicio web PassKit y la matriz de permisos están documentados en [Fase 5: boletas digitales y control de acceso](docs/phase-5-digital-ticketing.md).

### 13. Problemas frecuentes

- **Testcontainers no encuentra Docker:** abre Docker Desktop y ejecuta `docker version`.
- **Java incorrecto en Maven:** `java -version` y `mvn -version` deben mostrar Java 21.
- **Flyway detecta un checksum distinto:** no edites migraciones aplicadas; crea una migración nueva.
- **SQL Server no responde:** revisa el puerto 1433 y ejecuta `docker compose ps`.
- **NetBeans usa otro JDK:** cambia la plataforma Java del proyecto a JDK 21.
- **Puerto 8080 ocupado:** define `APP_PORT` o ejecuta `APP_PORT=8081 docker compose up`.

---

## 🎓 Información académica

| Información | Detalle |
|---|---|
| 👨‍🎓 Estudiante | Francis Jairo Matías Rosario |
| 🆔 Matrícula | 2015-2984 |
| 📖 Asignatura | Programación 2 (SOF-004) |
| 👨‍🏫 Profesor | Raydelto Hernández Perera |
| 🏫 Institución | Instituto Tecnológico de Las Américas (ITLA) |
| 📅 Período académico | 2017-C2 |
| 🎯 Tipo de proyecto | Proyecto final |

---

## 👨‍💻 Autor

**Francis Jairo Matías Rosario**
[GitHub](https://github.com/Jairo0811)

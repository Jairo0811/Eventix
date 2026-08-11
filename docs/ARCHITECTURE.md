# Arquitectura de Eventix

Eventix está implementado como un **monolito modular por dominio** sobre Spring Boot. La aplicación se despliega como una sola unidad, pero cada capacidad funcional mantiene controladores, servicios, repositorios, DTO y entidades según corresponda.

## Vista general

```mermaid
flowchart LR
    User["Usuario"] --> Web["Navegador · Thymeleaf / Bootstrap / JS"]
    Web --> Security["Spring Security"]
    Security --> Controllers["Controllers"]
    Controllers --> Services["Service Layer"]

    Services --> Repositories["Spring Data Repositories"]
    Repositories --> JPA["Hibernate / JPA"]
    JPA --> SQL[("SQL Server 2022")]
    Flyway["Flyway"] --> SQL

    Services --> Ticketing["Ticketing · QR · Wallet Passes"]
    Services --> Notifications["SMTP · Boletas · Recordatorios"]
    Services --> Payments["Payment Strategies"]
    Services --> Reports["Reportes · CSV / XLSX / PDF"]
    Services --> Audit["Auditoría"]

    Payments --> Azul["AZUL"]
    Azul --> ApplePay["Apple Pay"]
    Azul --> GooglePay["Google Pay"]

    Ticketing --> AppleWallet["Apple Wallet"]
    Ticketing --> GoogleWallet["Google Wallet"]

    Actuator["Actuator · Micrometer · Prometheus"] --> Services
```

La **Service Layer** es el centro de las reglas de negocio. Los controladores coordinan HTTP y presentación; los repositorios encapsulan persistencia; las integraciones externas se mantienen detrás de contratos y estrategias específicas.

## Módulos de dominio

```mermaid
flowchart TB
    Platform["Eventix"]
    Platform --> Auth["auth / profile / user"]
    Platform --> Events["event"]
    Platform --> Reservations["reservation"]
    Platform --> Sales["sale"]
    Platform --> Promotions["promotion"]
    Platform --> Settlements["settlement"]
    Platform --> Payments["payment"]
    Platform --> Tickets["ticket"]
    Platform --> Notifications["notification"]
    Platform --> Reporting["reporting"]
    Platform --> Audit["audit"]
    Platform --> Security["security"]
    Platform --> Observability["observability"]
    Platform --> Shared["shared"]
```

Cada módulo debe mantener sus reglas dentro de su límite funcional y utilizar servicios explícitos cuando necesita colaborar con otro dominio.

## Flujo de una operación web

```mermaid
sequenceDiagram
    participant U as Usuario
    participant W as Thymeleaf / JS
    participant SS as Spring Security
    participant C as Controller
    participant S as Service
    participant R as Repository
    participant DB as SQL Server

    U->>W: acción
    W->>SS: request + sesión + CSRF
    SS->>SS: autenticar y autorizar
    SS->>C: solicitud permitida
    C->>S: DTO / comando
    S->>S: reglas de negocio y transacción
    S->>R: persistencia
    R->>DB: JPA / SQL
    DB-->>R: resultado
    R-->>S: entidades
    S-->>C: resultado
    C-->>W: vista / redirect / respuesta
```

## Pagos

```mermaid
flowchart LR
    Sale["Venta"] --> PaymentService["Payment Service"]
    PaymentService --> Strategy["PaymentGateway Strategy"]
    Strategy --> Azul["AZUL"]
    Strategy --> Transfer["Transferencia bancaria"]
    Strategy --> Other["Otros proveedores configurables"]
    Azul --> ApplePay["Apple Pay"]
    Azul --> GooglePay["Google Pay"]
    PaymentService --> History[("Historial de intentos")]
```

El dominio de ventas no conoce detalles SOAP ni credenciales de proveedores. La infraestructura de pago implementa las estrategias y devuelve resultados normalizados al servicio de negocio.

## Ticketing y control de acceso

```mermaid
flowchart LR
    PaidSale["Venta pagada"] --> Issuer["Ticket Issuer"]
    Issuer --> PDF["PDF"]
    Issuer --> QR["QR firmado · Ed25519"]
    Issuer --> Apple["Apple Wallet"]
    Issuer --> Google["Google Wallet"]

    QR --> Scanner["Escáner web"]
    Scanner --> Validation["Validación transaccional"]
    Validation --> TicketDB[("Boletas / Accesos")]
    Validation --> AccessAudit["Bitácora de acceso"]
```

La emisión es idempotente por unidad vendida y la validación protege contra duplicados, cancelaciones, falsificaciones y reutilización no autorizada.

## Notificaciones transaccionales

```mermaid
sequenceDiagram
    participant S as Servicio de negocio
    participant DB as SQL Server
    participant E as AFTER_COMMIT
    participant N as Notification Service
    participant SMTP as SMTP

    S->>DB: confirmar operación
    DB-->>S: commit
    S->>E: publicar evento
    E->>N: procesar notificación
    N->>SMTP: correo / boletas / recordatorio
    SMTP-->>N: resultado
```

El envío se realiza después del commit para que un fallo temporal de correo no revierta una venta o reservación válida.

## Persistencia y migraciones

- SQL Server 2022 es la base transaccional.
- Spring Data JPA e Hibernate encapsulan acceso relacional.
- Flyway versiona el esquema y las migraciones.
- El usuario de ejecución se mantiene separado del usuario de migraciones.
- Las pruebas de integración utilizan SQL Server real mediante Testcontainers.

## Seguridad y observabilidad

```mermaid
flowchart LR
    Request["HTTP Request"] --> Security["Spring Security / CSRF / RBAC"]
    Security --> App["Eventix"]
    App --> Audit["Auditoría"]
    App --> Logs["Logs JSON / Correlation ID"]
    App --> Actuator["Actuator"]
    Actuator --> Prom["Prometheus"]
    App --> Health["Liveness / Readiness"]
```

## Criterio de evolución

Eventix debe permanecer como monolito modular mientras sus dominios compartan transacciones, datos y ciclo de despliegue. La extracción de servicios independientes solo tendría sentido ante necesidades reales de escalabilidad, disponibilidad o despliegue autónomo.

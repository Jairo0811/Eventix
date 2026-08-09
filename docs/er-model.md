# Modelo entidad-relación

```mermaid
erDiagram
    ROLES ||--o{ USERS : asigna
    USERS ||--o{ EVENTS : organiza
    EVENT_CATEGORIES ||--o{ EVENTS : clasifica
    EVENTS ||--o{ RESERVATIONS : recibe
    USERS ||--o{ RESERVATIONS : registra
    RESERVATIONS ||--o| SALES : origina
    EVENTS ||--o{ TICKET_TYPES : ofrece
    SALES ||--|{ SALE_ITEMS : contiene
    TICKET_TYPES ||--o{ SALE_ITEMS : define
    SALES ||--o{ PAYMENT_TRANSACTIONS : registra
    SALES ||--o{ DIGITAL_TICKETS : emite
    DIGITAL_TICKETS ||--o{ TICKET_SCAN_ATTEMPTS : valida
    DIGITAL_TICKETS ||--o{ APPLE_WALLET_REGISTRATIONS : sincroniza
    USERS ||--o{ TICKET_SCAN_ATTEMPTS : escanea
    USERS ||--o{ AUDIT_LOGS : ejecuta
```

## Reglas de integridad principales

- Una reservación solo puede producir una venta.
- Cada unidad vendida produce una boleta y la secuencia es única dentro de la venta.
- El código único y el código antifraude de cada boleta son irrepetibles.
- Las cantidades, capacidades y montos tienen restricciones `CHECK`.
- Los estados válidos se restringen en SQL y en enumeraciones Java.
- `version` habilita control optimista; los flujos de inventario y escaneo agregan bloqueo pesimista.
- `audit_logs` es append-only desde la aplicación: no existe interfaz para modificar o eliminar registros.

Las definiciones autoritativas están en `src/main/resources/db/migration/V1` a `V7`.

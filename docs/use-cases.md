# Casos de uso

| ID | Actor | Caso de uso | Resultado |
|---|---|---|---|
| CU-01 | Usuario | Iniciar/cerrar sesión y cambiar contraseña | Sesión segura y evento auditado. |
| CU-02 | Administrador | Gestionar usuarios, roles y categorías | Catálogos actualizados con autorización doble. |
| CU-03 | Administrador/Organizador | Crear y publicar eventos | Evento disponible según su ciclo de vida. |
| CU-04 | Operador | Registrar, confirmar o cancelar reservaciones | Cupos retenidos o liberados transaccionalmente. |
| CU-05 | Operador | Crear una venta y distribuir tipos de entrada | Inventario comercial validado. |
| CU-06 | Operador | Simular pago, reembolso o cancelación | Transacción y estado de venta persistidos. |
| CU-07 | Sistema | Emitir boletas tras una venta pagada | PDF, QR, firma y pases Wallet disponibles. |
| CU-08 | Personal de acceso | Escanear una boleta | Válida, reingreso, duplicada, cancelada, falsa o vencida. |
| CU-09 | Administrador/Organizador | Consultar reportes | Indicadores limitados al alcance del usuario. |
| CU-10 | Administrador/Organizador | Exportar CSV, XLSX o PDF | Archivo descargable y exportación auditada. |
| CU-11 | Administrador | Consultar auditoría | Trazabilidad filtrable por usuario, tipo, resultado y fecha. |
| CU-12 | Operaciones | Consultar salud y métricas | Estado de disponibilidad y métricas Prometheus. |

## Reglas de autorización

| Capacidad | ADMINISTRATOR | OPERATOR | ORGANIZER | ACCESS_STAFF |
|---|:---:|:---:|:---:|:---:|
| Usuarios y categorías | ✓ | — | — | — |
| Eventos | Todos | Consulta | Propios | Consulta |
| Reservaciones y ventas | Todas | Operación | Propias | — |
| Boletas | Todas | Operación | Propias | — |
| Control de acceso | ✓ | ✓ | Propios | ✓ |
| Reportes | Todos | — | Propios | — |
| Auditoría central | ✓ | — | — | — |

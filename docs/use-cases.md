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
| CU-13 | Cliente | Comprar entradas desde un evento | Venta, pago de demostración y boletas asociadas a su cuenta. |
| CU-14 | Cliente | Consultar compras y “Mis entradas” | Historial, detalle, QR, PDF y Wallet disponibles. |
| CU-15 | Administrador/Organizador | Gestionar elegibilidad y beneficios | Grupos, miembros y privilegios limitados al evento autorizado. |
| CU-16 | Cliente/Administrador/Organizador | Solicitar o revisar un vínculo familiar | Relación aprobada, rechazada o revocada con trazabilidad. |
| CU-17 | Administrador/Operador | Reembolsar boletas específicas | Importe proporcional, boletas anuladas y ledger actualizado. |
| CU-18 | Administrador/Organizador | Gestionar o consultar liquidaciones | Comisión y neto reconstruibles; pago externo registrado. |
| CU-19 | Usuario autenticado | Consultar notificaciones internas | Eventos persistentes con estado leído/no leído. |

## Reglas de autorización

| Capacidad | ADMINISTRATOR | OPERATOR | ORGANIZER | ACCESS_STAFF | USER |
|---|:---:|:---:|:---:|:---:|:---:|
| Usuarios y categorías | ✓ | — | — | — | Perfil propio |
| Eventos | Todos | Consulta | Propios | Consulta | Descubrimiento |
| Reservaciones y ventas | Todas | Operación | Propias | — | Compra propia |
| Boletas | Todas | Operación | Propias | — | Propias |
| Control de acceso | ✓ | ✓ | Propios | ✓ | — |
| Elegibilidad y beneficios | Todos | — | Eventos propios | — | Membresía propia |
| Liquidaciones | Gestiona | — | Consulta propias | — | — |
| Notificaciones internas | Propias | Propias | Propias | Propias | Propias |
| Reportes | Todos | — | Propios | — | — |
| Auditoría central | ✓ | — | — | — | — |

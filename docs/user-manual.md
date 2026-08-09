# Manual del usuario

## Acceso inicial

1. Abre `/login`.
2. Ingresa con la cuenta entregada por el administrador.
3. Si la contraseña es temporal, Eventix obliga a cambiarla antes de continuar.

## Flujo operativo

1. El administrador crea categorías, usuarios y organizadores.
2. El administrador u organizador crea el evento, configura fechas, capacidad y tipos de entrada, y lo publica.
3. El operador registra una reservación. Los cupos quedan retenidos durante el tiempo configurado.
4. El operador confirma la reservación, crea la venta y procesa el pago simulado.
5. Eventix genera automáticamente las boletas PDF/QR y las opciones Wallet configuradas.
6. El personal de acceso escanea cada QR desde `Control de acceso`.

## Reportes

`Reportes` permite filtrar por fechas, evento y categoría. El administrador también puede elegir organizador; un organizador solo ve sus propios eventos.

Los botones CSV, Excel y PDF exportan exactamente el filtro activo. Ingresos y porcentajes se calculan con ventas pagadas y primeros accesos.

## Auditoría

Solo el administrador abre `Auditoría`. Puede buscar por usuario, acción o correlación y filtrar por tipo, resultado y fecha. Los registros son informativos y no pueden editarse desde la interfaz.

## Interpretación del escáner

| Resultado | Significado |
|---|---|
| `VALID` | Primer ingreso aceptado. |
| `REENTRY` | Reingreso permitido por configuración. |
| `DUPLICATE` | La boleta ya se utilizó y el reingreso no está permitido. |
| `CANCELLED` | La venta o boleta fue revocada. |
| `COUNTERFEIT` | Código, firma o contenido no válido. |
| `EXPIRED` | El evento o la boleta venció. |

## Buenas prácticas

- No compartas cuentas.
- Verifica evento y comprador antes de cobrar.
- No desactives la cámara durante una jornada de acceso sin disponer de entrada manual.
- Reporta al administrador el `X-Correlation-ID` cuando ocurra un error.

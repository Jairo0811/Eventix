# Manual del usuario

## Acceso inicial

1. Abre `/login`.
2. Ingresa con la cuenta entregada por el administrador.
3. Si la contraseña es temporal, Eventix obliga a cambiarla antes de continuar.

## Compra del cliente

1. Inicia sesión con una cuenta `USER`.
2. Abre un evento publicado y selecciona `Comprar entradas`.
3. Eventix muestra únicamente tipos de entrada disponibles y autorizados para
   tu elegibilidad.
4. Selecciona tipo, cantidad, cupón si aplica y proveedor de demostración.
5. Confirma la compra. Eventix revalida cupos, precio, cupón, membresía y
   beneficios en el servidor.
6. Consulta el resultado, QR, PDF y pases configurados desde `Mis entradas`.

Los descuentos monetarios de elegibilidad se aplican automáticamente. No se
combinan con cupones en v1.3.x. Los proveedores PayPal, Stripe, AZUL, CardNET,
Qik y transferencia del checkout continúan siendo simulados hasta v1.4.0.

## Elegibilidad y relaciones

Los eventos `PRIVATE` o `CONTROLLED_ACCESS` requieren una membresía verificada.
Una autodeclaración no concede acceso. Los administradores u organizadores
propietarios pueden gestionar grupos, miembros, beneficios y solicitudes
familiares; toda aprobación, rechazo o revocación queda trazable.

La verificación escolar compara identidad y nombre contra un padrón autorizado.
Las discrepancias pasan a revisión manual y no conceden beneficios mientras
estén pendientes.

## Flujo operativo administrativo

1. El administrador crea categorías, usuarios y organizadores.
2. El administrador u organizador crea el evento, configura fechas, capacidad y tipos de entrada, y lo publica.
3. El operador registra una reservación. Los cupos quedan retenidos durante el tiempo configurado.
4. El operador confirma la reservación, crea la venta y procesa el pago simulado.
5. Eventix genera automáticamente las boletas PDF/QR y las opciones Wallet configuradas.
6. El personal de acceso escanea cada QR desde `Control de acceso`.

## Liquidaciones

Eventix calcula ventas brutas, descuentos, reembolsos, comisión de plataforma y
neto del organizador. El desembolso no es automático en v1.3.x: el administrador
debe pagar mediante un medio externo, registrar la referencia y solo entonces
marcar la liquidación como pagada.

## Notificaciones internas

La navegación `Notificaciones` muestra compras, reembolsos y otros eventos
internos. Cada registro puede marcarse como leído. El correo transaccional es un
canal separado y depende de que SMTP esté configurado.

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

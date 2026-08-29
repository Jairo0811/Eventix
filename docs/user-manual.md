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

### Verificar una promoción escolar

Una cuenta `USER` puede abrir `Verificar promoción` en la navegación lateral.

1. Selecciona la institución y promoción disponibles.
2. Introduce la cédula que figura en el padrón autorizado.
3. Eventix genera internamente una clave HMAC para localizar el registro; la
   cédula completa no se persiste en el padrón.
4. Si cédula y nombre coinciden, la verificación queda `VERIFIED`.
5. Si la cédula coincide pero el nombre requiere comprobación, el estado queda
   `MANUAL_REVIEW` hasta que un administrador tome una decisión.
6. Si no existe coincidencia, Eventix registra el intento sin conceder acceso ni
   beneficios.

Una verificación `VERIFIED` se sincroniza automáticamente con los grupos
`PROMOTION_MEMBER` vinculados a esa promoción. A partir de ese momento el
checkout puede reconocer los beneficios configurados para esos grupos. Una
revocación o rechazo administrativo retira las membresías derivadas.

### Administración de promociones escolares

Solo `ADMINISTRATOR` puede abrir `Promociones escolares`.

1. Registra la institución y su código interno.
2. Crea la promoción y su año de graduación.
3. Abre `Padrón` y descarga la plantilla CSV si la necesita.
4. Importa el listado oficial con el encabezado exacto:
   `full_name,student_code,national_id,source_reference`.
5. Revisa los miembros aceptados y el historial de importaciones.
6. Abre `Ver verificaciones` para aprobar, rechazar o revocar casos que requieran
   intervención administrativa, registrando siempre una justificación.

El importador limita cada archivo a 5 MB, evita volver a procesar el mismo
archivo mediante checksum SHA-256 y detecta duplicados dentro del padrón.

Para otorgar beneficios en un evento, el administrador u organizador propietario
abre la elegibilidad del evento, crea un grupo `PROMOTION_MEMBER` y selecciona la
promoción escolar correspondiente. Los usuarios ya verificados se sincronizan
con el nuevo grupo y los futuros usuarios se incorporan cuando completen su
verificación.

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
- Usa únicamente padrones autorizados y conserva la referencia de origen.
- No compartas archivos de padrón fuera del flujo administrativo autorizado.
- No desactives la cámara durante una jornada de acceso sin disponer de entrada manual.
- Reporta al administrador el `X-Correlation-ID` cuando ocurra un error.

# Eventix — CI de navegador y compra simulada

Este módulo solo se ejecuta dentro del entorno **efímero de GitHub Actions**.
El script rechaza cualquier URL distinta de `localhost:8080` o
`127.0.0.1:8080`; el código de creación de escenarios exige además
`GITHUB_ACTIONS=true`.

## Cobertura automatizada

- **55 capturas** con Chromium/Playwright: portada pública, catálogo, login,
  dashboard administrativo, reportes, inicio de comprador, mis boletas,
  dashboard organizador, checkout de demostración, mapa de asientos y
  detalle de boleta con QR, en 360, 390, 768, 1024 y 1440 px.
- Autenticación del administrador, cambio obligatorio de contraseña temporal,
  pruebas de acceso por rol `USER` y `ORGANIZER` y menú móvil con teclado.
- Creación de datos sintéticos a través de los **formularios reales**:
  categoría, recinto, sección `RESERVED_SEATING`, fila, tres asientos
  posicionados, evento de prueba asignado a un organizador, entrada VIP,
  inventario y tarifa por sección.
- Un comprador sintético selecciona y **retiene un asiento real** en el
  inventario SQL Server temporal; continúa al checkout y registra una venta
  aprobada por el componente `SimulatedPaymentGateway` con proveedor
  `CARDNET`. No se contactan gateways de cobro reales.
- Tras la venta, se exige una boleta del comprador, QR PNG visible y
  servido con HTTP 200 y PDF descargable con cabecera `%PDF`.
- Se valida que el mapa móvil muestre inicialmente un asiento seleccionable,
  que el correo permanezca ligado a la cuenta en modo lectura y que
  ninguna de las páginas provoque overflow horizontal global.

## Seguridad y límites

**Todos los usuarios, recintos, eventos, asientos y ventas son ficticios y
solo existen en el SQL Server temporal del CI.** El entorno se destruye al
terminar el workflow. Las contraseñas se generan aleatoriamente y se
enmascaran en los logs. Ninguna credencial se guarda en el repositorio.

El test no cubre dinero real, devoluciones, Apple/Google Wallet, proveedor
bancario en producción, reconocimiento físico del código QR en un control de
acceso, pruebas con dos compradores simultáneos ni permisos de los roles
`OPERATOR` y `ACCESS_STAFF`. La aprobación del CI **no equivale a aceptar
Eventix para producción**.

El artifact `eventix-browser-<SHA>` guarda las capturas y `manifest.json`
durante 14 días. GitHub Actions debe notificar fallo si una de las
comprobaciones no se cumple. Una revisión manual de las imágenes sigue
siendo necesaria antes de fusionar.

## Ejecución

Se integra en `.github/workflows/ci.yml`. El workflow prepara un
`docker compose` aislado y seis contraseñas aleatorias para administrador,
comprador y organizador (iniciales y definitivas). Node.js utiliza
Playwright 1.65.0 + Chromium.

No ejecutar la prueba sobre el sitio público, una base persistente, Render,
Netlify ni un entorno compartido.

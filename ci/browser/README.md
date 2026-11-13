# Eventix — pruebas reales de navegador en CI

Este módulo es exclusivo del `docker compose` **efímero de GitHub Actions**. No se puede
ejecutar contra servicios públicos ni de producción: el script restringe explícitamente
la URL a localhost/127.0.0.1, puerto 8080.

## Alcance verificado por Chromium

- Capturas *reales* de portada pública, catálogo, login, dashboard administrativo,
  reportes, inicio del comprador, sus boletas y dashboard de organizador (8 páginas ×
  5 anchos: 360, 390, 768, 1024 y 1440 px = **40 capturas**).
- Búsqueda pública y acceso protegido: visitante sin sesión se redirige al login para
  `/dashboard` y `/my/checkout/events/999999`.
- Inicio de sesión del administrador sembrado **solo en la base SQL Server del CI**,
  cambio obligatorio de contraseña temporal a una nueva contraseña aleatoria,
  reinicio de sesión y acceso a `/dashboard` y `/reports`.
- El administrador crea **solo en la base de CI** un comprador y un organizador
  sintéticos, ambos completan el cambio obligatorio de contraseña y acceden a
  sus paneles correspondientes. El comprador no puede abrir `/dashboard` y el
  organizador no puede abrir `/users/new`.
- En 390 px: abrir el menú lateral, cerrar con Escape recuperando el foco, y cerrar
  con el botón interno.
- Manifiesto JSON con commit, ruta, resolución, título y métricas de ancho de documento.

## Alcance expresamente NO cubierto

No inventar eventos ni compras; el catálogo puede estar vacío. Los usuarios
comprador y organizador del CI son registros sintéticos creados exclusivamente en la
base efímera de esta ejecución. No se ejecutan compras, devoluciones, pasarelas
bancarias, Apple/Google Wallet, selección de asientos, ni pruebas de privacidad
entre distintos organizadores. Tampoco se cubren OPERATOR o ACCESS_STAFF.

Las capturas no equivalen a la **revisión visual manual**. Hasta que se inspeccionen las
imágenes y se complete la matriz por rol, no declarar liberación estable ni mergear
el PR.

## Evidencia y seguridad

- El CI genera contraseñas aleatorias para esta ejecución; no se suben al repositorio,
  no se imprimen y no se incluyen en capturas.
- El servidor se ejecuta con SQL Server Developer **solo como entorno efímero de CI**.
  No reutilizar ni dirigir el script a bases persistentes.
- El artifact `eventix-browser-<SHA>` contiene PNG y `manifest.json` durante 14 días.
- En caso de error, GitHub Actions mantiene el registro y sube las capturas que se
  hayan logrado antes del fallo; la ausencia de archivos no se interpreta como éxito.

Para ejecutar manualmente hace falta un `docker compose` descartable en localhost con
`EVENTIX_BOOTSTRAP_ADMIN_PASSWORD` y `EVENTIX_BROWSER_NEXT_PASSWORD` definidos
mediante un canal seguro, Node.js y Chromium instalados con Playwright 1.65.0.

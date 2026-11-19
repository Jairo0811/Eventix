# Eventix Demo — criterios de aceptación (13/11/2026; revisión 19/11/2026)

## Propósito

Publicar una **demostración navegable y no comercial** de Eventix a partir del
perfil `render`, manteniendo a SQL Server y la configuración productiva aislados.
La aplicación completa (Spring Boot + Thymeleaf) se ejecuta en Render; la web
de Netlify es únicamente un escaparate estático.

Sitios preexistentes (verificar su estado en el panel antes de anunciar):
- Render: https://eventix-n09c.onrender.com
- Netlify: https://eventixrd.netlify.app

## Garantías del catálogo de ejemplo

La inicialización `RenderDemoCatalogInitializer` está restringida por dos
guardas independientes:

1. La clase está activada **solo con el perfil Spring `render`**.
2. La URL de datasource debe comenzar con `jdbc:h2:mem:`. En caso
   contrario, la aplicación falla en el arranque antes de sembrar datos.

El arranque incluye cinco roles de la aplicación y un organizador ficticio,
**INACTIVO, con contraseña aleatoria no reutilizable**, exclusivamente para
establecer las relaciones del catálogo. **No crea cuentas administrativas,
ventas, pagos, compradores, boletas ni credenciales públicas.** Sembrará
tres eventos visibles, categorizados y futuros, junto con sus tipos de
entrada:

| Evento ficticio | Categoría | Tipo | Precio referencial |
| --- | --- | --- | ---: |
| Eventix Tech Experience — DEMO | Tecnología | General | RD$599 |
| Noche de Música — DEMO | Música | General | RD$750 |
| Encuentro Creativo — DEMO | Cultura | Gratis | RD$0 |

Los recintos, las direcciones y los organizadores también son ficticios.
Estas referencias no representan ofertas, reservas ni ventas reales.

## Advertencias obligatorias

- La banda visible en el encabezado del catálogo declara que todo es
  demostrativo, que no hay cobros reales y que los datos pueden desaparecer.
- Los metadatos de los documentos públicos incluyen `noindex,nofollow`
  únicamente en Render demo. El entorno comercial conserva su SEO.
- H2 está en **memoria** y se reinicia con cada despliegue, reinicio o
  suspensión del servicio gratuito; no ofrece continuidad para boletas,
  cuentas ni transacciones.
- El gateway simulado existente **no conecta con CARDNET, Azul, Qik ni otra
  pasarela bancaria real**. No compartir capturas de «pago aprobado» como
  evidencia de capacidad comercial.
- Las claves de firma efímeras del perfil Render no son aptas para emitir
  entradas verificables tras reinicios.
- No introducir datos reales de participantes, clientes ni tarjetas en la demo.

## Estado verificado en repositorio — 19/11/2026

- `main`: `4920c775471a4013c8830bb8afd84c7c91c05c45` (PR #105).
- PR #104 (catálogo ficticio Render) **fusionado**; PR #103 (UX móvil y E2E
  simulado) y #105 (esquema H2) también integrados.
- Ejecución [Eventix CI #43521476245](https://github.com/Jairo0811/Eventix/actions/runs/43521476245):
  **success** sobre ese SHA. El commit registra 492 pruebas Java y 55 capturas
  Chromium, más Docker, Trivy y SBOM.
- La [release formal en GitHub](https://github.com/Jairo0811/Eventix/releases)
  sigue siendo **v1.3.4**. El workflow de publicación posterior a #105 terminó
  en success, pero **no generó una release nueva**.
- El código contiene un catálogo exclusivo del perfil `render` con H2 en
  memoria y acceso público informativo. Estos resultados de pruebas **no
  verifican** por sí solos el estado real del servicio Render ni el deploy de
  Netlify.
- Al revisar desde este entorno no fue posible alcanzar de forma verificable
  `eventix-n09c.onrender.com` ni `eventixrd.netlify.app`. No se inventa
  evidencia de HTTP 200, de versión live o de capturas públicas.

## Matriz de aceptación

**CI y código (verificables sin entrar a los paneles de hosting)**

- [x] PR #104 de catálogo Render fusionado.
- [x] `main` CI aprobado para `4920c775` (Maven, SQL Server/Docker,
      Playwright, Trivy y SBOM según la ejecución enlazada).
- [x] Garantías del modo demo cubiertas por tests: tres eventos ficticios,
      organizador inactivo, perfil `render`, prohibición de SQL Server en el
      inicializador, etiquetas DEMO y ausencia de compras en la ficha.
- [x] Configuración del demo con wallets y proveedor de pago externo
      deshabilitados por defecto. **Esto no verifica bancos reales**.
- [x] Existe una verificación remota reutilizable de **solo lectura** en
      `ci/browser/public-demo-smoke.mjs` y su workflow manual
      `Public Eventix Demo Smoke`, con capturas en cinco resoluciones.

**Despliegues públicos (pendientes hasta evidencia de una ejecución remota)**

- [ ] Render deploy del commit `4920c775` o posterior marcado **live**
      en el panel, con SHA exacto verificado.
- [ ] GET `/` público: aparecen los eventos ficticios y banda DEMOSTRACIÓN.
- [ ] GET `/e` público: tres tarjetas, tres categorías y búsqueda por título.
- [ ] GET `/e/{id}` público: ficha ficticia sin botón de compra.
- [ ] GET `/actuator/health` público: HTTP 200.
- [ ] Netlify público: home estático actualizado, filtros y ficha ficticia
      funcionando, sin enlaces a cuentas o pagos.
- [ ] Metadatos `noindex,nofollow` comprobados en ambos orígenes.
- [ ] Verificación real sin secretos expuestos en commits, logs y artifacts;
      requiere revisión privada de los logs que no se publican.
- [ ] Capturas reales en 360/390/768/1024/1440 px de **ambos despliegues**,
      inspeccionadas visualmente con ausencia de overflow y errores.
- [ ] QA manual: teclado, lector de pantalla, zoom 200 % y contraste.
- [ ] Confirmación documentada del deploy y de la inexistencia de gateway
      bancario productivo activo.

## Cómo verificar la demo pública sin modificar datos

1. Abrir [GitHub Actions → Public Eventix Demo Smoke](https://github.com/Jairo0811/Eventix/actions/workflows/public-demo-smoke.yml).
2. Ejecutar **Run workflow** desde la rama predeterminada después de que
   ambos despliegues estén publicados.
3. Esperar el resultado y descargar el artifact
   `eventix-public-demo-<run_id>`.
4. Revisar `manifest.json` y las 20 capturas de Render (`/`, `/e`,
   `/e/{id}`) y Netlify (`/`) en cinco anchos.
5. Confirmar en el panel de Render que la **versión live** coincide con
   el SHA esperado; Playwright no prueba esa correspondencia.
6. Si un paso falla por suspensión o error de hosting, registrar causa,
   fecha y URL. No sustituir el fallo por una casilla marcada.

Este workflow nunca inicia sesión, modifica datos, intenta comprar ni carga
credenciales productivas. En Render gratuito H2 puede reiniciarse; por ello
la prueba consulta únicamente páginas públicas de muestra.

## Continuidad

La demo pública no es una **release de pagos productivos**. La última
publicación formal en GitHub sigue siendo v1.3.4 hasta que se alinee
`pom.xml`, `docs/releases` y se ejecute el workflow de release.
No cambiar el número de versión ni generar tags comerciales como
consecuencia automática de este trabajo.

Tras cerrar este bloque: iniciar la especificación de v1.4.0
(webhooks bancarios, idempotencia, reembolsos, conciliación, liquidaciones,
datos persistentes y pruebas reales de producción) **en una rama aparte**.

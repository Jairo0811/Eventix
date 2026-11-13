# Eventix Demo — criterios de aceptación (13/11/2026)

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

## Matriz de aceptación

- [ ] PR de catálogo render fusionado tras CI verde.
- [ ] `main` CI: Maven completo, Docker con SQL Server, Playwright (55
      capturas), Trivy y SBOM aprobados.
- [ ] Render deploy del commit de `main` marcado `live`.
- [ ] GET `/`: aparecen tres eventos ficticios y banda DEMOSTRACIÓN.
- [ ] GET `/e`: muestra las tres categorías y la búsqueda por título.
- [ ] GET `/e/{id}`: ficha del evento y su naturaleza ficticia.
- [ ] GET `/actuator/health`: HTTP 200.
- [ ] No aparecen claves ni contraseñas en commits, logs o artifacts.
- [ ] Metadatos `noindex` presentes en perfiles Render.
- [ ] Verificación visual de 360/390/768/1024/1440 px en Render real.
- [ ] Confirmación explícita de que no existe flujo bancario productivo.

## Continuidad

La demo pública no es una **release de pagos productivos**. La última
publicación formal en GitHub sigue siendo v1.3.4 hasta que se alinee
`pom.xml`, `docs/releases` y se ejecute el workflow de release.
No cambiar el número de versión ni generar tags comerciales como
consecuencia automática de este trabajo.

Tras cerrar este bloque: iniciar la especificación de v1.4.0
(webhooks bancarios, idempotencia, reembolsos, conciliación, liquidaciones,
datos persistentes y pruebas reales de producción) **en una rama aparte**.

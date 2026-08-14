# Accesibilidad Eventix — NORTIC B2:2017 Nivel AA

Eventix adopta **NORTIC B2:2017 / WCAG 2.0 Nivel AA** como objetivo interno de accesibilidad.

> Este documento describe controles técnicos y pruebas internas. No representa ni sustituye una certificación oficial de OGTIC.

## Alcance

La conformidad se evalúa sobre páginas y procesos completos. El recorrido incluye, como mínimo:

- Home pública y autenticación.
- Mi Eventix y perfil.
- Dashboard por rol.
- Usuarios, eventos y tipos de entrada.
- Reservaciones, ventas y checkout.
- Ingresos, promociones y liquidaciones.
- Boletas digitales y control de acceso.
- Reportes, categorías y auditoría.
- Páginas de error.

Cada recorrido debe verificarse en tema **claro**, **oscuro** y **sistema**.

## Controles transversales implementados

| Área | Control | Estado |
|---|---|---|
| Idioma | Documento declarado en español (`lang="es"`) | Implementado |
| Títulos | Títulos de página descriptivos mediante fragmento compartido | Implementado |
| Navegación | Landmark `nav` con nombre accesible | Implementado |
| Ubicación | Elemento activo expuesto con `aria-current="page"` | Implementado |
| Iconografía | Iconos puramente decorativos ocultos a tecnología asistiva | Implementado en shell compartido |
| Tema | Selector con nombre accesible y estado `aria-pressed` | Implementado |
| Foco | Indicador `:focus-visible` de alto contraste en claro/oscuro | Implementado |
| Contraste | Tokens de texto secundario reforzados para ambos temas | Implementado; requiere revisión visual por pantalla |
| Mensajes | Confirmaciones y errores anunciables mediante live regions | Implementado |
| Zoom | Protecciones de layout para contenido a 200 % | Implementado en base; requiere recorrido manual |
| Movimiento | Respeto a `prefers-reduced-motion` | Implementado |
| Objetivos táctiles | Altura mínima reforzada para controles principales | Implementado |
| Estados | Badges con texto y borde, sin depender exclusivamente del color | Implementado en base |

## Matriz NORTIC B2 / WCAG 2.0

### Nivel A

- [ ] 3.01.1.a — Contenido no textual: revisar `alt`, nombres accesibles y contenido decorativo en todas las vistas.
- [ ] 3.01.3.a — Información y relaciones: revisar headings, labels, tablas y agrupaciones de formularios.
- [ ] 3.01.3.b — Secuencia significativa: validar lectura y DOM en navegación por teclado/lector.
- [ ] 3.01.3.c — Características sensoriales: eliminar instrucciones dependientes solo de posición, forma o color.
- [ ] 3.01.4.a — Uso del color: verificar estados, errores, finanzas y badges.
- [ ] 3.02.1.a — Teclado: recorrido completo sin ratón.
- [ ] 3.02.1.b — Sin trampas para el foco: dropdowns, modales, cámara QR y overlays.
- [ ] 3.02.2.a — Tiempo ajustable: revisar expiración de sesión/reservas y advertencias aplicables.
- [ ] 3.02.2.b — Pausar/detener/ocultar: verificar cualquier actualización o animación automática.
- [x] 3.02.3.a — Destellos: Eventix no depende de contenido con destellos para su operación.
- [ ] 3.02.4.a — Evitar bloques: incorporar/verificar mecanismo para omitir navegación repetitiva.
- [x] 3.02.4.b — Titulado de páginas: fragmento de `<head>` compartido.
- [ ] 3.02.4.c — Orden de foco: recorrido manual completo.
- [ ] 3.02.4.d — Propósito de enlaces: revisar enlaces genéricos como “Ver”, “Abrir” y acciones icon-only.
- [x] 3.03.1.a — Idioma de la página: `lang="es"`.
- [ ] 3.03.2.a — Al recibir el foco: verificar que ningún control cambie de contexto solo al enfocarse.
- [ ] 3.03.2.b — Al recibir entradas: verificar selects/filtros y controles dinámicos.
- [ ] 3.03.3.a — Identificación de errores: revisar todos los formularios.
- [ ] 3.03.3.b — Etiquetas o instrucciones: revisar todos los campos editables.
- [ ] 3.04.1.a — Procesamiento: validar HTML renderizado, IDs únicos y estructura.
- [ ] 3.04.1.b — Nombre, función y valor: revisar componentes Bootstrap/custom y QR.

### Nivel AA

- [ ] 3.01.4.c — Contraste mínimo: 4.5:1 texto normal y 3:1 texto grande en temas claro/oscuro.
- [ ] 3.01.4.d — Cambio de tamaño del texto: recorrido al 200 % sin pérdida funcional.
- [ ] 3.01.4.e — Imágenes de texto: usar texto real salvo marca/contenido esencial.
- [ ] 3.02.4.e — Múltiples vías: revisar navegación/búsqueda para páginas no pertenecientes a procesos lineales.
- [ ] 3.02.4.f — Encabezados y etiquetas: auditoría semántica por pantalla.
- [x] 3.02.4.g — Foco visible: capa global de foco de alto contraste.
- [ ] 3.03.1.b — Idioma de las partes: marcar contenido en idioma diferente cuando corresponda.
- [ ] 3.03.2.c — Navegación coherente: validar shell por rol y páginas públicas.
- [ ] 3.03.2.d — Identificación coherente: revisar nombres de acciones equivalentes.
- [ ] 3.03.3.c — Sugerencias ante errores: revisar validación de formularios y mensajes.
- [ ] 3.03.3.d — Prevención de errores financieros/datos: checkout, reembolsos, liquidaciones y acciones destructivas.

Los criterios multimedia se consideran **no aplicables mientras Eventix no publique audio/video tempodependiente**. Si se incorpora multimedia, debe reabrirse esa evaluación.

## Pruebas manuales obligatorias antes del cierre

1. Navegar cada flujo usando únicamente `Tab`, `Shift+Tab`, `Enter`, `Space` y flechas donde aplique.
2. Verificar foco visible y orden lógico.
3. Ejecutar todos los recorridos en claro y oscuro.
4. Aplicar zoom del navegador al 200 % y verificar ausencia de pérdida de contenido/función.
5. Revisar contraste con herramienta de medición; no aprobar por inspección visual únicamente.
6. Ejecutar un smoke test con lector de pantalla (NVDA recomendado en Windows) para navegación, formularios, tablas, alertas y checkout.
7. Validar errores de formularios: identificación, asociación al campo y sugerencia correctiva.
8. Validar operaciones financieras/destructivas con confirmación o posibilidad de revisión previa.

## Criterio de cierre

Eventix solo debe documentarse como **diseñado conforme a NORTIC B2:2017 / WCAG 2.0 Nivel AA** cuando todos los criterios aplicables estén comprobados en la matriz y los recorridos manuales hayan sido completados. Una auditoría interna satisfactoria no equivale a certificación oficial.

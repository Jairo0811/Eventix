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

## Estado de la auditoría

La **pasada técnica automatizable** está implementada: estructura semántica compartida, foco visible, asociaciones de formularios, prevención de errores financieros, capas de contraste Light/Dark, soporte de `prefers-reduced-motion`, zoom responsivo, estados accesibles, control de acceso/QR y pruebas de regresión.

La conformidad global **todavía requiere la pasada manual final** indicada más abajo: teclado, zoom real al 200 %, medición de contraste, lector de pantalla y validación visual de todos los roles y pantallas. Hasta completar esa pasada, Eventix no debe presentarse como certificado ni como auditoría AA cerrada.

## Controles transversales implementados

| Área | Control | Estado |
|---|---|---|
| Idioma | Documento declarado en español (`lang="es"`) | Implementado |
| Títulos | Títulos de página descriptivos mediante fragmento compartido | Implementado |
| Navegación | Landmark `nav` con nombre accesible | Implementado |
| Ubicación | Elemento activo expuesto con `aria-current="page"` | Implementado |
| Iconografía | Iconos decorativos ocultos a tecnología asistiva | Implementado en shell y flujos auditados |
| Tema | Selector con nombre accesible y estado `aria-pressed` | Implementado |
| Foco | Indicador `:focus-visible` de alto contraste en claro/oscuro | Implementado |
| Contraste | Capas `theme-contrast.css` + `final-a11y.css` | Implementado técnicamente; medición manual pendiente |
| Mensajes | Confirmaciones y errores anunciables mediante live regions | Implementado |
| Zoom | Protecciones de layout para contenido a 200 % | Implementado en base; recorrido manual pendiente |
| Movimiento | Respeto a `prefers-reduced-motion` | Implementado |
| Objetivos táctiles | Altura mínima reforzada para controles principales | Implementado |
| Estados | Badges, resultados QR y errores no dependen exclusivamente del color | Implementado |
| Formularios | `aria-invalid`, `aria-describedby`, errores y ayudas asociadas | Implementado en flujos principales |
| Finanzas | Revisión/confirmación previa en checkout y liquidaciones | Implementado |
| QR / acceso | Estado anunciado, tabla semántica y reingreso explicado | Implementado |
| Forced colors | Bordes/estados críticos protegidos | Implementado |

## Matriz NORTIC B2 / WCAG 2.0

### Nivel A

- [ ] 3.01.1.a — Contenido no textual: pasada manual final de `alt`/nombres accesibles.
- [x] 3.01.3.a — Información y relaciones: headings, labels, tablas y agrupaciones reforzados en flujos principales.
- [ ] 3.01.3.b — Secuencia significativa: validar lectura y DOM mediante recorrido manual.
- [x] 3.01.3.c — Características sensoriales: instrucciones críticas acompañadas de texto, no solo posición/color.
- [x] 3.01.4.a — Uso del color: estados, errores, finanzas, badges y QR reforzados con texto/borde.
- [ ] 3.02.1.a — Teclado: recorrido completo sin ratón pendiente.
- [ ] 3.02.1.b — Sin trampas para el foco: recorrido manual de dropdowns, diálogos, cámara QR y overlays pendiente.
- [ ] 3.02.2.a — Tiempo ajustable: revisar expiración de sesión/reservas y advertencias aplicables.
- [x] 3.02.2.b — Pausar/detener/ocultar: cámara QR dispone de activación/detención; no hay movimiento esencial.
- [x] 3.02.3.a — Destellos: Eventix no depende de contenido con destellos.
- [ ] 3.02.4.a — Evitar bloques: verificar mecanismo efectivo de salto en páginas completas.
- [x] 3.02.4.b — Titulado de páginas: fragmento de `<head>` compartido.
- [ ] 3.02.4.c — Orden de foco: recorrido manual completo pendiente.
- [ ] 3.02.4.d — Propósito de enlaces: pasada manual final de acciones genéricas/icon-only.
- [x] 3.03.1.a — Idioma de la página: `lang="es"`.
- [x] 3.03.2.a — Al recibir el foco: no se implementan cambios de contexto por foco en la capa compartida.
- [x] 3.03.2.b — Al recibir entradas: filtros relevantes requieren acción explícita o actualización controlada.
- [x] 3.03.3.a — Identificación de errores: flujos principales enlazan errores y campos.
- [x] 3.03.3.b — Etiquetas o instrucciones: formularios principales incluyen etiquetas/ayudas.
- [ ] 3.04.1.a — Procesamiento: validación final de HTML renderizado/IDs únicos pendiente.
- [x] 3.04.1.b — Nombre, función y valor: componentes críticos y QR reforzados con estados ARIA.

### Nivel AA

- [ ] 3.01.4.c — Contraste mínimo: capas AA implementadas; medición manual 4.5:1 / 3:1 pendiente.
- [ ] 3.01.4.d — Cambio de tamaño del texto: recorrido real al 200 % pendiente.
- [x] 3.01.4.e — Imágenes de texto: interfaz usa texto real salvo marca/contenido gráfico esencial.
- [ ] 3.02.4.e — Múltiples vías: validar navegación/búsqueda para páginas no lineales.
- [x] 3.02.4.f — Encabezados y etiquetas: reforzados en shell y flujos principales.
- [x] 3.02.4.g — Foco visible: capa global de foco de alto contraste.
- [ ] 3.03.1.b — Idioma de las partes: revisar contenido eventual en idioma distinto.
- [x] 3.03.2.c — Navegación coherente: shell compartido por rol y navegación pública consistente.
- [x] 3.03.2.d — Identificación coherente: acciones equivalentes usan nomenclatura consistente en flujos auditados.
- [x] 3.03.3.c — Sugerencias ante errores: ayudas y errores asociados en formularios principales.
- [x] 3.03.3.d — Prevención de errores financieros/datos: checkout, liquidaciones y acciones sensibles incluyen revisión/confirmación.

Los criterios multimedia se consideran **no aplicables mientras Eventix no publique audio/video tempodependiente**. Si se incorpora multimedia, debe reabrirse esa evaluación.

## Pruebas manuales obligatorias antes del cierre

1. Navegar cada flujo usando únicamente `Tab`, `Shift+Tab`, `Enter`, `Space` y flechas donde aplique.
2. Verificar foco visible y orden lógico.
3. Ejecutar todos los recorridos en claro, oscuro y sistema.
4. Aplicar zoom del navegador al 200 % y verificar ausencia de pérdida de contenido o función.
5. Medir contraste: mínimo 4.5:1 en texto normal y 3:1 en texto grande.
6. Ejecutar smoke test con lector de pantalla (NVDA recomendado en Windows) para navegación, formularios, tablas, alertas, checkout y control de acceso.
7. Validar errores de formularios: identificación, asociación al campo y sugerencia correctiva.
8. Validar operaciones financieras/destructivas con confirmación o posibilidad de revisión previa.
9. Revisar Home/Login, Dashboard por rol, eventos, checkout, ventas, liquidaciones, reportes, boletas/QR, control de acceso y páginas 400/403/404/405/500 en ambos temas.
10. Validar contenido a alto contraste/forced-colors cuando el entorno lo permita.

## Criterio de cierre

Eventix solo debe documentarse como **diseñado y validado internamente conforme a NORTIC B2:2017 / WCAG 2.0 Nivel AA** cuando todos los criterios aplicables restantes estén comprobados mediante la pasada manual y los hallazgos se hayan corregido.

Una auditoría interna satisfactoria no equivale a certificación oficial de OGTIC.

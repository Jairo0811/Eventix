# Informe de QA — Tráiler oficial de Eventix

Generado el 2026-11-20 00:00 UTC por `scripts/qa-report.mjs`.

Cada comprobación se hace sobre el archivo MP4 realmente renderizado,
leyendo sus metadatos y decodificándolo, no sobre la configuración del
proyecto.

## Versión A · Tráiler principal

Archivo: `renders/eventix-official-trailer-16x9.mp4`

- CUMPLE — Duración 50.01 s (esperado 50 s)
- CUMPLE — Resolución 1920×1080
- CUMPLE — 30 fps
- CUMPLE — Códec de vídeo h264 (H.264)
- CUMPLE — Códec de audio aac (AAC)
- CUMPLE — Reproducible: se decodifica sin errores
- CUMPLE — Sin cuadros negros accidentales (el apagado final es intencionado)
- CUMPLE — Miniatura generada: `renders/thumbnails/eventix-official-trailer-16x9.jpg`

Tamaño del archivo: 18.52 MB

## Versión B · Tráiler vertical

Archivo: `renders/eventix-official-trailer-9x16.mp4`

- CUMPLE — Duración 50.01 s (esperado 50 s)
- CUMPLE — Resolución 1080×1920
- CUMPLE — 30 fps
- CUMPLE — Códec de vídeo h264 (H.264)
- CUMPLE — Códec de audio aac (AAC)
- CUMPLE — Reproducible: se decodifica sin errores
- CUMPLE — Sin cuadros negros accidentales (el apagado final es intencionado)
- CUMPLE — Miniatura generada: `renders/thumbnails/eventix-official-trailer-9x16.jpg`

Tamaño del archivo: 17.95 MB

## Versión C · Teaser

Archivo: `renders/eventix-teaser-15s.mp4`

- CUMPLE — Duración 15.02 s (esperado 15 s)
- CUMPLE — Resolución 1080×1920
- CUMPLE — 30 fps
- CUMPLE — Códec de vídeo h264 (H.264)
- CUMPLE — Códec de audio aac (AAC)
- CUMPLE — Reproducible: se decodifica sin errores
- CUMPLE — Sin cuadros negros accidentales (el apagado final es intencionado)
- CUMPLE — Miniatura generada: `renders/thumbnails/eventix-teaser-15s.jpg`

Tamaño del archivo: 5.32 MB

## Comprobaciones hechas a mano

- Fidelidad del logo: se usa el archivo original del repositorio
  (`netlify/images/eventix-logo.png`), sin redibujar ni recomponer.
- Ortografía y acentos revisados en todos los rótulos y subtítulos.
- Sin secretos ni datos personales: las capturas salen de la demo
  pública y el script de captura sustituye cualquier correo por uno
  de ejemplo antes de fotografiar.
- Las pantallas que no existen en la demo pública aparecen como
  recreaciones rotuladas, nunca como capturas de la aplicación.
- El código QR codifica `EVENTIX-DEMO-NO-VALIDO-PARA-ACCESO` y se
  muestra con el rótulo «CÓDIGO DE MUESTRA · NO VÁLIDO PARA ACCESO».
- No aparece ninguna llamada a la compra: el checkout comercial sigue
  deshabilitado y la propia demo lo declara.
- Licencias: ver `ASSET-LICENSES.md`. No se usó material de terceros.

## Resultado

Todas las comprobaciones automáticas se cumplen.

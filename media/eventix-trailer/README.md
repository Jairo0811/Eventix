# Tráiler oficial de Eventix

Proyecto audiovisual que produce las tres piezas promocionales de Eventix a
partir de su código, su identidad visual y su demo pública.

Es **independiente del backend**: vive en `media/eventix-trailer/`, tiene su
propio `package.json` y no añade ninguna dependencia al build de Maven ni toca
el Docker de la aplicación.

## Entregables

| Pieza | Archivo | Formato | Uso |
|---|---|---|---|
| Tráiler principal | `renders/eventix-official-trailer-16x9.mp4` | 1920×1080, 30 fps, 50 s, H.264/AAC | LinkedIn, YouTube, portafolio |
| Tráiler vertical | `renders/eventix-official-trailer-9x16.mp4` | 1080×1920, 30 fps, 50 s | Reels, TikTok, Shorts |
| Teaser | `renders/eventix-teaser-15s.mp4` | 1080×1920, 30 fps, 15 s | Anuncio previo |

Acompañan miniaturas en `renders/thumbnails/`, subtítulos en `subtitles/` y el
informe de verificación en `renders/QA-REPORT.md`.

La versión vertical **no es un recorte** de la horizontal: cada escena recibe
`vertical` y se recompone por dentro —reencuadra, reordena y ajusta tipografía—
respetando las zonas seguras de Reels y TikTok.

## Requisitos

- Node.js 20 o superior.
- Un Chrome o Chromium en el sistema. Las órdenes usan
  `--browser-executable=/usr/bin/google-chrome`; cámbialo si el tuyo está en
  otra ruta.
- `ffmpeg` y `ffprobe` en el PATH, para el informe de QA.
- Python 3 para regenerar el audio.

## Cómo volver a renderizar

```bash
cd media/eventix-trailer
npm install

# 1. Capturas auténticas de la demo pública (opcional: ya están versionadas)
npm run capture

# 2. Banda sonora original (opcional: ya está versionada)
npm run audio

# 3. Las tres piezas
npm run render:all

# 4. Verificación sobre los MP4 realmente producidos
npm run qa
```

Para editar en vivo con vista previa:

```bash
npm run studio
```

> En máquinas con pocos núcleos, Remotion limita `--concurrency` al número de
> cores. Si una orden falla por eso, baja el valor o quita la bandera.

## Estructura

```text
media/eventix-trailer/
├── src/
│   ├── compositions/     Trailer (16:9 y 9:16) y Teaser
│   ├── scenes/           Las seis escenas del storyboard
│   ├── components/       Fondo, partículas, marcos, tipografía, marca
│   ├── animations/       Curvas y utilidades compartidas
│   └── theme/            Tokens de marca copiados del repositorio
├── assets/
│   ├── branding/         Logo, favicon y QR de demostración
│   ├── screenshots/
│   │   ├── real/         Capturas auténticas de la demo pública
│   │   └── recreations/  (las recreaciones son componentes, no imágenes)
│   └── audio/            Banda sonora original sintetizada
├── subtitles/            SRT en español
├── renders/              MP4, miniaturas e informe de QA
└── scripts/              Captura, audio y verificación
```

`assets/` hace de directorio público de Remotion (ver `remotion.config.ts`), de
modo que `staticFile('branding/eventix-logo.png')` resuelve ahí.

## Qué es real y qué es recreación

Esta distinción importa y está documentada en detalle en `ASSET-LICENSES.md`.

**Capturas reales**, tomadas de https://eventixrd.netlify.app: la portada
pública, la sección de exploración del catálogo y las vistas a 360, 390, 768,
1024 y 1440 px.

**Recreaciones**, implementadas como componentes en
`src/components/Recreations.tsx`: el panel de gestión, la boleta digital y el
control de acceso. Esas pantallas viven detrás de autenticación y requieren la
aplicación Spring Boot levantada, que no estaba disponible en el entorno donde
se produjo el tráiler. Están construidas con los tokens y la estructura reales
del repositorio y aparecen rotuladas en pantalla como «Vista de demostración».

Ninguna pieza presenta una función que no exista en el producto.

## Decisiones que conviene no deshacer sin pensarlo

- **El eslogan no se repite como texto en los cierres.** El logo oficial ya lo
  incorpora; duplicarlo lo mostraba dos veces en pantalla.
- **No se usa fotografía de terceros.** La escena 5 se construye con los
  propios activos del producto. Presentar imágenes de archivo como eventos
  organizados con Eventix sería falso.
- **No aparece ninguna llamada a la compra.** El checkout comercial sigue
  deshabilitado, y la propia demo lo declara.
- **El QR es de muestra.** Codifica `EVENTIX-DEMO-NO-VALIDO-PARA-ACCESO` y se
  rotula como no válido para acceso.
- **El panel no muestra cifras inventadas** de ingresos, ventas ni usuarios.

## Narración

Las piezas se entregan **instrumentales, con subtítulos**. No se incorporó voz
porque no había una herramienta de síntesis de voz con autorización de uso en el
entorno de producción, y el encargo pedía expresamente no inventar una entrega.

El guion está en `subtitles/` en formato SRT y sincronizado con las escenas, de
modo que incorporar narración más adelante es directo: basta grabar o sintetizar
las seis líneas y añadirlas como pista de audio en `src/compositions/Trailer.tsx`
junto a la música, ajustando el volumen de esta con la propiedad `volume` de
`<Audio>` para que la voz se entienda.

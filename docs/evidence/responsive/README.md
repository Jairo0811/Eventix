# Evidencia responsive

Capturas de la portada pública de Eventix en los cinco anchos de verificación.

**Origen:** https://eventixrd.netlify.app, capturada con Playwright mediante
`media/eventix-trailer/scripts/capture-demo.mjs` el 19 de noviembre de 2026.
Son la aplicación tal y como está publicada; no se retocaron ni se recompusieron.

| Ancho | Archivo | Desbordamiento horizontal |
|---|---|---|
| 360 px | `responsive-360.jpg` | 0 px |
| 390 px | `responsive-390.jpg` | 0 px |
| 768 px | `responsive-768.jpg` | 0 px |
| 1024 px | `responsive-1024.jpg` | 0 px |
| 1440 px | `responsive-1440.jpg` | 0 px |

Además, `event-dialog.jpg` muestra la sección de exploración del catálogo con
el evento de muestra.

## Qué comprueba esto

El script mide, en cada ancho,
`document.documentElement.scrollWidth - clientWidth`, que es el desbordamiento
horizontal: el defecto clásico en móvil, donde un elemento más ancho que la
ventana obliga a desplazarse de lado. **Dio 0 px en los cinco anchos.**

Las capturas se toman con las animaciones congeladas y a página completa, de
modo que son reproducibles: volver a ejecutar el script sobre la misma versión
publicada produce las mismas imágenes.

## Alcance, dicho con precisión

Esta evidencia cubre **la portada pública**, que es lo que la demo expone. No
cubre el panel de gestión, la boleta digital ni el control de acceso: esas
pantallas requieren autenticación y la aplicación Spring Boot levantada, lo que
no era posible en el entorno donde se generaron estas capturas.

Para completar la evidencia de esas vistas hace falta ejecutar la aplicación
y repetir la captura apuntando a `localhost`.

## Cómo regenerarla

```bash
cd media/eventix-trailer
npm install
npm run capture
```

El script escribe en `assets/screenshots/real/`; las imágenes de evidencia se
convirtieron a JPEG y se movieron aquí para no cargar el repositorio con PNG de
varios megabytes.

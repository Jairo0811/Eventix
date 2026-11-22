# Recursos y licencias

Este documento declara el origen de cada recurso del tráiler. La regla que se
siguió es simple: **no se usó ningún material de terceros**. Todo procede del
propio repositorio de Eventix, de su demo pública o se generó aquí.

## Marca

| Recurso | Origen | Licencia |
|---|---|---|
| `assets/branding/eventix-logo.png` | Copia exacta de `netlify/images/eventix-logo.png` del repositorio | Propiedad del proyecto Eventix |
| `assets/branding/eventix-favicon.png` | Copia exacta de `netlify/images/eventix-favicon.png` | Propiedad del proyecto Eventix |
| `assets/branding/eventix-tech-experience-2026.svg` | Copia exacta del recurso del repositorio | Propiedad del proyecto Eventix |

El logo **no se redibujó ni se recompuso**: se usa el archivo original, que ya
incorpora el eslogan oficial.

### Colores y tipografía

Los colores están copiados de las variables CSS reales del repositorio
(`src/main/resources/static/css/*.css`, bloque `--eventix-*`, valores del tema
oscuro) y se declaran en `src/theme/tokens.ts`. La tipografía es la misma pila
que usa la aplicación: Inter con alternativas del sistema. No se descargó
ninguna fuente de terceros, de modo que el render no depende de un servicio
externo ni arrastra una licencia ajena.

## Capturas reales

Carpeta `assets/screenshots/real/`. Obtenidas con Playwright contra la demo
pública **https://eventixrd.netlify.app**, mediante `scripts/capture-demo.mjs`.
Son la aplicación tal y como está publicada; no se retocaron.

| Archivo | Contenido |
|---|---|
| `home-hero.png` | Portada pública, zona superior |
| `home-explore.png` | Sección de exploración del catálogo |
| `home-full.png` | Portada completa, usada para los movimientos de cámara |
| `event-dialog.png` | Detalle del evento de muestra |
| `responsive-360/390/768/1024/1440.png` | La misma portada en los cinco anchos de verificación |

El script oculta correos y limpia campos con aspecto de credencial antes de
fotografiar, y congela animaciones para que cada captura sea reproducible.

Todo el contenido visible en estas capturas es ficticio por diseño: la propia
demo lo rotula como «Catálogo de muestra · sin ventas reales» y «Sitio en
preparación · Vista de demostración, sin compras ni cuentas habilitadas».

## Recreaciones visuales

Implementadas en `src/components/Recreations.tsx`. **No son capturas de la
aplicación en ejecución.**

| Pantalla | Por qué es recreación |
|---|---|
| Panel de gestión | Requiere autenticación; no está en la demo pública |
| Boleta digital | Requiere sesión iniciada y una compra |
| Control de acceso | Requiere sesión y una boleta emitida |

Están construidas con los tokens de color, la tipografía y la estructura reales
del repositorio, y aparecen rotuladas en pantalla como «Vista de demostración».
No representan ninguna función inexistente.

El panel **no muestra cifras de ingresos, ventas ni usuarios**: los indicadores
usan etiquetas descriptivas y barras ilustrativas en lugar de números
inventados.

## Código QR

`assets/branding/qr-demo.png`, generado con la biblioteca `qrcode` (MIT) en el
propio proyecto. Codifica la cadena:

```
EVENTIX-DEMO-NO-VALIDO-PARA-ACCESO
```

Aparece siempre acompañado del rótulo «CÓDIGO DE MUESTRA · NO VÁLIDO PARA
ACCESO». No es una credencial válida ni corresponde a ninguna boleta real.

## Audio

`assets/audio/trailer-50s.wav` y `assets/audio/teaser-15s.wav`.

**Compuestos y sintetizados íntegramente en `scripts/make-audio.py`**, muestra a
muestra, sin grabaciones, canciones ni bibliotecas de efectos de terceros. La
pieza es electrónica cinematográfica en La menor, con progresión emocional
alineada a las seis escenas, e incluye los efectos pedidos: barridos (whoosh),
impactos digitales, pulsaciones de interfaz y risers.

Al ser original y generada por código, puede publicarse en redes y portafolio
sin restricciones de licencia, y puede reproducirse ejecutando el script.

## Material que deliberadamente NO se usó

El storyboard sugería planos de conciertos, conferencias y público. **No se
incluyó ninguna fotografía ni vídeo de terceros.** No había material con
licencia comercial comprobada disponible en el entorno de producción, y
presentar imágenes de archivo como eventos organizados mediante Eventix habría
sido falso. La escena 5 se resolvió con un montaje construido con los propios
activos del producto.

## Dependencias de software

El proyecto audiovisual es independiente del backend Java: vive en
`media/eventix-trailer/`, tiene su propio `package.json` y **no añade ninguna
dependencia al build de Maven**.

| Paquete | Licencia |
|---|---|
| remotion, @remotion/cli, @remotion/renderer | Remotion License (gratuita para individuos y empresas pequeñas; revisar si la empresa supera el umbral) |
| react, react-dom | MIT |
| playwright | Apache-2.0 |
| qrcode | MIT |
| typescript | Apache-2.0 |

> **Nota sobre Remotion:** su licencia es gratuita para individuos y para
> empresas de hasta tres personas, pero requiere licencia de empresa por encima
> de ese tamaño. Conviene revisarlo antes de un uso comercial amplio:
> https://remotion.dev/license

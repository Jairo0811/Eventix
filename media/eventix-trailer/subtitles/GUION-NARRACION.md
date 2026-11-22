# Guion de narración — Tráiler oficial de Eventix

Guion definitivo en español, listo para grabar o sintetizar.

**Estado:** las piezas se entregaron **instrumentales**. No se incorporó voz
porque no había una herramienta de síntesis con autorización de uso en el
entorno de producción. Este guion y los SRT quedan sincronizados para que
añadirla después sea directo.

## Voz

- Masculina, profesional.
- Acento latinoamericano natural, compatible con el público dominicano.
- Tono tecnológico, inspirador y seguro. Nada enfático de más.
- Velocidad moderada: las líneas tienen aire de sobra en su ventana.
- Sin pronunciación robótica ni entonación de locutor de anuncio antiguo.

## Tráiler principal (50 s)

| # | Entra | Sale | Línea |
|---|---|---|---|
| 1 | 00:01,2 | 00:07,6 | Cada gran experiencia comienza con un momento. |
| 2 | 00:08,4 | 00:16,6 | Descubre eventos, nuevas oportunidades y experiencias que conectan personas. |
| 3 | 00:17,4 | 00:26,6 | Organiza cada detalle desde una plataforma diseñada para simplificar la gestión. |
| 4 | 00:27,4 | 00:36,6 | Desde tu entrada digital hasta el acceso al evento, todo conectado. |
| 5 | 00:37,4 | 00:43,6 | Una plataforma para transformar la manera en que vivimos los eventos. |
| 6 | 00:44,4 | 00:48,8 | Eventix. Transforma tus eventos. Conecta experiencias. |

Cada línea empieza después del barrido de luz que abre su escena, de modo que
la voz nunca compite con una transición.

## Teaser (15 s)

| # | Entra | Sale | Línea |
|---|---|---|---|
| 1 | 00:00,4 | 00:02,8 | Eventix. |
| 2 | 00:03,2 | 00:06,8 | Descubre eventos y experiencias que conectan personas. |
| 3 | 00:07,2 | 00:10,8 | Tu entrada digital, tu código QR y el control de acceso. |
| 4 | 00:11,2 | 00:14,6 | El futuro de tus eventos comienza aquí. |

## Cómo incorporar la voz

1. Grabar o sintetizar las líneas respetando las marcas de tiempo. Conviene un
   único archivo por pieza, con silencio en los huecos, en lugar de seis
   archivos sueltos: evita desajustes acumulados.
2. Guardarlos como `assets/audio/trailer-voz.wav` y `assets/audio/teaser-voz.wav`.
3. Añadir la pista en la composición correspondiente, junto a la música, y
   bajar el volumen de esta para que la voz se entienda:

```tsx
// src/compositions/Trailer.tsx
<Audio src={staticFile('audio/trailer-50s.wav')} volume={0.45} />
<Audio src={staticFile('audio/trailer-voz.wav')} />
```

   `0.45` es un punto de partida razonable: la música quedaría unos 7 dB por
   debajo. Conviene ajustarlo escuchando, no a ojo.

4. Volver a renderizar con `npm run render:all` y verificar con `npm run qa`.

## Qué no decir

Estas restricciones no son de estilo, son de exactitud. El guion ya las
respeta y conviene no romperlas al reescribirlo:

- Ninguna llamada a la compra. El checkout comercial sigue deshabilitado.
- No afirmar que los pagos bancarios reales están habilitados.
- No atribuir cifras de ventas, ingresos ni usuarios.
- No presentar como organizados con Eventix eventos que no lo fueron.

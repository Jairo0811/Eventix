import React from 'react';
import {AbsoluteFill, Img, interpolate, staticFile, useCurrentFrame} from 'remotion';
import {Backdrop} from '../components/Backdrop';
import {Particles} from '../components/Particles';
import {LightSweep} from '../components/Brand';
import {BrandText, MaskedTitle} from '../components/Typography';
import {COLORS, FONT_STACK} from '../theme/tokens';
import {CINEMATIC} from '../animations/easing';

/**
 * ESCENA 5 — CONECTA EXPERIENCIAS (00:37-00:44)
 *
 * Montaje rápido pero elegante.
 *
 * Decisión deliberada sobre el material: el storyboard sugería planos de
 * conciertos y público. No se usa NINGUNA fotografía de terceros, porque no
 * había material con licencia comercial comprobada disponible en el entorno de
 * producción, y presentar imágenes de archivo como eventos organizados con
 * Eventix habría sido falso. El montaje se construye con los propios activos
 * del producto: capturas reales de la demo, la portada ilustrativa del evento
 * de muestra que vive en el repositorio y tratamiento gráfico de marca.
 */
export const Scene05Conecta: React.FC<{vertical?: boolean}> = ({vertical = false}) => {
    const frame = useCurrentFrame();

    /** Cada pieza del mosaico entra con un pequeño desfase. */
    const tile = (delay: number) => {
        const progress = interpolate(frame, [delay, delay + 20], [0, 1], {
            extrapolateLeft: 'clamp',
            extrapolateRight: 'clamp',
            easing: CINEMATIC,
        });
        const drift = interpolate(frame, [delay, 210], [0, -26], {
            extrapolateLeft: 'clamp',
            extrapolateRight: 'clamp',
        });
        return {
            opacity: progress,
            transform: `translateY(${(1 - progress) * 54 + drift}px) scale(${
                0.94 + progress * 0.06
            })`,
        };
    };

    const cardStyle: React.CSSProperties = {
        borderRadius: 18,
        overflow: 'hidden',
        border: `1px solid ${COLORS.border}`,
        boxShadow: '0 30px 80px rgba(0,0,0,0.55)',
        backgroundColor: COLORS.elevated,
    };

    /**
     * Cada pieza encuadra una zona DISTINTA del producto. Sin `offsetY` las
     * tres mostraban la misma cabecera y el mosaico parecía repetido; la
     * columna móvil además necesita menos ancho y más escala para que su
     * contenido se lea en lugar de cortarse a media palabra.
     */
    const pieces = [
        {
            src: 'screenshots/real/home-hero.png',
            delay: 4,
            label: 'Descubrimiento',
            offsetY: 0,
            zoom: 1.04,
            flex: 1.18,
        },
        {
            src: 'screenshots/real/home-full.png',
            delay: 14,
            label: 'Catálogo',
            offsetY: vertical ? 1180 : 1010,
            zoom: 1.04,
            flex: 1.18,
        },
        // En vertical la propia pieza ya es móvil, y una captura de 390 px
        // estirada al ancho completo se veía vacía y blanda. Se muestra en su
        // lugar la tercera sección real de la página: la de organizadores.
        vertical
            ? {
                  src: 'screenshots/real/home-full.png',
                  delay: 24,
                  label: 'Organizadores',
                  offsetY: 1760,
                  zoom: 1.0,
                  flex: 1,
              }
            : {
                  src: 'screenshots/real/responsive-390.png',
                  delay: 24,
                  label: 'Móvil',
                  offsetY: 150,
                  zoom: 1.0,
                  flex: 0.64,
              },
    ];

    /**
     * En vertical el mosaico se apila. Puestas en fila dentro de 1080 px, las
     * tres piezas quedaban reducidas hasta resultar ilegibles, que es
     * justamente lo que hay que evitar en este formato.
     */
    const mosaic = (
        <div
            style={{
                display: 'flex',
                flexDirection: vertical ? 'column' : 'row',
                gap: vertical ? 26 : 30,
                alignItems: 'stretch',
                justifyContent: 'center',
                width: '100%',
            }}
        >
            {pieces.map((piece) => (
                <div
                    key={piece.src}
                    style={{
                        ...cardStyle,
                        ...tile(piece.delay),
                        flex: vertical ? 'none' : piece.flex,
                        width: vertical ? '100%' : undefined,
                        height: vertical ? 330 : 440,
                        position: 'relative',
                    }}
                >
                    <Img
                        src={staticFile(piece.src)}
                        style={{
                            width: '100%',
                            display: 'block',
                            transform: `translateY(${-piece.offsetY}px) scale(${piece.zoom})`,
                            transformOrigin: 'top center',
                        }}
                    />
                    <div
                        style={{
                            position: 'absolute',
                            inset: 0,
                            // El degradado debe cerrar casi opaco y empezar
                            // antes: con 0.92 el rótulo se pisaba con el texto
                            // de la página que queda debajo.
                            background:
                                'linear-gradient(180deg, rgba(8,17,24,0) 34%, rgba(6,25,35,0.80) 72%, rgba(4,16,22,0.99) 100%)',
                        }}
                    />
                    <div
                        style={{
                            position: 'absolute',
                            left: 18,
                            bottom: 16,
                            fontFamily: FONT_STACK,
                            fontSize: 21,
                            fontWeight: 700,
                            letterSpacing: '0.08em',
                            textTransform: 'uppercase',
                            color: COLORS.primary,
                        }}
                    >
                        {piece.label}
                    </div>
                </div>
            ))}
        </div>
    );

    return (
        <AbsoluteFill>
            <Backdrop glow={1} glowY="50%">
                <Particles count={vertical ? 46 : 40} intensity={0.66} />

                <AbsoluteFill
                    style={{
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        padding: vertical ? '160px 58px' : '0 120px',
                        gap: vertical ? 64 : 54,
                    }}
                >
                    <MaskedTitle start={38} size={vertical ? 72 : 84} align="center">
                        UNA PLATAFORMA.
                        <br />
                        <BrandText>INFINITAS EXPERIENCIAS.</BrandText>
                    </MaskedTitle>

                    {mosaic}
                </AbsoluteFill>

                {/* Pulso luminoso que crece hacia el cierre */}
                <AbsoluteFill
                    style={{
                        background: `radial-gradient(60% 50% at 50% 50%,
                            rgba(190,242,100,${interpolate(frame, [150, 210], [0, 0.2], {
                                extrapolateLeft: 'clamp',
                                extrapolateRight: 'clamp',
                            })}) 0%,
                            rgba(8,17,24,0) 72%)`,
                        pointerEvents: 'none',
                    }}
                />

                <LightSweep start={184} duration={30} />
            </Backdrop>
        </AbsoluteFill>
    );
};

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

    const pieces = [
        {src: 'screenshots/real/home-hero.png', delay: 4, label: 'Descubrimiento'},
        {src: 'screenshots/real/home-explore.png', delay: 14, label: 'Catálogo'},
        {src: 'screenshots/real/responsive-390.png', delay: 24, label: 'Móvil'},
    ];

    const mosaic = (
        <div
            style={{
                display: 'flex',
                gap: vertical ? 22 : 30,
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
                        flex: 1,
                        height: vertical ? 520 : 440,
                        position: 'relative',
                    }}
                >
                    <Img
                        src={staticFile(piece.src)}
                        style={{
                            width: '100%',
                            display: 'block',
                            transform: 'scale(1.06)',
                            transformOrigin: 'top center',
                        }}
                    />
                    <div
                        style={{
                            position: 'absolute',
                            inset: 0,
                            background:
                                'linear-gradient(180deg, rgba(8,17,24,0) 44%, rgba(6,25,35,0.92) 100%)',
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

import React from 'react';
import {AbsoluteFill, interpolate, useCurrentFrame} from 'remotion';
import {Backdrop} from '../components/Backdrop';
import {Particles} from '../components/Particles';
import {Logo, LightSweep} from '../components/Brand';
import {COLORS, FONT_STACK} from '../theme/tokens';
import {CINEMATIC, fadeOut} from '../animations/easing';

/**
 * ESCENA 1 — EL INICIO (00:00-00:08)
 *
 * Oscuridad, una luz verde que nace en el centro, partículas turquesas que
 * convergen y forman el logo. Cierra con el barrido de luz hacia la escena 2.
 */
export const Scene01Inicio: React.FC<{vertical?: boolean}> = ({vertical = false}) => {
    const frame = useCurrentFrame();

    // La luz nace de un punto y se abre. Arranca en el fotograma 0 con algo de
    // brillo: un inicio completamente negro se convierte en la miniatura del
    // vídeo en redes y parece que está roto.
    const spark = interpolate(frame, [0, 34], [0.16, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });
    // Las partículas convergen hacia el centro justo antes de formarse el logo.
    const converge = interpolate(frame, [40, 128], [0, 0.82], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });
    // y se dispersan cuando el logo ya está.
    const disperse = interpolate(frame, [132, 186], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });

    const logoWidth = vertical ? 760 : 820;
    const titleSize = vertical ? 46 : 42;

    return (
        <AbsoluteFill>
            <Backdrop glow={spark} grid={spark > 0.5}>
                <Particles
                    count={vertical ? 54 : 46}
                    intensity={spark * (1 - disperse * 0.45)}
                    converge={Math.max(0, converge - disperse * 0.9)}
                />

                {/* Punto de luz inicial */}
                <AbsoluteFill
                    style={{
                        background: `radial-gradient(${18 + spark * 46}% ${18 + spark * 40}% at 50% 46%,
                            rgba(190,242,100,${0.5 * spark}) 0%,
                            rgba(74,222,128,${0.26 * spark}) 38%,
                            rgba(8,17,24,0) 72%)`,
                        opacity: 1 - disperse * 0.55,
                    }}
                />

                <AbsoluteFill
                    style={{
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexDirection: 'column',
                        gap: vertical ? 54 : 42,
                        padding: vertical ? '0 70px' : '0 120px',
                    }}
                >
                    <Logo width={logoWidth} start={108} glow={1} />

                    <div
                        style={{
                            fontFamily: FONT_STACK,
                            fontSize: titleSize,
                            fontWeight: 700,
                            letterSpacing: '0.16em',
                            textAlign: 'center',
                            color: COLORS.muted,
                            textTransform: 'uppercase',
                            lineHeight: 1.5,
                            opacity: interpolate(frame, [150, 178], [0, 1], {
                                extrapolateLeft: 'clamp',
                                extrapolateRight: 'clamp',
                                easing: CINEMATIC,
                            }),
                            maxWidth: vertical ? 880 : 1180,
                        }}
                    >
                        Cada evento comienza con una experiencia.
                    </div>
                </AbsoluteFill>

                {/* Salida: la escena se oscurece mientras entra el barrido.
                    El velo se queda en 0.6 a propósito: llevándolo a 1 el paso
                    a la escena 2 producía seis fotogramas completamente negros,
                    que es un corte accidental, no una transición. */}
                <AbsoluteFill
                    style={{
                        backgroundColor: COLORS.background,
                        opacity: (1 - fadeOut(frame, 214, 20)) * 0.6,
                        pointerEvents: 'none',
                    }}
                />
                <LightSweep start={206} duration={30} />
            </Backdrop>
        </AbsoluteFill>
    );
};

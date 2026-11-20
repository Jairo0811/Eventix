import React from 'react';
import {AbsoluteFill, interpolate, useCurrentFrame} from 'remotion';
import {Backdrop} from '../components/Backdrop';
import {Particles} from '../components/Particles';
import {DemoNote, Logo} from '../components/Brand';
import {COLORS} from '../theme/tokens';
import {CINEMATIC} from '../animations/easing';

/**
 * ESCENA 6 — CIERRE OFICIAL (00:44-00:50)
 *
 * Fondo negro azulado, el logo al centro con halo verde y turquesa, el eslogan
 * oficial y la dirección de la demo con discreción. Termina con un apagado de
 * luz elegante.
 *
 * No aparece ninguna llamada a la compra: el checkout comercial sigue
 * deshabilitado, y la propia demo lo declara.
 */
export const Scene06Cierre: React.FC<{vertical?: boolean}> = ({vertical = false}) => {
    const frame = useCurrentFrame();

    // Apagado final: la luz se retira y el encuadre se cierra en negro.
    const blackout = interpolate(frame, [138, 178], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });
    const glow = interpolate(frame, [0, 40, 130, 172], [0.2, 1, 0.95, 0], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });

    const tagline = interpolate(frame, [44, 72], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });

    return (
        <AbsoluteFill style={{backgroundColor: COLORS.deep}}>
            <Backdrop glow={glow} glowY="44%" grid={false}>
                <Particles count={28} intensity={glow * 0.7} />

                <AbsoluteFill
                    style={{
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: vertical ? 52 : 42,
                        padding: vertical ? '0 70px' : '0 140px',
                    }}
                >
                    {/* El logo oficial ya incorpora el eslogan. Repetirlo debajo
                        como texto lo duplicaba en pantalla, así que se deja que
                        lo lleve la marca y aquí solo se subraya con una línea
                        luminosa que se despliega. */}
                    <Logo width={vertical ? 900 : 1020} start={2} glow={glow} />

                    <div
                        style={{
                            width: (vertical ? 900 : 1020) * 0.82 * tagline,
                            height: 3,
                            borderRadius: 999,
                            background: `linear-gradient(90deg,
                                rgba(45,212,191,0) 0%, ${COLORS.secondary} 22%,
                                ${COLORS.primary} 58%, ${COLORS.accent} 82%,
                                rgba(190,242,100,0) 100%)`,
                            opacity: tagline * glow,
                        }}
                    />

                    <div style={{marginTop: vertical ? 26 : 20}}>
                        <DemoNote start={88} size={vertical ? 26 : 24} />
                    </div>
                </AbsoluteFill>
            </Backdrop>

            {/* Apagado de luz */}
            <AbsoluteFill
                style={{
                    backgroundColor: '#02070a',
                    opacity: blackout,
                    pointerEvents: 'none',
                }}
            />
        </AbsoluteFill>
    );
};

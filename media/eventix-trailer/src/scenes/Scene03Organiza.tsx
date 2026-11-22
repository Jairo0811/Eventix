import React from 'react';
import {AbsoluteFill, interpolate, useCurrentFrame} from 'remotion';
import {Backdrop} from '../components/Backdrop';
import {Particles} from '../components/Particles';
import {LightSweep} from '../components/Brand';
import {DashboardPanel} from '../components/Recreations';
import {BrandText, Eyebrow, MaskedTitle, Support} from '../components/Typography';
import {CINEMATIC} from '../animations/easing';

/**
 * ESCENA 3 — ORGANIZA (00:17-00:27)
 *
 * El área de gestión, con movimiento 2.5D: el panel se inclina levemente y sus
 * capas entran escalonadas, lo que da profundidad sin alterar la información.
 *
 * El panel es una RECREACIÓN basada en la implementación del repositorio: esa
 * zona exige autenticación y la aplicación no podía levantarse en el entorno de
 * producción del tráiler. No muestra cifras de ingresos, ventas ni usuarios.
 */
export const Scene03Organiza: React.FC<{vertical?: boolean}> = ({vertical = false}) => {
    const frame = useCurrentFrame();

    // Inclinación 2.5D que se endereza: el plano "se presenta" al espectador.
    const settle = interpolate(frame, [0, 90], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });
    const tilt = (1 - settle) * (vertical ? 13 : 16);
    const drift = interpolate(frame, [0, 300], [0, vertical ? -34 : -52], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
    });
    const scale = interpolate(frame, [0, 300], [0.97, 1.05], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
    });

    const panel = (width: number) => (
        <div
            style={{
                perspective: 1800,
                transform: `translateY(${drift}px)`,
            }}
        >
            <div
                style={{
                    transform: `rotateY(${tilt}deg) rotateX(${tilt * 0.32}deg) scale(${scale})`,
                    transformStyle: 'preserve-3d',
                }}
            >
                <DashboardPanel width={width} start={6} />
            </div>
        </div>
    );

    if (vertical) {
        return (
            <AbsoluteFill>
                <Backdrop glow={0.82} glowY="38%">
                    <Particles count={30} intensity={0.42} />
                    <AbsoluteFill
                        style={{
                            flexDirection: 'column',
                            alignItems: 'center',
                            justifyContent: 'center',
                            padding: '150px 56px',
                            gap: 56,
                        }}
                    >
                        <div style={{display: 'flex', flexDirection: 'column', gap: 20, alignItems: 'center'}}>
                            <Eyebrow start={8}>Organiza</Eyebrow>
                            <MaskedTitle start={14} size={76} align="center">
                                ORGANIZA CON
                                <br />
                                <BrandText>INTELIGENCIA.</BrandText>
                            </MaskedTitle>
                        </div>
                        {panel(950)}
                        <Support start={54} size={31} maxWidth={820}>
                            Publica eventos, controla el aforo y sigue cada reservación desde un
                            solo lugar.
                        </Support>
                    </AbsoluteFill>
                    <LightSweep start={276} duration={28} direction="left" />
                </Backdrop>
            </AbsoluteFill>
        );
    }

    return (
        <AbsoluteFill>
            <Backdrop glow={0.88} glowX="34%" glowY="46%">
                <Particles count={28} intensity={0.4} />

                <AbsoluteFill
                    style={{
                        flexDirection: 'row-reverse',
                        alignItems: 'center',
                        padding: '0 104px',
                        gap: 66,
                    }}
                >
                    <div
                        style={{
                            width: 660,
                            display: 'flex',
                            flexDirection: 'column',
                            gap: 26,
                            flexShrink: 0,
                        }}
                    >
                        <Eyebrow start={6} align="left">
                            Organiza
                        </Eyebrow>
                        {/* 70 px: a 88 la palabra «INTELIGENCIA.» no cabía en
                            la columna y la máscara del título la recortaba. */}
                        <MaskedTitle start={12} size={70} align="left">
                            ORGANIZA CON
                            <br />
                            <BrandText>INTELIGENCIA.</BrandText>
                        </MaskedTitle>
                        <Support start={40} align="left" maxWidth={540}>
                            Publica eventos, controla el aforo, gestiona asistentes y sigue cada
                            reservación desde una sola plataforma.
                        </Support>
                    </div>

                    <div style={{flex: 1, display: 'flex', justifyContent: 'center'}}>
                        {panel(1120)}
                    </div>
                </AbsoluteFill>

                <LightSweep start={276} duration={28} direction="left" />
            </Backdrop>
        </AbsoluteFill>
    );
};

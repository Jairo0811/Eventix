import React from 'react';
import {AbsoluteFill, interpolate, useCurrentFrame} from 'remotion';
import {Backdrop} from '../components/Backdrop';
import {Particles} from '../components/Particles';
import {LightSweep} from '../components/Brand';
import {BrowserFrame} from '../components/Frames';
import {BrandText, Eyebrow, MaskedTitle, Support} from '../components/Typography';
import {COLORS, FONT_STACK} from '../theme/tokens';
import {CINEMATIC, kenBurns} from '../animations/easing';

/**
 * ESCENA 2 — DESCUBRE (00:08-00:17)
 *
 * El home real de Eventix, capturado de la demo pública, dentro de una
 * composición cinematográfica. La cámara recorre la interfaz y se destacan el
 * buscador, las categorías y el evento de muestra.
 *
 * Las capturas son auténticas: assets/screenshots/real/home-*.png.
 * No se simula ninguna compra.
 */
export const Scene02Descubre: React.FC<{vertical?: boolean}> = ({vertical = false}) => {
    const frame = useCurrentFrame();

    // Movimiento lento sobre la captura: desciende del hero a la exploración.
    const camera = kenBurns(frame, 270, {
        fromScale: 1.0,
        toScale: 1.1,
        fromY: 0,
        toY: vertical ? -120 : -70,
    });

    const chips = ['Hoy', 'Este fin de semana', 'Este mes', 'Ver catálogo'];

    if (vertical) {
        // En vertical la pantalla ocupa el centro y el texto va arriba y abajo,
        // en lugar de reducir la web entera hasta volverla ilegible.
        return (
            <AbsoluteFill>
                <Backdrop glow={0.85} glowY="34%">
                    <Particles count={34} intensity={0.5} />
                    <AbsoluteFill
                        style={{
                            flexDirection: 'column',
                            alignItems: 'center',
                            justifyContent: 'center',
                            padding: '150px 62px',
                            gap: 52,
                        }}
                    >
                        <div style={{display: 'flex', flexDirection: 'column', gap: 20, alignItems: 'center'}}>
                            <Eyebrow start={8}>Descubre</Eyebrow>
                            <MaskedTitle start={14} size={84} align="center">
                                DESCUBRE
                                <br />
                                <BrandText>EVENTOS.</BrandText>
                            </MaskedTitle>
                        </div>

                        <div style={{transform: camera}}>
                            <BrowserFrame
                                src="screenshots/real/home-hero.png"
                                width={880}
                                height={700}
                                offsetY={120}
                            />
                        </div>

                        <Support start={44} size={32} maxWidth={820}>
                            Busca por evento, artista o lugar y filtra por fecha o categoría.
                        </Support>
                    </AbsoluteFill>
                    <LightSweep start={246} duration={28} />
                </Backdrop>
            </AbsoluteFill>
        );
    }

    return (
        <AbsoluteFill>
            <Backdrop glow={0.9} glowX="68%" glowY="42%">
                <Particles count={30} intensity={0.45} />

                <AbsoluteFill
                    style={{
                        flexDirection: 'row',
                        alignItems: 'center',
                        padding: '0 108px',
                        gap: 70,
                    }}
                >
                    {/* Columna de texto */}
                    <div
                        style={{
                            width: 620,
                            display: 'flex',
                            flexDirection: 'column',
                            gap: 26,
                            flexShrink: 0,
                        }}
                    >
                        <Eyebrow start={6} align="left">
                            Descubre
                        </Eyebrow>
                        <MaskedTitle start={12} size={96} align="left">
                            DESCUBRE
                            <br />
                            <BrandText>EVENTOS.</BrandText>
                        </MaskedTitle>
                        <Support start={38} align="left" maxWidth={560}>
                            Conciertos, conferencias y experiencias. Busca por evento, artista o
                            lugar y filtra por fecha o categoría.
                        </Support>

                        {/* Las categorías rápidas aparecen escalonadas */}
                        <div style={{display: 'flex', flexWrap: 'wrap', gap: 12, marginTop: 10}}>
                            {chips.map((chip, index) => {
                                const appear = interpolate(
                                    frame,
                                    [62 + index * 11, 62 + index * 11 + 18],
                                    [0, 1],
                                    {
                                        extrapolateLeft: 'clamp',
                                        extrapolateRight: 'clamp',
                                        easing: CINEMATIC,
                                    }
                                );
                                const highlight = index === chips.length - 1;
                                return (
                                    <span
                                        key={chip}
                                        style={{
                                            fontFamily: FONT_STACK,
                                            fontSize: 24,
                                            fontWeight: 600,
                                            padding: '12px 24px',
                                            borderRadius: 999,
                                            color: highlight ? COLORS.primary : COLORS.text,
                                            border: `1px solid ${
                                                highlight ? COLORS.primary : COLORS.border
                                            }`,
                                            backgroundColor: highlight
                                                ? 'rgba(74,222,128,0.1)'
                                                : 'rgba(19,35,45,0.72)',
                                            opacity: appear,
                                            transform: `translateY(${(1 - appear) * 14}px)`,
                                        }}
                                    >
                                        {chip}
                                    </span>
                                );
                            })}
                        </div>
                    </div>

                    {/* La interfaz real, con movimiento de cámara */}
                    <div style={{flex: 1, display: 'flex', justifyContent: 'center'}}>
                        <div style={{transform: camera, transformOrigin: 'center top'}}>
                            <BrowserFrame
                                src="screenshots/real/home-full.png"
                                width={1060}
                                height={720}
                                offsetY={60}
                            />
                        </div>
                    </div>
                </AbsoluteFill>

                <LightSweep start={246} duration={28} />
            </Backdrop>
        </AbsoluteFill>
    );
};

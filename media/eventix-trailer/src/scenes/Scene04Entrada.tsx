import React from 'react';
import {AbsoluteFill, interpolate, useCurrentFrame} from 'remotion';
import {Backdrop} from '../components/Backdrop';
import {Particles} from '../components/Particles';
import {LightSweep} from '../components/Brand';
import {PhoneFrame} from '../components/Frames';
import {AccessCheck, DigitalTicket} from '../components/Recreations';
import {BrandText, Eyebrow, MaskedTitle, Support} from '../components/Typography';
import {COLORS} from '../theme/tokens';
import {CINEMATIC} from '../animations/easing';

/**
 * ESCENA 4 — TU ENTRADA DIGITAL (00:27-00:37)
 *
 * Un teléfono con la boleta digital, acercamiento al QR y paso al control de
 * acceso con el indicador de entrada válida.
 *
 * La boleta y el control son RECREACIONES: ambas pantallas requieren sesión
 * iniciada. El QR codifica EVENTIX-DEMO-NO-VALIDO-PARA-ACCESO y aparece
 * rotulado como muestra, de modo que no representa una credencial real, y el
 * acceso verificado se identifica como escenario de prueba.
 */
export const Scene04Entrada: React.FC<{vertical?: boolean}> = ({vertical = false}) => {
    const frame = useCurrentFrame();

    // Acercamiento progresivo al código QR.
    const zoom = interpolate(frame, [80, 170], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });
    // El teléfono se desplaza a un lado cuando entra el control de acceso.
    const shift = interpolate(frame, [186, 232], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });

    // En horizontal el teléfono debe caber entero en 1080 px de alto: a 2.02 de
    // proporción, 460 de ancho deja margen suficiente arriba y abajo.
    const phoneWidth = vertical ? 620 : 460;
    const phoneHeight = phoneWidth * 2.02;

    const phone = (
        <div
            style={{
                transform: `scale(${1 + zoom * 0.1}) translateX(${
                    shift * (vertical ? -70 : -130)
                }px)`,
            }}
        >
            <PhoneFrame width={phoneWidth} height={phoneHeight}>
                <div
                    style={{
                        position: 'absolute',
                        inset: 0,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        background: `radial-gradient(70% 50% at 50% 42%,
                            rgba(45,212,191,0.16) 0%, rgba(8,17,24,0) 72%)`,
                        padding: phoneWidth * 0.06,
                    }}
                >
                    <DigitalTicket
                        width={phoneWidth * 0.86}
                        start={16}
                        zoomToQr={zoom}
                    />
                </div>
            </PhoneFrame>
        </div>
    );

    const access = (
        <div
            style={{
                opacity: shift,
                transform: `translateY(${(1 - shift) * 40}px)`,
            }}
        >
            <AccessCheck width={vertical ? 760 : 560} start={188} />
        </div>
    );

    if (vertical) {
        return (
            <AbsoluteFill>
                <Backdrop glow={0.9} glowY="42%">
                    <Particles count={32} intensity={0.44} />
                    <AbsoluteFill
                        style={{
                            flexDirection: 'column',
                            alignItems: 'center',
                            justifyContent: 'center',
                            padding: '140px 56px',
                            gap: 44,
                        }}
                    >
                        <div style={{display: 'flex', flexDirection: 'column', gap: 18, alignItems: 'center'}}>
                            <Eyebrow start={8}>Tu entrada</Eyebrow>
                            <MaskedTitle start={14} size={76} align="center">
                                TU ENTRADA.
                                <br />
                                <BrandText>TU EXPERIENCIA.</BrandText>
                            </MaskedTitle>
                        </div>
                        {phone}
                        {access}
                    </AbsoluteFill>
                    <LightSweep start={276} duration={26} />
                </Backdrop>
            </AbsoluteFill>
        );
    }

    return (
        <AbsoluteFill>
            <Backdrop glow={0.92} glowX="58%" glowY="48%">
                <Particles count={30} intensity={0.44} />

                <AbsoluteFill
                    style={{
                        flexDirection: 'row',
                        alignItems: 'center',
                        padding: '0 120px',
                        gap: 80,
                    }}
                >
                    <div
                        style={{
                            width: 640,
                            display: 'flex',
                            flexDirection: 'column',
                            gap: 26,
                            flexShrink: 0,
                        }}
                    >
                        <Eyebrow start={6} align="left">
                            Tu entrada
                        </Eyebrow>
                        {/* 78 px evita que «TU EXPERIENCIA.» se parta en dos
                            líneas dentro de la columna de 640 px. */}
                        <MaskedTitle start={12} size={78} align="left">
                            TU ENTRADA.
                            <br />
                            <BrandText>TU EXPERIENCIA.</BrandText>
                        </MaskedTitle>
                        <Support start={40} align="left" maxWidth={560}>
                            Boleta digital con código QR, pases para Wallet y control de acceso en
                            la puerta del evento.
                        </Support>
                        <div style={{marginTop: 16}}>{access}</div>
                    </div>

                    <div
                        style={{
                            flex: 1,
                            display: 'flex',
                            justifyContent: 'center',
                            alignItems: 'center',
                        }}
                    >
                        {phone}
                    </div>
                </AbsoluteFill>

                {/* Destello en el momento de la validación */}
                <AbsoluteFill
                    style={{
                        background: `radial-gradient(40% 40% at ${vertical ? '50%' : '68%'} 50%,
                            rgba(74,222,128,${0.22 * Math.max(0, 1 - Math.abs(frame - 196) / 18)}) 0%,
                            rgba(8,17,24,0) 70%)`,
                        pointerEvents: 'none',
                    }}
                />

                <LightSweep start={276} duration={26} />
            </Backdrop>
        </AbsoluteFill>
    );
};

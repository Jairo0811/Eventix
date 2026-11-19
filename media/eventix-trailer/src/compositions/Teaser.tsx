import React from 'react';
import {
    AbsoluteFill,
    Audio,
    Img,
    Sequence,
    interpolate,
    staticFile,
    useCurrentFrame,
} from 'remotion';
import {Backdrop} from '../components/Backdrop';
import {Particles} from '../components/Particles';
import {DemoNote, LightSweep, Logo} from '../components/Brand';
import {PhoneFrame} from '../components/Frames';
import {DashboardPanel, DigitalTicket} from '../components/Recreations';
import {BrandText, MaskedTitle} from '../components/Typography';
import {COLORS, FONT_STACK, seconds} from '../theme/tokens';
import {CINEMATIC} from '../animations/easing';

/**
 * Teaser vertical de 15 segundos.
 *
 * No es un recorte del tráiler: tiene su propio montaje, más directo, con
 * cuatro bloques de unos cuatro segundos. Reutiliza los mismos componentes,
 * que es lo que mantiene la coherencia visual entre las tres piezas.
 */

/** 00:00-00:03 · Logo con impacto de luz. */
const TeaserApertura: React.FC = () => {
    const frame = useCurrentFrame();
    const burst = interpolate(frame, [0, 22], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });

    return (
        <Backdrop glow={burst} grid={false}>
            <Particles count={44} intensity={burst} converge={0.42} />
            <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', padding: '0 64px'}}>
                <Logo width={840} start={4} glow={burst} />
            </AbsoluteFill>
            <LightSweep start={62} duration={26} />
        </Backdrop>
    );
};

/** 00:03-00:07 · Home y descubrimiento de eventos. */
const TeaserDescubre: React.FC = () => {
    const frame = useCurrentFrame();
    const rise = interpolate(frame, [0, 110], [40, -40], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
    });

    return (
        <Backdrop glow={0.86} glowY="38%">
            <Particles count={30} intensity={0.5} />
            <AbsoluteFill
                style={{
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: '130px 56px',
                    gap: 46,
                }}
            >
                <MaskedTitle start={4} size={78} align="center">
                    DESCUBRE
                    <br />
                    <BrandText>EVENTOS.</BrandText>
                </MaskedTitle>
                <div
                    style={{
                        width: 880,
                        height: 760,
                        borderRadius: 22,
                        overflow: 'hidden',
                        border: `1px solid ${COLORS.border}`,
                        boxShadow: '0 40px 110px rgba(0,0,0,0.6)',
                    }}
                >
                    <Img
                        src={staticFile('screenshots/real/home-full.png')}
                        style={{
                            width: '100%',
                            display: 'block',
                            transform: `translateY(${rise}px) scale(1.04)`,
                            transformOrigin: 'top center',
                        }}
                    />
                </div>
            </AbsoluteFill>
            <LightSweep start={92} duration={24} direction="left" />
        </Backdrop>
    );
};

/** 00:07-00:11 · Boleta digital, QR y panel de gestión. */
const TeaserProducto: React.FC = () => {
    const frame = useCurrentFrame();
    const zoom = interpolate(frame, [10, 90], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });
    const panel = interpolate(frame, [46, 86], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });

    return (
        <Backdrop glow={0.92} glowY="44%">
            <Particles count={32} intensity={0.52} />
            <AbsoluteFill
                style={{
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: '120px 50px',
                    gap: 40,
                }}
            >
                <MaskedTitle start={2} size={70} align="center">
                    TU ENTRADA.
                    <br />
                    <BrandText>TU ACCESO.</BrandText>
                </MaskedTitle>

                <div style={{transform: `scale(${1 + zoom * 0.07})`}}>
                    <PhoneFrame width={560} height={1130}>
                        <div
                            style={{
                                position: 'absolute',
                                inset: 0,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                padding: 34,
                            }}
                        >
                            <DigitalTicket width={476} start={6} zoomToQr={zoom} />
                        </div>
                    </PhoneFrame>
                </div>

                <div
                    style={{
                        opacity: panel,
                        transform: `translateY(${(1 - panel) * 34}px)`,
                    }}
                >
                    <DashboardPanel width={880} start={48} />
                </div>
            </AbsoluteFill>
            <LightSweep start={94} duration={24} />
        </Backdrop>
    );
};

/** 00:11-00:15 · Logo y eslogan. */
const TeaserCierre: React.FC = () => {
    const frame = useCurrentFrame();
    const blackout = interpolate(frame, [86, 118], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });
    const glow = interpolate(frame, [0, 26, 80, 114], [0.3, 1, 0.92, 0], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });
    const claim = interpolate(frame, [6, 30], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });

    return (
        <AbsoluteFill style={{backgroundColor: COLORS.deep}}>
            <Backdrop glow={glow} grid={false}>
                <Particles count={26} intensity={glow * 0.7} />
                <AbsoluteFill
                    style={{
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: 44,
                        padding: '0 62px',
                    }}
                >
                    <div
                        style={{
                            fontFamily: FONT_STACK,
                            fontSize: 44,
                            fontWeight: 800,
                            letterSpacing: '0.06em',
                            textAlign: 'center',
                            color: COLORS.onDark,
                            opacity: claim,
                            transform: `translateY(${(1 - claim) * 20}px)`,
                            lineHeight: 1.3,
                        }}
                    >
                        EL FUTURO DE TUS EVENTOS
                        <br />
                        <BrandText>COMIENZA AQUÍ.</BrandText>
                    </div>

                    {/* El eslogan viaja dentro del propio logo oficial; no se
                        repite como texto para no duplicarlo en pantalla. */}
                    <Logo width={880} start={16} glow={glow} />

                    <DemoNote start={62} size={24} />
                </AbsoluteFill>
            </Backdrop>
            <AbsoluteFill
                style={{backgroundColor: '#02070a', opacity: blackout, pointerEvents: 'none'}}
            />
        </AbsoluteFill>
    );
};

export const Teaser: React.FC = () => (
    <AbsoluteFill style={{backgroundColor: COLORS.background}}>
        <Audio src={staticFile('audio/teaser-15s.wav')} />

        <Sequence from={0} durationInFrames={seconds(3)} name="1 · Apertura">
            <TeaserApertura />
        </Sequence>
        <Sequence from={seconds(3)} durationInFrames={seconds(4)} name="2 · Descubre">
            <TeaserDescubre />
        </Sequence>
        <Sequence from={seconds(7)} durationInFrames={seconds(4)} name="3 · Producto">
            <TeaserProducto />
        </Sequence>
        <Sequence from={seconds(11)} durationInFrames={seconds(4)} name="4 · Cierre">
            <TeaserCierre />
        </Sequence>
    </AbsoluteFill>
);

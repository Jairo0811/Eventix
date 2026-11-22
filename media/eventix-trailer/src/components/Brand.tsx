import React from 'react';
import {AbsoluteFill, Img, interpolate, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {COLORS} from '../theme/tokens';
import {CINEMATIC, pop} from '../animations/easing';

/**
 * Logo oficial de Eventix.
 *
 * Se usa el archivo real del repositorio (netlify/images/eventix-logo.png),
 * que ya incluye el eslogan. No se redibuja ni se altera: la fidelidad de la
 * marca es un requisito, y recomponerla a mano solo introduciría diferencias.
 */
export const Logo: React.FC<{
    width: number;
    start?: number;
    /** Halo detrás del logo, de 0 a 1. */
    glow?: number;
}> = ({width, start = 0, glow = 1}) => {
    const frame = useCurrentFrame();
    const {fps} = useVideoConfig();

    const entrance = pop(frame, fps, start, 16);
    const opacity = interpolate(frame, [start, start + 14], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });
    // Latido muy leve: evita que el cierre quede inerte.
    const pulse = interpolate(
        Math.sin(((frame - start) / 76) * Math.PI * 2),
        [-1, 1],
        [0.95, 1.06]
    );

    return (
        <div
            style={{
                position: 'relative',
                width,
                opacity,
                transform: `scale(${0.82 + entrance * 0.18})`,
            }}
        >
            <div
                style={{
                    position: 'absolute',
                    inset: '-24%',
                    background: `radial-gradient(50% 50% at 50% 50%,
                        rgba(74,222,128,${0.3 * glow * pulse}) 0%,
                        rgba(45,212,191,${0.17 * glow * pulse}) 42%,
                        rgba(8,17,24,0) 74%)`,
                    filter: 'blur(6px)',
                }}
            />
            <Img
                src={staticFile('branding/eventix-logo.png')}
                style={{width: '100%', display: 'block', position: 'relative'}}
            />
        </div>
    );
};

/**
 * Barrido de luz verde: la transición que une las escenas del storyboard.
 * Cruza el encuadre y deja un rastro luminoso.
 */
export const LightSweep: React.FC<{
    start: number;
    duration?: number;
    /** Dirección del barrido. */
    direction?: 'left' | 'right';
}> = ({start, duration = 20, direction = 'right'}) => {
    const frame = useCurrentFrame();

    if (frame < start || frame > start + duration) {
        return null;
    }

    const progress = interpolate(frame, [start, start + duration], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });
    const position = direction === 'right' ? -40 + progress * 180 : 140 - progress * 180;
    // Más intenso en mitad del recorrido que en los extremos.
    const strength = Math.sin(progress * Math.PI);

    return (
        <AbsoluteFill style={{pointerEvents: 'none', overflow: 'hidden'}}>
            <div
                style={{
                    position: 'absolute',
                    top: '-30%',
                    bottom: '-30%',
                    left: `${position}%`,
                    width: '34%',
                    transform: 'skewX(-14deg)',
                    background: `linear-gradient(90deg,
                        rgba(45,212,191,0) 0%,
                        rgba(74,222,128,${0.3 * strength}) 38%,
                        rgba(190,242,100,${0.52 * strength}) 52%,
                        rgba(45,212,191,0) 100%)`,
                    filter: 'blur(26px)',
                }}
            />
        </AbsoluteFill>
    );
};

/** Franja inferior con la dirección de la demo, usada en el cierre. */
export const DemoNote: React.FC<{start: number; size?: number}> = ({
    start,
    size = 22,
}) => {
    const frame = useCurrentFrame();
    const opacity = interpolate(frame, [start, start + 22], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });

    return (
        <div style={{opacity, textAlign: 'center'}}>
            <div
                style={{
                    fontSize: size,
                    fontWeight: 600,
                    letterSpacing: '0.12em',
                    color: COLORS.muted,
                }}
            >
                Conoce nuestra demo
            </div>
            <div
                style={{
                    marginTop: size * 0.34,
                    fontSize: size * 0.96,
                    fontWeight: 700,
                    letterSpacing: '0.04em',
                    color: COLORS.secondary,
                    fontFamily: 'ui-monospace, monospace',
                }}
            >
                eventixrd.netlify.app
            </div>
        </div>
    );
};

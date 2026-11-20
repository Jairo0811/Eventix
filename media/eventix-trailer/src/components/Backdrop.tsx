import React from 'react';
import {AbsoluteFill, interpolate, useCurrentFrame} from 'remotion';
import {COLORS} from '../theme/tokens';

/**
 * Fondo común de todas las escenas: azul marino casi negro, con un halo de
 * color que respira y una viñeta que concentra la mirada en el centro.
 *
 * Mantener un único fondo para toda la pieza es lo que impide que el montaje
 * parezca una colección de clips sueltos.
 */
export const Backdrop: React.FC<{
    /** Intensidad del halo, de 0 a 1. Las escenas de cierre usan menos. */
    glow?: number;
    /** Desplaza el halo para acompañar la composición de cada escena. */
    glowX?: string;
    glowY?: string;
    /** Muestra la retícula técnica de fondo. */
    grid?: boolean;
    children?: React.ReactNode;
}> = ({glow = 1, glowX = '50%', glowY = '46%', grid = true, children}) => {
    const frame = useCurrentFrame();

    // Respiración lenta: el halo nunca queda completamente quieto.
    const breath = interpolate(
        Math.sin((frame / 108) * Math.PI * 2),
        [-1, 1],
        [0.84, 1.08]
    );

    return (
        <AbsoluteFill style={{backgroundColor: COLORS.background}}>
            {/* Halo de marca */}
            <AbsoluteFill
                style={{
                    background: `radial-gradient(60% 55% at ${glowX} ${glowY},
                        rgba(45, 212, 191, ${0.3 * glow * breath}) 0%,
                        rgba(74, 222, 128, ${0.17 * glow * breath}) 32%,
                        rgba(8, 17, 24, 0) 72%)`,
                }}
            />

            {/* Retícula técnica, muy tenue: aporta textura sin distraer */}
            {grid ? (
                <AbsoluteFill
                    style={{
                        backgroundImage: `
                            linear-gradient(rgba(152, 170, 181, 0.055) 1px, transparent 1px),
                            linear-gradient(90deg, rgba(152, 170, 181, 0.055) 1px, transparent 1px)`,
                        backgroundSize: '96px 96px',
                        maskImage:
                            'radial-gradient(70% 60% at 50% 50%, #000 0%, transparent 85%)',
                        WebkitMaskImage:
                            'radial-gradient(70% 60% at 50% 50%, #000 0%, transparent 85%)',
                    }}
                />
            ) : null}

            {children}

            {/* Viñeta: oscurece los bordes y da profundidad cinematográfica */}
            <AbsoluteFill
                style={{
                    background: `radial-gradient(78% 72% at 50% 50%,
                        rgba(0,0,0,0) 42%, rgba(3, 9, 13, 0.62) 100%)`,
                    pointerEvents: 'none',
                }}
            />
        </AbsoluteFill>
    );
};

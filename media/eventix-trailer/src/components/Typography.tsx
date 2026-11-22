import React from 'react';
import {interpolate, useCurrentFrame} from 'remotion';
import {BRAND_GRADIENT, COLORS, FONT_STACK} from '../theme/tokens';
import {CINEMATIC} from '../animations/easing';

/**
 * Título principal con aparición por máscara: el texto se descubre de abajo
 * arriba en lugar de limitarse a aparecer, que es lo que distingue un montaje
 * cuidado de una plantilla genérica.
 */
export const MaskedTitle: React.FC<{
    children: React.ReactNode;
    start?: number;
    size?: number;
    /** Resalta la última palabra con el degradado de marca. */
    accentFrom?: number;
    align?: 'left' | 'center';
    maxWidth?: number;
}> = ({children, start = 0, size = 92, align = 'center', maxWidth}) => {
    const frame = useCurrentFrame();
    const progress = interpolate(frame, [start, start + 26], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });

    return (
        <div
            style={{
                overflow: 'hidden',
                maxWidth,
                textAlign: align,
            }}
        >
            <div
                style={{
                    fontFamily: FONT_STACK,
                    fontSize: size,
                    fontWeight: 800,
                    lineHeight: 1.04,
                    letterSpacing: '-0.025em',
                    color: COLORS.onDark,
                    transform: `translateY(${(1 - progress) * 112}%)`,
                    opacity: Math.min(1, progress * 1.5),
                    textShadow: '0 10px 46px rgba(0,0,0,0.55)',
                }}
            >
                {children}
            </div>
        </div>
    );
};

/** Palabra o fragmento con el degradado de marca aplicado al texto. */
export const BrandText: React.FC<{children: React.ReactNode}> = ({children}) => (
    <span
        style={{
            backgroundImage: BRAND_GRADIENT,
            WebkitBackgroundClip: 'text',
            backgroundClip: 'text',
            color: 'transparent',
        }}
    >
        {children}
    </span>
);

/**
 * Antetítulo: texto corto en mayúsculas sobre el título, con el punto verde
 * que la propia interfaz de Eventix usa en sus secciones.
 */
export const Eyebrow: React.FC<{
    children: React.ReactNode;
    start?: number;
    align?: 'left' | 'center';
}> = ({children, start = 0, align = 'center'}) => {
    const frame = useCurrentFrame();
    const progress = interpolate(frame, [start, start + 20], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });

    return (
        <div
            style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: align === 'center' ? 'center' : 'flex-start',
                gap: 14,
                opacity: progress,
                transform: `translateY(${(1 - progress) * 16}px)`,
            }}
        >
            <span
                style={{
                    width: 11,
                    height: 11,
                    borderRadius: '50%',
                    backgroundColor: COLORS.primary,
                    boxShadow: `0 0 18px ${COLORS.primary}`,
                }}
            />
            <span
                style={{
                    fontFamily: FONT_STACK,
                    fontSize: 24,
                    fontWeight: 700,
                    letterSpacing: '0.2em',
                    textTransform: 'uppercase',
                    color: COLORS.primary,
                }}
            >
                {children}
            </span>
        </div>
    );
};

/** Línea de apoyo bajo el título. */
export const Support: React.FC<{
    children: React.ReactNode;
    start?: number;
    size?: number;
    align?: 'left' | 'center';
    maxWidth?: number;
}> = ({children, start = 0, size = 30, align = 'center', maxWidth = 900}) => {
    const frame = useCurrentFrame();
    const progress = interpolate(frame, [start, start + 24], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });

    return (
        <p
            style={{
                fontFamily: FONT_STACK,
                fontSize: size,
                fontWeight: 500,
                lineHeight: 1.45,
                color: COLORS.muted,
                margin: 0,
                maxWidth,
                textAlign: align,
                opacity: progress,
                transform: `translateY(${(1 - progress) * 20}px)`,
            }}
        >
            {children}
        </p>
    );
};

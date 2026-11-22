import React from 'react';
import {Img, staticFile} from 'remotion';
import {COLORS} from '../theme/tokens';

/**
 * Marcos de dispositivo para presentar las pantallas de Eventix.
 *
 * El marco cumple dos funciones: da profundidad cinematográfica a una captura
 * plana y, sobre todo, deja claro al espectador que lo que ve es una interfaz
 * de software, no un cartel.
 */

/** Ventana de escritorio con barra de título sobria. */
export const BrowserFrame: React.FC<{
    src: string;
    width: number;
    /** Altura visible del recorte; la captura se ancla arriba. */
    height: number;
    /** Desplazamiento vertical dentro de la captura, para encuadrar. */
    offsetY?: number;
    /** Escala interna de la captura. */
    zoom?: number;
    radius?: number;
    style?: React.CSSProperties;
}> = ({src, width, height, offsetY = 0, zoom = 1, radius = 18, style}) => (
    <div
        style={{
            width,
            borderRadius: radius,
            overflow: 'hidden',
            backgroundColor: COLORS.elevated,
            border: `1px solid ${COLORS.border}`,
            boxShadow:
                '0 44px 120px rgba(0,0,0,0.62), 0 0 0 1px rgba(45,212,191,0.10), 0 0 90px rgba(45,212,191,0.10)',
            ...style,
        }}
    >
        {/* Barra de la ventana */}
        <div
            style={{
                height: 38,
                display: 'flex',
                alignItems: 'center',
                gap: 9,
                padding: '0 16px',
                backgroundColor: COLORS.sunken,
                borderBottom: `1px solid ${COLORS.border}`,
            }}
        >
            {['#f87171', '#fbbf24', COLORS.primary].map((color) => (
                <span
                    key={color}
                    style={{
                        width: 11,
                        height: 11,
                        borderRadius: '50%',
                        backgroundColor: color,
                        opacity: 0.75,
                    }}
                />
            ))}
            <span
                style={{
                    marginLeft: 14,
                    fontSize: 13,
                    letterSpacing: '0.04em',
                    color: COLORS.muted,
                    fontFamily: 'ui-monospace, monospace',
                }}
            >
                eventixrd.netlify.app
            </span>
        </div>

        {/* Lienzo de la captura */}
        <div style={{height, overflow: 'hidden', position: 'relative'}}>
            <Img
                src={staticFile(src)}
                style={{
                    width: '100%',
                    display: 'block',
                    transform: `translateY(${-offsetY}px) scale(${zoom})`,
                    transformOrigin: 'top center',
                }}
            />
        </div>
    </div>
);

/** Teléfono con marco, pensado para la boleta digital y la versión vertical. */
export const PhoneFrame: React.FC<{
    width: number;
    height: number;
    children: React.ReactNode;
    style?: React.CSSProperties;
}> = ({width, height, children, style}) => (
    <div
        style={{
            width,
            height,
            borderRadius: width * 0.13,
            padding: width * 0.028,
            background: 'linear-gradient(160deg, #334652 0%, #16242e 46%, #0b151c 100%)',
            boxShadow:
                '0 52px 130px rgba(0,0,0,0.68), 0 0 0 1px rgba(45,212,191,0.14), 0 0 110px rgba(74,222,128,0.14)',
            position: 'relative',
            ...style,
        }}
    >
        <div
            style={{
                width: '100%',
                height: '100%',
                borderRadius: width * 0.105,
                overflow: 'hidden',
                backgroundColor: COLORS.background,
                position: 'relative',
            }}
        >
            {/* Muesca superior */}
            <div
                style={{
                    position: 'absolute',
                    top: 0,
                    left: '50%',
                    transform: 'translateX(-50%)',
                    width: width * 0.3,
                    height: width * 0.055,
                    borderBottomLeftRadius: 14,
                    borderBottomRightRadius: 14,
                    backgroundColor: '#0b151c',
                    zIndex: 3,
                }}
            />
            {children}
        </div>
    </div>
);

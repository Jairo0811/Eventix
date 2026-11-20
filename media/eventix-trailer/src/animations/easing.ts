/**
 * Curvas y utilidades de animación compartidas por todas las escenas.
 *
 * Tener un único lugar para las curvas es lo que hace que la pieza se sienta
 * de una sola pieza: si cada escena inventara su propio ritmo, el montaje
 * parecería una suma de clips en vez de un tráiler.
 */
import {Easing, interpolate, spring} from 'remotion';

/** Curva principal: entra con decisión y asienta con suavidad. */
export const CINEMATIC = Easing.bezier(0.16, 1, 0.3, 1);

/** Curva para salidas: acelera al final, útil en barridos y cortes. */
export const EXIT = Easing.bezier(0.7, 0, 0.84, 0);

/**
 * Aparición estándar: opacidad y desplazamiento vertical combinados.
 * Devuelve los estilos listos para aplicar.
 */
export const reveal = (
    frame: number,
    start: number,
    duration = 24,
    distance = 38
): {opacity: number; transform: string} => {
    const progress = interpolate(frame, [start, start + duration], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });
    return {
        opacity: progress,
        transform: `translateY(${(1 - progress) * distance}px)`,
    };
};

/** Desvanecido simple entre dos instantes. */
export const fade = (
    frame: number,
    start: number,
    duration = 18
): number =>
    interpolate(frame, [start, start + duration], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });

/** Desvanecido de salida: 1 hasta `start`, luego a 0. */
export const fadeOut = (
    frame: number,
    start: number,
    duration = 18
): number =>
    interpolate(frame, [start, start + duration], [1, 0], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: EXIT,
    });

/**
 * Movimiento de cámara lento sobre una imagen fija: el recurso que convierte
 * una captura estática en un plano con vida. Escala y desplaza a la vez.
 */
export const kenBurns = (
    frame: number,
    duration: number,
    options: {
        fromScale?: number;
        toScale?: number;
        fromX?: number;
        toX?: number;
        fromY?: number;
        toY?: number;
    } = {}
): string => {
    const {
        fromScale = 1.04,
        toScale = 1.14,
        fromX = 0,
        toX = 0,
        fromY = 0,
        toY = 0,
    } = options;
    const progress = interpolate(frame, [0, duration], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: Easing.inOut(Easing.ease),
    });
    const scale = fromScale + (toScale - fromScale) * progress;
    const x = fromX + (toX - fromX) * progress;
    const y = fromY + (toY - fromY) * progress;
    return `translate3d(${x}px, ${y}px, 0) scale(${scale})`;
};

/** Rebote contenido para el logo y los impactos. */
export const pop = (
    frame: number,
    fps: number,
    delay = 0,
    damping = 14
): number =>
    spring({
        frame: frame - delay,
        fps,
        config: {damping, mass: 0.8, stiffness: 110},
    });

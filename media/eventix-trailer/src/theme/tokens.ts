/**
 * Identidad visual de Eventix.
 *
 * Los colores NO se inventaron para el tráiler: están copiados de las
 * variables CSS reales del repositorio
 * (src/main/resources/static/css/*.css, bloque --eventix-*), tomando los
 * valores del tema oscuro, que es el que corresponde a la dirección artística
 * de la pieza. Si la marca cambia allí, este archivo debe actualizarse.
 */

export const COLORS = {
    /** --eventix-background (oscuro): azul marino casi negro. */
    background: '#081118',
    /** --eventix-dark: el tono más profundo, para viñetas y cierres. */
    deep: '#061923',
    /** --eventix-elevated: superficies elevadas, tarjetas y paneles. */
    elevated: '#13232d',
    /** --eventix-input-bg: campos y superficies hundidas. */
    sunken: '#101f28',
    /** --eventix-border. */
    border: '#263945',

    /** --eventix-primary (oscuro): el verde de marca. */
    primary: '#4ade80',
    /** --eventix-primary en tema claro: verde más saturado, para acentos. */
    primaryDeep: '#22c55e',
    /** --eventix-secondary (oscuro): el turquesa de marca. */
    secondary: '#2dd4bf',
    /** --eventix-secondary en tema claro. */
    secondaryDeep: '#14b8a6',
    /** --eventix-accent (oscuro): el lima de los destellos. */
    accent: '#bef264',
    /** --eventix-accent en tema claro. */
    accentDeep: '#a3e635',

    /** --eventix-text (oscuro). */
    text: '#e6edf3',
    /** --eventix-muted (oscuro). */
    muted: '#98aab5',
    /** --eventix-final-on-dark: el blanco cálido del cierre. */
    onDark: '#eef8f4',
} as const;

/**
 * Inter es la tipografía de la aplicación
 * (font-family: Inter, "Segoe UI", Roboto, Arial, sans-serif en app.css).
 * Se declara la misma pila para que el tráiler y el producto coincidan, y se
 * mantienen las alternativas por si Inter no está instalada en la máquina que
 * renderiza.
 */
export const FONT_STACK =
    'Inter, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif';

/** Eslogan oficial, tal y como aparece en el logo de la marca. */
export const TAGLINE = 'TRANSFORMA TUS EVENTOS. CONECTA EXPERIENCIAS.';

/** Dirección de la demo pública, mostrada con discreción en el cierre. */
export const DEMO_URL = 'eventixrd.netlify.app';

/**
 * Degradado de marca: del turquesa al lima pasando por el verde. Es el mismo
 * recorrido cromático del logo oficial.
 */
export const BRAND_GRADIENT =
    `linear-gradient(100deg, ${COLORS.secondary} 0%, ${COLORS.primary} 48%, ${COLORS.accent} 100%)`;

/** Duraciones del tráiler principal, en segundos, según el storyboard. */
export const SCENE_TIMING = {
    inicio: {from: 0, duration: 8},
    descubre: {from: 8, duration: 9},
    organiza: {from: 17, duration: 10},
    entrada: {from: 27, duration: 10},
    conecta: {from: 37, duration: 7},
    cierre: {from: 44, duration: 6},
} as const;

export const FPS = 30;

/** Convierte segundos a fotogramas con los FPS del proyecto. */
export const seconds = (value: number): number => Math.round(value * FPS);

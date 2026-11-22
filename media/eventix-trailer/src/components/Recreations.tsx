import React from 'react';
import {Img, interpolate, staticFile, useCurrentFrame} from 'remotion';
import {COLORS, FONT_STACK} from '../theme/tokens';
import {CINEMATIC} from '../animations/easing';

/**
 * RECREACIONES VISUALES.
 *
 * Estas pantallas NO son capturas de la aplicación en ejecución. El dashboard,
 * la boleta digital y el control de acceso viven detrás de autenticación y
 * requieren la aplicación Spring Boot levantada, que no estaba disponible en el
 * entorno donde se produjo el tráiler. Son recreaciones construidas con los
 * tokens de color, la tipografía y la estructura reales del repositorio.
 *
 * Reglas que se respetan aquí y que conviene no romper al editarlas:
 *
 *  - No se inventan cifras de ingresos, ventas ni usuarios. Los indicadores
 *    muestran etiquetas ilustrativas y van rotulados como demostración.
 *  - El código QR codifica la cadena EVENTIX-DEMO-NO-VALIDO-PARA-ACCESO y
 *    aparece marcado como DEMO: no es una credencial válida.
 *  - No se representa ninguna función que no exista en el producto.
 *
 * Están registradas como recreación en ASSET-LICENSES.md y en el informe de QA.
 */

/** Rótulo que acompaña a toda recreación dentro del encuadre. */
const DemoBadge: React.FC<{label?: string}> = ({label = 'Vista de demostración'}) => (
    <span
        style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 8,
            padding: '7px 14px',
            borderRadius: 999,
            fontFamily: FONT_STACK,
            fontSize: 15,
            fontWeight: 600,
            color: COLORS.primary,
            backgroundColor: 'rgba(74, 222, 128, 0.12)',
            border: `1px solid rgba(74, 222, 128, 0.34)`,
        }}
    >
        <span
            style={{
                width: 8,
                height: 8,
                borderRadius: '50%',
                backgroundColor: COLORS.primary,
            }}
        />
        {label}
    </span>
);

/**
 * Panel de gestión. Las capas se animan por separado para construir el
 * movimiento 2.5D que pide el storyboard, sin alterar la información mostrada.
 */
export const DashboardPanel: React.FC<{
    width: number;
    start?: number;
}> = ({width, start = 0}) => {
    const frame = useCurrentFrame();
    const scale = width / 1180;

    const layer = (delay: number) => {
        const progress = interpolate(frame, [start + delay, start + delay + 26], [0, 1], {
            extrapolateLeft: 'clamp',
            extrapolateRight: 'clamp',
            easing: CINEMATIC,
        });
        return {
            opacity: progress,
            transform: `translateY(${(1 - progress) * 26}px)`,
        };
    };

    const cards = [
        {label: 'Eventos publicados', hint: 'Catálogo activo'},
        {label: 'Reservaciones', hint: 'En seguimiento'},
        {label: 'Asistentes registrados', hint: 'Control de acceso'},
    ];

    return (
        <div
            style={{
                width,
                borderRadius: 20 * scale,
                overflow: 'hidden',
                backgroundColor: COLORS.elevated,
                border: `1px solid ${COLORS.border}`,
                boxShadow: '0 44px 120px rgba(0,0,0,0.6), 0 0 90px rgba(74,222,128,0.10)',
                fontFamily: FONT_STACK,
                display: 'flex',
            }}
        >
            {/* Barra lateral oscura: la identidad de Eventix en el área privada */}
            <div
                style={{
                    width: 232 * scale,
                    backgroundColor: COLORS.deep,
                    padding: `${26 * scale}px ${20 * scale}px`,
                    borderRight: `1px solid ${COLORS.border}`,
                }}
            >
                <div
                    style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 10 * scale,
                        marginBottom: 30 * scale,
                    }}
                >
                    <Img
                        src={staticFile('branding/eventix-favicon.png')}
                        style={{width: 30 * scale, height: 30 * scale}}
                    />
                    <span
                        style={{
                            color: COLORS.onDark,
                            fontWeight: 800,
                            fontSize: 20 * scale,
                            letterSpacing: '-0.02em',
                        }}
                    >
                        Eventix
                    </span>
                </div>
                {['Panel', 'Eventos', 'Reservaciones', 'Boletas', 'Accesos', 'Reportes'].map(
                    (item, index) => (
                        <div
                            key={item}
                            style={{
                                ...layer(6 + index * 2),
                                padding: `${11 * scale}px ${13 * scale}px`,
                                marginBottom: 7 * scale,
                                borderRadius: 10 * scale,
                                fontSize: 16 * scale,
                                fontWeight: index === 0 ? 700 : 500,
                                color: index === 0 ? COLORS.deep : COLORS.muted,
                                backgroundColor:
                                    index === 0 ? COLORS.primary : 'transparent',
                            }}
                        >
                            {item}
                        </div>
                    )
                )}
            </div>

            {/* Área principal clara, como en la aplicación */}
            <div
                style={{
                    flex: 1,
                    backgroundColor: '#f6f9fa',
                    padding: `${26 * scale}px ${30 * scale}px`,
                }}
            >
                <div
                    style={{
                        ...layer(2),
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        marginBottom: 24 * scale,
                    }}
                >
                    <div>
                        <div
                            style={{
                                fontSize: 27 * scale,
                                fontWeight: 800,
                                color: '#0f172a',
                                letterSpacing: '-0.02em',
                            }}
                        >
                            Gestión de eventos
                        </div>
                        <div style={{fontSize: 15 * scale, color: '#64748b', marginTop: 4 * scale}}>
                            Publica, controla el aforo y sigue cada reservación.
                        </div>
                    </div>
                    <div style={{transform: `scale(${scale})`, transformOrigin: 'right center'}}>
                        <DemoBadge />
                    </div>
                </div>

                {/* Indicadores sin cifras inventadas */}
                <div style={{display: 'flex', gap: 16 * scale, marginBottom: 22 * scale}}>
                    {cards.map((card, index) => (
                        <div
                            key={card.label}
                            style={{
                                ...layer(10 + index * 4),
                                flex: 1,
                                padding: `${17 * scale}px ${18 * scale}px`,
                                borderRadius: 14 * scale,
                                backgroundColor: '#ffffff',
                                border: '1px solid #e2e8f0',
                            }}
                        >
                            <div style={{fontSize: 14 * scale, color: '#64748b'}}>{card.label}</div>
                            {/* Barra ilustrativa en lugar de un número inventado */}
                            <div
                                style={{
                                    height: 11 * scale,
                                    borderRadius: 999,
                                    marginTop: 13 * scale,
                                    background: `linear-gradient(90deg, ${COLORS.secondaryDeep}, ${COLORS.primaryDeep})`,
                                    width: `${58 + index * 13}%`,
                                }}
                            />
                            <div style={{fontSize: 13 * scale, color: '#94a3b8', marginTop: 9 * scale}}>
                                {card.hint}
                            </div>
                        </div>
                    ))}
                </div>

                {/* Tabla de eventos, con nombres ficticios de la propia demo */}
                <div
                    style={{
                        ...layer(22),
                        borderRadius: 14 * scale,
                        backgroundColor: '#ffffff',
                        border: '1px solid #e2e8f0',
                        overflow: 'hidden',
                    }}
                >
                    {[
                        ['Eventix Tech Experience 2026', 'Publicado'],
                        ['Foro de innovación digital', 'Borrador'],
                        ['Festival de música y luces', 'Publicado'],
                    ].map(([title, state], index) => (
                        <div
                            key={title}
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                padding: `${14 * scale}px ${18 * scale}px`,
                                borderTop: index ? '1px solid #eef2f5' : 'none',
                            }}
                        >
                            <span style={{fontSize: 16 * scale, color: '#0f172a', fontWeight: 600}}>
                                {title}
                            </span>
                            <span
                                style={{
                                    fontSize: 13 * scale,
                                    fontWeight: 700,
                                    padding: `${5 * scale}px ${11 * scale}px`,
                                    borderRadius: 999,
                                    color: state === 'Publicado' ? '#126b36' : '#694700',
                                    backgroundColor:
                                        state === 'Publicado' ? '#dcfce7' : '#fef3c7',
                                }}
                            >
                                {state}
                            </span>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};

/**
 * Boleta digital con el código QR de demostración.
 * El QR codifica una cadena que declara explícitamente que no es válida.
 */
export const DigitalTicket: React.FC<{
    width: number;
    start?: number;
    /** De 0 a 1: acerca la vista al QR. */
    zoomToQr?: number;
}> = ({width, start = 0, zoomToQr = 0}) => {
    const frame = useCurrentFrame();
    const scale = width / 520;

    const appear = interpolate(frame, [start, start + 26], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });

    return (
        <div
            style={{
                width,
                fontFamily: FONT_STACK,
                opacity: appear,
                transform: `translateY(${(1 - appear) * 30}px) scale(${1 + zoomToQr * 0.1})`,
            }}
        >
            <div
                style={{
                    borderRadius: 26 * scale,
                    overflow: 'hidden',
                    backgroundColor: '#ffffff',
                    boxShadow: '0 40px 110px rgba(0,0,0,0.5)',
                }}
            >
                {/* Cabecera con el degradado de marca */}
                <div
                    style={{
                        padding: `${22 * scale}px ${26 * scale}px`,
                        background: `linear-gradient(120deg, ${COLORS.secondaryDeep}, ${COLORS.primaryDeep})`,
                    }}
                >
                    <div
                        style={{
                            fontSize: 13 * scale,
                            fontWeight: 800,
                            letterSpacing: '0.18em',
                            color: 'rgba(6,25,35,0.72)',
                        }}
                    >
                        BOLETA DIGITAL · DEMO
                    </div>
                    <div
                        style={{
                            fontSize: 27 * scale,
                            fontWeight: 800,
                            color: '#06212b',
                            marginTop: 5 * scale,
                            letterSpacing: '-0.02em',
                        }}
                    >
                        Eventix Tech Experience 2026
                    </div>
                    <div style={{fontSize: 15 * scale, color: 'rgba(6,33,43,0.76)', marginTop: 3 * scale}}>
                        21 noviembre 2026 · 6:00 p. m. · Santo Domingo
                    </div>
                </div>

                {/* Cuerpo con el QR */}
                <div style={{padding: `${26 * scale}px`, textAlign: 'center'}}>
                    <div
                        style={{
                            display: 'inline-block',
                            padding: 13 * scale,
                            borderRadius: 18 * scale,
                            border: `2px solid ${COLORS.primaryDeep}`,
                            backgroundColor: '#ffffff',
                            boxShadow: `0 0 ${40 * scale}px rgba(34,197,94,${0.22 + zoomToQr * 0.4})`,
                        }}
                    >
                        <Img
                            src={staticFile('branding/qr-demo.png')}
                            style={{
                                width: 208 * scale,
                                height: 208 * scale,
                                display: 'block',
                            }}
                        />
                    </div>
                    <div
                        style={{
                            marginTop: 13 * scale,
                            fontSize: 13 * scale,
                            fontWeight: 700,
                            letterSpacing: '0.1em',
                            color: '#64748b',
                        }}
                    >
                        CÓDIGO DE MUESTRA · NO VÁLIDO PARA ACCESO
                    </div>

                    <div
                        style={{
                            marginTop: 20 * scale,
                            paddingTop: 18 * scale,
                            borderTop: '1px dashed #cbd5e1',
                            display: 'flex',
                            justifyContent: 'space-between',
                            textAlign: 'left',
                        }}
                    >
                        {[
                            ['Asistente', 'Nombre de muestra'],
                            ['Entrada', 'General'],
                            ['Sección', 'A · Fila 1'],
                        ].map(([label, value]) => (
                            <div key={label}>
                                <div style={{fontSize: 12 * scale, color: '#94a3b8'}}>{label}</div>
                                <div
                                    style={{
                                        fontSize: 16 * scale,
                                        fontWeight: 700,
                                        color: '#0f172a',
                                        marginTop: 2 * scale,
                                    }}
                                >
                                    {value}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
};

/**
 * Control de acceso. El indicador de entrada válida corresponde a un escenario
 * de prueba y va rotulado como tal, nunca como una validación real.
 */
export const AccessCheck: React.FC<{
    width: number;
    start?: number;
}> = ({width, start = 0}) => {
    const frame = useCurrentFrame();
    const scale = width / 480;

    const appear = interpolate(frame, [start, start + 18], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });
    // El visto bueno llega un poco después del panel, como en un escaneo real.
    const check = interpolate(frame, [start + 14, start + 30], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: CINEMATIC,
    });

    return (
        <div
            style={{
                width,
                fontFamily: FONT_STACK,
                opacity: appear,
                transform: `translateY(${(1 - appear) * 22}px)`,
                borderRadius: 20 * scale,
                padding: `${24 * scale}px ${26 * scale}px`,
                backgroundColor: COLORS.elevated,
                border: `1px solid rgba(74,222,128,0.34)`,
                boxShadow: '0 34px 90px rgba(0,0,0,0.55), 0 0 70px rgba(74,222,128,0.14)',
            }}
        >
            <div style={{display: 'flex', alignItems: 'center', gap: 16 * scale}}>
                <div
                    style={{
                        width: 58 * scale,
                        height: 58 * scale,
                        borderRadius: '50%',
                        backgroundColor: COLORS.primary,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        transform: `scale(${0.6 + check * 0.4})`,
                        opacity: check,
                        boxShadow: `0 0 ${34 * scale}px ${COLORS.primary}`,
                    }}
                >
                    <svg width={30 * scale} height={30 * scale} viewBox="0 0 24 24" fill="none">
                        <path
                            d="M4.5 12.6l4.8 4.8L19.5 7.2"
                            stroke="#06212b"
                            strokeWidth={3.2}
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeDasharray={28}
                            strokeDashoffset={28 * (1 - check)}
                        />
                    </svg>
                </div>
                <div>
                    <div
                        style={{
                            fontSize: 25 * scale,
                            fontWeight: 800,
                            color: COLORS.onDark,
                            letterSpacing: '-0.02em',
                        }}
                    >
                        Acceso verificado
                    </div>
                    <div style={{fontSize: 15 * scale, color: COLORS.muted, marginTop: 3 * scale}}>
                        Escenario de prueba · boleta de muestra
                    </div>
                </div>
            </div>
        </div>
    );
};

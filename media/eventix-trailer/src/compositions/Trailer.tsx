import React from 'react';
import {AbsoluteFill, Audio, Sequence, staticFile} from 'remotion';
import {Scene01Inicio} from '../scenes/Scene01Inicio';
import {Scene02Descubre} from '../scenes/Scene02Descubre';
import {Scene03Organiza} from '../scenes/Scene03Organiza';
import {Scene04Entrada} from '../scenes/Scene04Entrada';
import {Scene05Conecta} from '../scenes/Scene05Conecta';
import {Scene06Cierre} from '../scenes/Scene06Cierre';
import {COLORS, SCENE_TIMING, seconds} from '../theme/tokens';

/**
 * Tráiler principal de 50 segundos.
 *
 * La misma composición sirve para 16:9 y 9:16: cada escena recibe `vertical` y
 * se recompone por dentro —reencuadra, reordena y ajusta tipografía— en lugar
 * de recortarse, que es lo que el encargo pedía explícitamente evitar.
 */
export const Trailer: React.FC<{vertical?: boolean}> = ({vertical = false}) => {
    const scene = (key: keyof typeof SCENE_TIMING) => ({
        from: seconds(SCENE_TIMING[key].from),
        durationInFrames: seconds(SCENE_TIMING[key].duration),
    });

    return (
        <AbsoluteFill style={{backgroundColor: COLORS.background}}>
            {/* Banda sonora original, sintetizada en scripts/make-audio.py */}
            <Audio src={staticFile('audio/trailer-50s.wav')} />

            <Sequence {...scene('inicio')} name="1 · El inicio">
                <Scene01Inicio vertical={vertical} />
            </Sequence>

            <Sequence {...scene('descubre')} name="2 · Descubre">
                <Scene02Descubre vertical={vertical} />
            </Sequence>

            <Sequence {...scene('organiza')} name="3 · Organiza">
                <Scene03Organiza vertical={vertical} />
            </Sequence>

            <Sequence {...scene('entrada')} name="4 · Tu entrada digital">
                <Scene04Entrada vertical={vertical} />
            </Sequence>

            <Sequence {...scene('conecta')} name="5 · Conecta experiencias">
                <Scene05Conecta vertical={vertical} />
            </Sequence>

            <Sequence {...scene('cierre')} name="6 · Cierre oficial">
                <Scene06Cierre vertical={vertical} />
            </Sequence>
        </AbsoluteFill>
    );
};

import React from 'react';
import {Composition} from 'remotion';
import {Trailer} from './compositions/Trailer';
import {Teaser} from './compositions/Teaser';
import {FPS} from './theme/tokens';

/**
 * Registro de las tres piezas del encargo. Las dos primeras comparten
 * composición y se diferencian por `vertical`, que recompone cada escena.
 */
export const RemotionRoot: React.FC = () => (
    <>
        <Composition
            id="TrailerHorizontal"
            component={Trailer}
            durationInFrames={FPS * 50}
            fps={FPS}
            width={1920}
            height={1080}
            defaultProps={{vertical: false}}
        />
        <Composition
            id="TrailerVertical"
            component={Trailer}
            durationInFrames={FPS * 50}
            fps={FPS}
            width={1080}
            height={1920}
            defaultProps={{vertical: true}}
        />
        <Composition
            id="Teaser"
            component={Teaser}
            durationInFrames={FPS * 15}
            fps={FPS}
            width={1080}
            height={1920}
        />
    </>
);

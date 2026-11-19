import React, {useMemo} from 'react';
import {AbsoluteFill, interpolate, random, useCurrentFrame} from 'remotion';
import {COLORS} from '../theme/tokens';

type Spark = {
    seed: string;
    x: number;
    y: number;
    size: number;
    speed: number;
    drift: number;
    hue: string;
    delay: number;
};

/**
 * Partículas turquesas y verdes que se desplazan despacio.
 *
 * Usan `random()` de Remotion con semilla fija, de modo que cada fotograma se
 * calcula igual en cada render: el resultado es reproducible, que es un
 * requisito del proyecto, y no parpadea entre renders sucesivos.
 */
export const Particles: React.FC<{
    count?: number;
    /** Opacidad general, para subirlas o bajarlas según la escena. */
    intensity?: number;
    /** Las atrae hacia el centro, útil en la formación del logo. */
    converge?: number;
}> = ({count = 46, intensity = 1, converge = 0}) => {
    const frame = useCurrentFrame();

    const sparks = useMemo<Spark[]>(
        () =>
            new Array(count).fill(true).map((_, index) => {
                const seed = `eventix-spark-${index}`;
                return {
                    seed,
                    x: random(`${seed}-x`) * 100,
                    y: random(`${seed}-y`) * 100,
                    size: 1.4 + random(`${seed}-s`) * 4.2,
                    speed: 0.1 + random(`${seed}-v`) * 0.42,
                    drift: (random(`${seed}-d`) - 0.5) * 24,
                    hue:
                        random(`${seed}-h`) > 0.52
                            ? COLORS.secondary
                            : random(`${seed}-h`) > 0.24
                              ? COLORS.primary
                              : COLORS.accent,
                    delay: random(`${seed}-t`) * 90,
                };
            }),
        [count]
    );

    return (
        <AbsoluteFill style={{pointerEvents: 'none'}}>
            {sparks.map((spark) => {
                const life = frame + spark.delay;
                // Ascenso continuo con reciclado: nunca se agotan.
                const rise = (life * spark.speed) % 118;
                const y = spark.y - rise + 9;
                const wrapped = ((y % 118) + 118) % 118 - 9;
                const x = spark.x + Math.sin(life / 46 + spark.delay) * spark.drift * 0.1;

                // Atracción hacia el centro para la formación del logo.
                const cx = x + (50 - x) * converge;
                const cy = wrapped + (46 - wrapped) * converge;

                const twinkle = interpolate(
                    Math.sin(life / 17 + spark.delay),
                    [-1, 1],
                    [0.22, 0.92]
                );

                return (
                    <div
                        key={spark.seed}
                        style={{
                            position: 'absolute',
                            left: `${cx}%`,
                            top: `${cy}%`,
                            width: spark.size,
                            height: spark.size,
                            borderRadius: '50%',
                            backgroundColor: spark.hue,
                            opacity: twinkle * intensity,
                            boxShadow: `0 0 ${spark.size * 5}px ${spark.hue}`,
                        }}
                    />
                );
            })}
        </AbsoluteFill>
    );
};

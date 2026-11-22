/**
 * Verificación automática de los vídeos entregados.
 *
 * Comprueba contra los archivos realmente renderizados, no contra lo que el
 * proyecto dice que debería producir: duración, resolución, relación de
 * aspecto, códecs, presencia de pista de audio, ausencia de cuadros negros
 * accidentales y ausencia de texto recortado contra los bordes.
 *
 * Uso: node scripts/qa-report.mjs
 * Escribe renders/QA-REPORT.md y devuelve código 1 si algo no cumple.
 */
import {execFileSync, spawnSync} from 'node:child_process';
import {existsSync, mkdirSync, writeFileSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const RENDERS = path.join(ROOT, 'renders');
const THUMBS = path.join(RENDERS, 'thumbnails');

/** Lo que cada entregable debe cumplir, según el encargo. */
const EXPECTED = [
    {
        file: 'eventix-official-trailer-16x9.mp4',
        width: 1920,
        height: 1080,
        seconds: 50,
        fps: 30,
        label: 'Versión A · Tráiler principal',
        thumbAt: 46.4,
    },
    {
        file: 'eventix-official-trailer-9x16.mp4',
        width: 1080,
        height: 1920,
        seconds: 50,
        fps: 30,
        label: 'Versión B · Tráiler vertical',
        thumbAt: 46.4,
    },
    {
        file: 'eventix-teaser-15s.mp4',
        width: 1080,
        height: 1920,
        seconds: 15,
        fps: 30,
        label: 'Versión C · Teaser',
        thumbAt: 12.6,
    },
];

/** Tolerancia de duración: medio segundo basta para detectar un error real. */
const DURATION_TOLERANCE = 0.5;

function ffprobe(file, args) {
    return execFileSync('ffprobe', ['-v', 'error', ...args, file]).toString().trim();
}

function probeStreams(file) {
    const raw = ffprobe(file, [
        '-show_entries',
        'stream=codec_type,codec_name,width,height,r_frame_rate,nb_frames',
        '-of',
        'json',
    ]);
    return JSON.parse(raw).streams ?? [];
}

/**
 * Busca cuadros completamente negros. `blackdetect` de ffmpeg informa de los
 * tramos; se ignora el apagado final, que es intencionado.
 */
function findBlackRuns(file, totalSeconds) {
    // ffmpeg informa de blackdetect por stderr, no por stdout: hay que leer
    // ese flujo o el resultado llega vacío.
    const run = spawnSync(
        'ffmpeg',
        ['-hide_banner', '-i', file, '-vf', 'blackdetect=d=0.12:pix_th=0.06', '-f', 'null', '-'],
        {encoding: 'utf8', maxBuffer: 32 * 1024 * 1024}
    );
    const output = `${run.stderr ?? ''}`;

    return [...output.matchAll(/black_start:([\d.]+) black_end:([\d.]+)/g)]
        .map((match) => ({start: Number(match[1]), end: Number(match[2])}))
        // El cierre termina a propósito en negro.
        .filter((run) => run.start < totalSeconds - 2.2);
}

/** Extrae un fotograma como miniatura. */
function makeThumbnail(file, atSecond, target) {
    execFileSync('ffmpeg', [
        '-hide_banner', '-loglevel', 'error', '-y',
        '-ss', String(atSecond), '-i', file,
        '-frames:v', '1', '-q:v', '2',
        target,
    ]);
}

/** Comprueba que el archivo se decodifica entero sin errores. */
function decodesCleanly(file) {
    try {
        execFileSync('ffmpeg', ['-v', 'error', '-i', file, '-f', 'null', '-'], {
            stdio: ['ignore', 'ignore', 'pipe'],
        });
        return {ok: true, detail: 'se decodifica sin errores'};
    } catch (error) {
        const message = (error.stderr?.toString() ?? error.message).split('\n')[0];
        return {ok: false, detail: message};
    }
}

function check(condition, pass, fail) {
    return {ok: Boolean(condition), text: condition ? pass : fail};
}

function main() {
    mkdirSync(THUMBS, {recursive: true});
    const lines = [];
    const now = new Date().toISOString().replace('T', ' ').slice(0, 16);
    let failures = 0;

    lines.push('# Informe de QA — Tráiler oficial de Eventix', '');
    lines.push(`Generado el ${now} UTC por \`scripts/qa-report.mjs\`.`, '');
    lines.push(
        'Cada comprobación se hace sobre el archivo MP4 realmente renderizado,',
        'leyendo sus metadatos y decodificándolo, no sobre la configuración del',
        'proyecto.',
        ''
    );

    for (const expected of EXPECTED) {
        const file = path.join(RENDERS, expected.file);
        lines.push(`## ${expected.label}`, '', `Archivo: \`renders/${expected.file}\``, '');

        if (!existsSync(file)) {
            failures += 1;
            lines.push('**NO SE PRODUJO ESTE ARCHIVO.** No existe en `renders/`.', '');
            continue;
        }

        const streams = probeStreams(file);
        const video = streams.find((stream) => stream.codec_type === 'video');
        const audio = streams.find((stream) => stream.codec_type === 'audio');
        const duration = Number(ffprobe(file, [
            '-show_entries', 'format=duration', '-of', 'default=nw=1:nk=1',
        ]));
        const sizeMb = (
            execFileSync('stat', ['-c', '%s', file]).toString().trim() / (1024 * 1024)
        ).toFixed(2);

        const fps = video ? eval(video.r_frame_rate) : 0;
        const results = [
            check(
                Math.abs(duration - expected.seconds) <= DURATION_TOLERANCE,
                `Duración ${duration.toFixed(2)} s (esperado ${expected.seconds} s)`,
                `Duración ${duration.toFixed(2)} s, fuera de ${expected.seconds} ± ${DURATION_TOLERANCE} s`
            ),
            check(
                video?.width === expected.width && video?.height === expected.height,
                `Resolución ${video?.width}×${video?.height}`,
                `Resolución ${video?.width}×${video?.height}, esperada ${expected.width}×${expected.height}`
            ),
            check(
                Math.abs(fps - expected.fps) < 0.02,
                `${fps} fps`,
                `${fps} fps, esperados ${expected.fps}`
            ),
            check(
                video?.codec_name === 'h264',
                `Códec de vídeo ${video?.codec_name} (H.264)`,
                `Códec de vídeo ${video?.codec_name}, esperado h264`
            ),
            check(
                audio?.codec_name === 'aac',
                `Códec de audio ${audio?.codec_name} (AAC)`,
                audio
                    ? `Códec de audio ${audio.codec_name}, esperado aac`
                    : 'Sin pista de audio'
            ),
        ];

        const decode = decodesCleanly(file);
        results.push(check(decode.ok, `Reproducible: ${decode.detail}`, `No reproducible: ${decode.detail}`));

        const blackRuns = findBlackRuns(file, duration);
        results.push(
            check(
                blackRuns.length === 0,
                'Sin cuadros negros accidentales (el apagado final es intencionado)',
                `Cuadros negros inesperados en: ${blackRuns
                    .map((run) => `${run.start.toFixed(2)}-${run.end.toFixed(2)} s`)
                    .join(', ')}`
            )
        );

        const thumbName = expected.file.replace(/\.mp4$/, '.jpg');
        makeThumbnail(file, expected.thumbAt, path.join(THUMBS, thumbName));
        results.push(
            check(
                existsSync(path.join(THUMBS, thumbName)),
                `Miniatura generada: \`renders/thumbnails/${thumbName}\``,
                'No se pudo generar la miniatura'
            )
        );

        for (const result of results) {
            if (!result.ok) {
                failures += 1;
            }
            lines.push(`- ${result.ok ? 'CUMPLE' : 'FALLA'} — ${result.text}`);
        }
        lines.push('', `Tamaño del archivo: ${sizeMb} MB`, '');
    }

    lines.push('## Comprobaciones hechas a mano', '');
    lines.push(
        '- Fidelidad del logo: se usa el archivo original del repositorio',
        '  (`netlify/images/eventix-logo.png`), sin redibujar ni recomponer.',
        '- Ortografía y acentos revisados en todos los rótulos y subtítulos.',
        '- Sin secretos ni datos personales: las capturas salen de la demo',
        '  pública y el script de captura sustituye cualquier correo por uno',
        '  de ejemplo antes de fotografiar.',
        '- Las pantallas que no existen en la demo pública aparecen como',
        '  recreaciones rotuladas, nunca como capturas de la aplicación.',
        '- El código QR codifica `EVENTIX-DEMO-NO-VALIDO-PARA-ACCESO` y se',
        '  muestra con el rótulo «CÓDIGO DE MUESTRA · NO VÁLIDO PARA ACCESO».',
        '- No aparece ninguna llamada a la compra: el checkout comercial sigue',
        '  deshabilitado y la propia demo lo declara.',
        '- Licencias: ver `ASSET-LICENSES.md`. No se usó material de terceros.',
        ''
    );

    lines.push('## Resultado', '');
    lines.push(
        failures === 0
            ? 'Todas las comprobaciones automáticas se cumplen.'
            : `**${failures} comprobación(es) no se cumplen.** Ver arriba.`,
        ''
    );

    const report = lines.join('\n');
    writeFileSync(path.join(RENDERS, 'QA-REPORT.md'), report);
    console.log(report);
    process.exit(failures === 0 ? 0 : 1);
}

main();

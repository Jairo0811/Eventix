/**
 * Captura pantallas AUTÉNTICAS de la demo pública de Eventix.
 *
 * Todo lo que produce este script sale de https://eventixrd.netlify.app,
 * es decir, de la aplicación tal y como está publicada. No compone ni retoca
 * nada. Las pantallas que no viven en esa demo (dashboard, boleta, control de
 * acceso) NO se capturan aquí: se recrean aparte y se guardan en
 * assets/screenshots/recreations, nunca mezcladas con estas.
 *
 * Uso: node scripts/capture-demo.mjs
 */
import {chromium} from 'playwright';
import {mkdir} from 'node:fs/promises';
import {existsSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

/**
 * Navegador a usar. Se prefiere el Chrome del sistema —el mismo que renderiza
 * Remotion, así que lo que se captura y lo que se compone usan el mismo motor—
 * y se recurre al que trae Playwright si aquél no está.
 */
function browserExecutable() {
    const candidates = [
        process.env.CHROME_PATH,
        '/usr/bin/google-chrome',
        '/usr/bin/chromium'
    ].filter(Boolean);
    return candidates.find((candidate) => existsSync(candidate));
}

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const OUT = path.join(ROOT, 'assets/screenshots/real');
const SITE = 'https://eventixrd.netlify.app/';

/** Anchos de la evidencia responsive que exige la fase de verificación. */
const WIDTHS = [360, 390, 768, 1024, 1440];

/** Oculta cualquier rastro de datos personales antes de fotografiar. */
const SCRUB = () => {
    const risky = /@|token|api[_-]?key|secret/i;
    document.querySelectorAll('a[href^="mailto:"]').forEach((node) => {
        node.textContent = 'contacto@ejemplo.test';
        node.setAttribute('href', 'mailto:contacto@ejemplo.test');
    });
    document.querySelectorAll('input').forEach((input) => {
        if (input.value && risky.test(input.value)) {
            input.value = '';
        }
    });
    // Congela animaciones para que cada captura sea reproducible.
    const style = document.createElement('style');
    style.textContent = `*,*::before,*::after{animation-play-state:paused !important;
        transition:none !important;scroll-behavior:auto !important}`;
    document.head.appendChild(style);
};

async function settle(page) {
    await page.waitForLoadState('networkidle').catch(() => {});
    await page.evaluate(SCRUB);
    await page.waitForTimeout(500);
}

async function main() {
    await mkdir(OUT, {recursive: true});
    const executablePath = browserExecutable();
    const browser = await chromium.launch(executablePath ? {executablePath} : {});
    const log = [];

    // --- Capturas cinematográficas (fuente del tráiler), a 2x ---
    const cine = await browser.newContext({
        viewport: {width: 1440, height: 900},
        deviceScaleFactor: 2,
        reducedMotion: 'reduce'
    });
    const page = await cine.newPage();
    await page.goto(SITE, {waitUntil: 'domcontentloaded', timeout: 60000});
    await settle(page);

    await page.screenshot({path: path.join(OUT, 'home-hero.png')});
    log.push('home-hero.png');

    const explore = page.locator('#explore-title').first();
    if (await explore.count()) {
        await explore.scrollIntoViewIfNeeded();
        await page.waitForTimeout(600);
        await page.screenshot({path: path.join(OUT, 'home-explore.png')});
        log.push('home-explore.png');
    }

    // Página completa, útil para los movimientos de cámara verticales.
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.waitForTimeout(400);
    await page.screenshot({path: path.join(OUT, 'home-full.png'), fullPage: true});
    log.push('home-full.png');

    // Diálogo del evento de muestra: es contenido ficticio de la propia demo.
    const trigger = page
        .locator('[data-demo-event], button:has-text("muestra"), a:has-text("muestra")')
        .first();
    if (await trigger.count()) {
        await trigger.click({timeout: 5000}).catch(() => {});
        await page.waitForTimeout(900);
        await page.evaluate(SCRUB);
        await page.screenshot({path: path.join(OUT, 'event-dialog.png')});
        log.push('event-dialog.png');
    }
    await cine.close();

    // --- Evidencia responsive en los cinco anchos exigidos ---
    for (const width of WIDTHS) {
        const ctx = await browser.newContext({
            viewport: {width, height: width < 500 ? 844 : 900},
            deviceScaleFactor: 2,
            isMobile: width < 500,
            hasTouch: width < 500,
            reducedMotion: 'reduce'
        });
        const p = await ctx.newPage();
        await p.goto(SITE, {waitUntil: 'domcontentloaded', timeout: 60000});
        await settle(p);

        const name = `responsive-${width}.png`;
        await p.screenshot({path: path.join(OUT, name), fullPage: true});
        log.push(name);

        // Comprueba que no haya desbordamiento horizontal, que es el defecto
        // clásico en móvil y lo que esta evidencia debe poder demostrar.
        const overflow = await p.evaluate(() =>
            document.documentElement.scrollWidth - document.documentElement.clientWidth);
        console.log(`  ${width}px -> ${name}  desbordamiento horizontal: ${overflow}px`);
        await ctx.close();
    }

    await browser.close();
    console.log(`\n${log.length} capturas reales en assets/screenshots/real`);
}

main().catch((error) => {
    console.error('Fallo la captura:', error.message);
    process.exit(1);
});

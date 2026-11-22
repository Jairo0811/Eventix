/**
 * Read-only acceptance smoke for public Eventix demos.
 * - No authentication, forms, POST, checkout, or real payments.
 * - Does not claim to certify production: captures evidence for manual review.
 * Run only against the two explicit demo hosts below.
 */
import assert from 'node:assert/strict';
import { mkdir, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { chromium } from 'playwright';

const renderUrl = 'https://eventix-n09c.onrender.com';
const netlifyUrl = 'https://eventixrd.netlify.app';
const out = resolve(process.env.EVENTIX_PUBLIC_SMOKE_OUTPUT ?? 'target/public-demo-evidence');
const widths = [360, 390, 768, 1024, 1440];
const evidence = [];
const errors = [];
const browser = await chromium.launch({ headless: true });
await mkdir(out, { recursive: true });

async function gotoWithColdStart(page, url) {
    // Render free instances may need to wake up. Never POST to awaken a demo.
    let lastError;
    for (let attempt = 1; attempt <= 3; attempt += 1) {
        try {
            const response = await page.goto(url, {
                waitUntil: 'domcontentloaded',
                timeout: 100_000
            });
            assert.equal(response?.status(), 200, 'HTTP 200 required: ' + url);
            return;
        } catch (error) {
            lastError = error;
            process.stderr.write('GET attempt ' + attempt + ' unsuccessful: ' + url + '\n');
        }
    }
    throw lastError;
}

function onApprovedHost(page, base) {
    assert.equal(new URL(page.url()).origin, base, 'Unexpected redirect or origin');
}

async function capture(page, base, slug, path, heading) {
    for (const width of widths) {
        await page.setViewportSize({ width, height: 900 });
        await gotoWithColdStart(page, base + path);
        onApprovedHost(page, base);
        await page.locator('main h1').first().waitFor({ state: 'visible' });
        if (heading) assert.match(await page.locator('main h1').first().innerText(), heading);
        await page.evaluate(async () => { await document.fonts.ready; });
        const dimensions = await page.evaluate(() => ({
            innerWidth: window.innerWidth,
            scrollWidth: document.documentElement.scrollWidth,
            clientWidth: document.documentElement.clientWidth
        }));
        assert.ok(dimensions.scrollWidth <= dimensions.clientWidth + 1,
            'Horizontal document overflow on ' + slug + ' at ' + width + 'px');
        const filename = slug + '-' + width + '.png';
        await page.screenshot({
            path: resolve(out, filename),
            fullPage: true,
            animations: 'disabled',
            timeout: 30_000
        });
        evidence.push({ base, path, width, filename, ...dimensions });
        process.stdout.write('PASS public GET + screenshot ' + slug + ': ' + width + 'px\n');
    }
}

try {
    const context = await browser.newContext({
        viewport: { width: 1440, height: 900 },
        locale: 'es-DO',
        timezoneId: 'America/Santo_Domingo',
        reducedMotion: 'reduce',
        colorScheme: 'light',
        serviceWorkers: 'block'
    });
    const page = await context.newPage();
    page.on('pageerror', error => errors.push(error.message.slice(0, 350)));

    await gotoWithColdStart(page, renderUrl + '/');
    onApprovedHost(page, renderUrl);
    assert.match(await page.locator('body').innerText(), /DEMOSTRACIÓN/);
    assert.match(await page.locator('main h1').first().innerText(), /Explora Eventix con eventos de demostración/);
    assert.equal(await page.locator('meta[name="robots"][content="noindex, nofollow"]').count(), 1);
    assert.equal(await page.locator('a[href^="/login"]').count(), 0);
    assert.match(await page.locator('main').innerText(), /Eventix Tech Experience/);
    await capture(page, renderUrl, 'render-home', '/', /Explora Eventix/);

    await gotoWithColdStart(page, renderUrl + '/e');
    assert.match(await page.locator('body').innerText(), /no se realizan cobros reales/);
    assert.equal(await page.locator('.home-event-card').count(), 3, 'Render must show exactly three fictional cards');
    assert.equal(await page.locator('select#category option').count(), 4,
        'Three fictitious categories plus unfiltered');
    assert.equal(await page.locator('meta[name="robots"][content="noindex, nofollow"]').count(), 1);
    const detailLink = await page.locator('.home-event-card .public-card-link').first().getAttribute('href');
    assert.match(detailLink ?? '', /^\/e\/\d+$/);
    await capture(page, renderUrl, 'render-catalog', '/e', /Encuentra tu próximo evento/);

    await gotoWithColdStart(page, renderUrl + '/e?term=Eventix%20Tech');
    assert.equal(await page.locator('.home-event-card').count(), 1,
        'The title search must select one event');
    await gotoWithColdStart(page, renderUrl + detailLink);
    assert.match(await page.locator('body').innerText(), /Solo demostración/);
    assert.equal(await page.locator('meta[name="robots"][content="noindex, nofollow"]').count(), 1);
    assert.equal(await page.locator('a[href*="/my/checkout"]').count(), 0);
    await capture(page, renderUrl, 'render-detail', detailLink, /DEMO/);

    const health = await page.request.get(renderUrl + '/actuator/health', { timeout: 100_000 });
    assert.equal(health.status(), 200, 'Render health must be HTTP 200');

    await gotoWithColdStart(page, netlifyUrl + '/');
    onApprovedHost(page, netlifyUrl);
    assert.match(await page.locator('main h1').first().innerText(), /Descubre eventos y nuevas experiencias/);
    assert.equal(await page.locator('meta[name="robots"][content="noindex, nofollow"]').count(), 1);
    assert.equal(await page.locator('[data-demo-event]').count(), 4);
    assert.equal(await page.locator('form').count(), 0, 'Static Netlify demo may not process forms');
    assert.match(await page.locator('body').innerText(), /Sin compras habilitadas/);
    await capture(page, netlifyUrl, 'netlify-home', '/', /Descubre eventos/);

    await page.locator('#demo-search').fill('Festival de música');
    await page.locator('[data-demo-search]').click();
    assert.equal(await page.locator('[data-demo-event]:visible').count(), 1,
        'Static search must filter illustrative cards without API calls');
    await page.locator('#demo-search').fill('palabra-sin-eventos-987');
    await page.locator('[data-demo-search]').click();
    assert.equal(await page.locator('#demo-empty:visible').count(), 1);
    await page.locator('[data-demo-reset]').click();
    assert.equal(await page.locator('[data-demo-event]:visible').count(), 4);
    await page.locator('[data-demo-details]').first().click();
    assert.equal(await page.locator('#demo-dialog[open]').count(), 1);
    await page.locator('[data-demo-close]').click();
    assert.equal(await page.locator('#demo-dialog[open]').count(), 0);

    assert.deepEqual(errors, [], 'Uncaught browser errors on public demos');
    process.stdout.write('PASS: public smoke finished, no logins, writes or payments attempted.\n');
} finally {
    await writeFile(resolve(out, 'manifest.json'), JSON.stringify({
        checkedAt: new Date().toISOString(),
        commit: process.env.GITHUB_SHA ?? null,
        publicEndpoints: [renderUrl, netlifyUrl],
        viewports: widths,
        screenshots: evidence,
        browserErrors: errors,
        paymentGatewayExercised: false,
        authenticatedAccountExercised: false,
        dataMutated: false,
        screenshotsReviewedManually: false,
        deploymentLiveCommitVerified: false
    }, null, 2));
    await browser.close();
}

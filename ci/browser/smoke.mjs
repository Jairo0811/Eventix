/**
 * CI-only browser evidence on the temporary Docker Compose application.
 * Never use against production: credentials and account mutations are isolated to CI.
 */
import assert from 'node:assert/strict';
import { mkdir, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { chromium } from 'playwright';

const baseUrl = process.env.EVENTIX_BROWSER_BASE_URL ?? 'http://127.0.0.1:8080';
const initialPassword = process.env.EVENTIX_BOOTSTRAP_ADMIN_PASSWORD;
const nextPassword = process.env.EVENTIX_BROWSER_NEXT_PASSWORD;
const outputDir = resolve(process.env.EVENTIX_BROWSER_OUTPUT ?? 'target/browser-evidence');
const viewports = [360, 390, 768, 1024, 1440];
const snapshots = [];
const browserErrors = [];

if (!initialPassword || !nextPassword || initialPassword === nextPassword) {
    throw new Error('Isolated CI administrator passwords must be configured and distinct.');
}
if (!/^https?:\/\/(?:localhost|127\.0\.0\.1):8080\/?$/.test(baseUrl)) {
    throw new Error('Browser smoke tests only accept the local CI application on port 8080.');
}
await mkdir(outputDir, { recursive: true });

const browser = await chromium.launch({ headless: true });
try {
    const context = await browser.newContext({
        viewport: { width: 1440, height: 900 },
        deviceScaleFactor: 1,
        locale: 'es-DO',
        timezoneId: 'America/Santo_Domingo',
        colorScheme: 'light',
        reducedMotion: 'reduce'
    });
    const page = await context.newPage();
    page.on('pageerror', error => browserErrors.push({
        url: page.url().split('?')[0],
        message: error.message.slice(0, 400)
    }));

    async function openPage(path, heading) {
        const response = await page.goto(new URL(path, baseUrl).toString(), {
            waitUntil: 'domcontentloaded',
            timeout: 45_000
        });
        assert.equal(response?.status(), 200, 'Expected HTTP 200 for ' + path);
        const title = page.locator('main h1').first();
        await title.waitFor({ state: 'visible', timeout: 20_000 });
        assert.match(await title.innerText(), heading, 'Unexpected heading at ' + path);
        await page.evaluate(async () => { await document.fonts.ready; });
    }

    async function capture(slug, path, heading) {
        for (const width of viewports) {
            await page.setViewportSize({ width, height: 900 });
            await openPage(path, heading);
            const filename = slug + '-' + width + '.png';
            await page.screenshot({
                path: resolve(outputDir, filename),
                fullPage: true,
                animations: 'disabled'
            });
            const metrics = await page.evaluate(() => ({
                viewport: window.innerWidth,
                documentWidth: document.documentElement.scrollWidth,
                visibleWidth: document.documentElement.clientWidth,
                pageTitle: document.title
            }));
            snapshots.push({ route: path, viewport: width, filename, ...metrics });
            process.stdout.write('Captured ' + slug + ' at ' + width + 'px\n');
        }
    }

    // Public pages and discovery never create or pay for tickets.
    await capture('home', '/', /Compra boletas/);
    await capture('catalogo', '/e', /Encuentra tu próximo evento/);
    await capture('login', '/login', /Inicia sesión/);
    await page.setViewportSize({ width: 1440, height: 900 });
    await openPage('/', /Compra boletas/);
    await page.locator('#home-term').fill('consulta-sin-coincidencias-browser-ci');
    await page.locator('.home-hero-search button[type="submit"]').click();
    await page.waitForURL(url => url.pathname === '/e' && url.searchParams.has('term'));
    assert.equal(
        await page.locator('main h1').first().innerText(),
        'Encuentra tu próximo evento'
    );

    for (const path of ['/dashboard', '/my/checkout/events/999999']) {
        await page.goto(new URL(path, baseUrl).toString(), {
            waitUntil: 'domcontentloaded'
        });
        assert.equal(
            new URL(page.url()).pathname,
            '/login',
            'Anonymous user must not access ' + path
        );
    }

    // The administrator exists only in the ephemeral SQL Server used by CI.
    await openPage('/login', /Inicia sesión/);
    await page.locator('#username').fill('admin');
    await page.locator('#password').fill(initialPassword);
    await page.locator('form[action$="/login"] button[type="submit"]').click();
    await page.waitForURL(url => url.pathname === '/auth/change-password', {
        timeout: 20_000
    });
    await page.locator('#currentPassword').fill(initialPassword);
    await page.locator('#newPassword').fill(nextPassword);
    await page.locator('#confirmPassword').fill(nextPassword);
    const passwordPostResponse = page.waitForResponse(response =>
        new URL(response.url()).pathname === '/auth/change-password'
        && response.request().method() === 'POST',
        { timeout: 20_000 }
    );
    await page.locator('form[action$="/auth/change-password"] button[type="submit"]').click();
    const changeResponse = await passwordPostResponse;
    await page.waitForLoadState('domcontentloaded');
    const changeUrl = new URL(page.url());
    if (changeUrl.pathname !== '/login' || !changeUrl.searchParams.has('passwordChanged')) {
        const feedback = await page.locator('[role="alert"], .invalid-feedback')
            .allTextContents();
        throw new Error(
            'CI password-change flow failed: HTTP ' + changeResponse.status()
            + '; route ' + changeUrl.pathname + changeUrl.search
            + '; validation: ' + feedback.map(s => s.trim()).filter(Boolean).join(' | ').slice(0, 400)
        );
    }
    await page.locator('#username').fill('admin');
    await page.locator('#password').fill(nextPassword);
    await page.locator('form[action$="/login"] button[type="submit"]').click();
    await page.waitForURL(url => url.pathname === '/dashboard', { timeout: 20_000 });
    await openPage('/dashboard', /Hola/);

    // Verify the mobile menu and focus restoration on an actual browser.
    await page.setViewportSize({ width: 390, height: 844 });
    await openPage('/dashboard', /Hola/);
    const toggle = page.locator('[data-sidebar-toggle]');
    assert.equal(await toggle.getAttribute('aria-expanded'), 'false');
    await toggle.click();
    assert.equal(await toggle.getAttribute('aria-expanded'), 'true');
    await page.locator('[data-sidebar-close]').waitFor({ state: 'visible' });
    await page.keyboard.press('Escape');
    assert.equal(await toggle.getAttribute('aria-expanded'), 'false');
    assert.equal(
        await toggle.evaluate(element => document.activeElement === element),
        true
    );
    await toggle.click();
    await page.locator('[data-sidebar-close]').click();
    assert.equal(await toggle.getAttribute('aria-expanded'), 'false');

    await capture('dashboard-administrador', '/dashboard', /Hola/);
    await capture('reportes-administrador', '/reports', /Reportes y estadísticas/);

    await writeFile(resolve(outputDir, 'manifest.json'), JSON.stringify({
        generatedAt: new Date().toISOString(),
        source: 'Ephemeral GitHub Actions Docker Compose environment',
        commit: process.env.GITHUB_SHA ?? null,
        viewports,
        snapshots,
        browserErrors,
        unauthenticatedCheckout: 'redirect-to-login',
        authenticatedRole: 'ADMINISTRATOR',
        paymentGatewayExercised: false,
        reservedSeatFlowExercised: false,
        screenshotsReviewedManually: false
    }, null, 2));
    process.stdout.write(
        'Browser smoke PASS: ' + snapshots.length + ' screenshots recorded.\n'
    );
} finally {
    await browser.close();
}

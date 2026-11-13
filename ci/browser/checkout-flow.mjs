/**
 * Full, non-production demonstration: admin creates a reserved-seat event,
 * buyer acquires a seat hold, the simulated CARDNET gateway approves,
 * and the generated ticket's QR/PDF endpoints return real bytes.
 *
 * Invoked only from ci/browser/smoke.mjs against ephemeral localhost:8080.
 * No secrets, seeded accounts, business event records, or payment operations
 * from this module should ever target an external deployment.
 */
import assert from 'node:assert/strict';

const EVENT_NAME = 'CI Eventix Tech Experience · compra simulada';
const CATEGORY_NAME = 'CI Experiencias de prueba';
const VENUE_NAME = 'Arena Eventix CI — recinto temporal';

function pathFromHref(href) {
    assert.ok(href, 'Expected an href from the real admin interface');
    return new URL(href, 'http://127.0.0.1:8080').pathname;
}

async function navigate(page, path, title) {
    const response = await page.goto('http://127.0.0.1:8080' + path, {
        waitUntil: 'domcontentloaded', timeout: 45_000
    });
    assert.equal(response?.status(), 200, 'GET ' + path + ' should render');
    if (title) {
        await page.locator('main h1').first().waitFor({ state: 'visible' });
        assert.match(await page.locator('main h1').first().innerText(), title);
    }
}

async function submitAndExpect(page, submit, pattern, label) {
    await submit.click();
    try {
        await page.waitForURL(url => pattern.test(url.pathname), { timeout: 20_000 });
    } catch (error) {
        const current = new URL(page.url());
        const messages = await page.locator(
            '.invalid-feedback, .alert-danger, [role="alert"]'
        ).allTextContents();
        throw new Error(
            label + ': expected redirect to ' + pattern
            + ', got ' + current.pathname
            + '. UI feedback: ' + messages.map(m => m.trim()).filter(Boolean).join(' | ').slice(0, 550),
            { cause: error }
        );
    }
}

export async function createReservedSeatDemo(page) {
    if (process.env.GITHUB_ACTIONS !== 'true') {
        throw new Error('Demo event creation is restricted to GitHub Actions CI');
    }

    // Create a separate classification so no existing business categories are touched.
    await navigate(page, '/categories/new', /Crear categoría/);
    await page.locator('#name').fill(CATEGORY_NAME);
    await page.locator('#description').fill('Solo para pruebas automáticas sin pago real');
    await submitAndExpect(
        page, page.locator('form[action$="/categories"] button[type="submit"]'),
        /^\/categories$/, 'Create test category'
    );

    // Create a structured venue with a section, a row and real placed seats.
    await navigate(page, '/venues/new', /Crear recinto/);
    await page.locator('#name').fill(VENUE_NAME);
    await page.locator('#address').fill('Av. de Pruebas 100, laboratorio efímero');
    await page.locator('#city').fill('Santo Domingo');
    await page.locator('#countryCode').fill('DO');
    await page.locator('#timeZone').fill('America/Santo_Domingo');
    await submitAndExpect(
        page, page.locator('form[action$="/venues"] button[type="submit"]'),
        /^\/venues$/, 'Create temporary venue'
    );
    const venueRow = page.locator('table tbody tr').filter({ hasText: VENUE_NAME });
    await venueRow.waitFor({ state: 'visible' });
    const venueEdit = await venueRow.locator('a[aria-label="Editar recinto"]').getAttribute('href');
    const venueId = Number(pathFromHref(venueEdit).match(/^\/venues\/(\d+)\/edit$/)?.[1]);
    assert.ok(venueId > 0, 'Unable to get the created venue ID');

    const layout = '/venues/' + venueId + '/layout';
    await navigate(page, layout + '/sections/new', /Nueva sección/);
    await page.locator('#code').fill('VIP-CI');
    await page.locator('#name').fill('Palco de prueba');
    await page.locator('#sectionType').selectOption('RESERVED_SEATING');
    await page.locator('#capacity').fill('3');
    await page.locator('#sortOrder').fill('1');
    await submitAndExpect(
        page, page.locator('form[action$="/sections"] button[type="submit"]'),
        new RegExp('^' + layout + '$'), 'Create reserved section'
    );
    const rowLink = await page.locator('a[href$="/rows/new"]').first().getAttribute('href');
    const sectionId = Number(pathFromHref(rowLink).match(/\/sections\/(\d+)\/rows\/new$/)?.[1]);
    assert.ok(sectionId > 0, 'Unable to read the new section ID');

    const rowPath = layout + '/sections/' + sectionId + '/rows';
    await navigate(page, rowPath + '/new', /Nueva fila/);
    await page.locator('#code').fill('A');
    await page.locator('#label').fill('Fila A');
    await page.locator('#sortOrder').fill('1');
    await submitAndExpect(
        page, page.locator('form[action$="' + rowPath + '"] button[type="submit"]'),
        new RegExp('^' + layout + '$'), 'Create seat row'
    );
    const seatLink = await page.locator('a[href$="/seats/new"]').first().getAttribute('href');
    const rowId = Number(pathFromHref(seatLink).match(/\/rows\/(\d+)\/seats\/new$/)?.[1]);
    assert.ok(rowId > 0, 'Unable to read the new row ID');
    const seatsPath = rowPath + '/' + rowId + '/seats';
    for (let i = 1; i <= 3; i++) {
        await navigate(page, seatsPath + '/new', /Nuevo asiento/);
        await page.locator('#seatNumber').fill(String(i));
        await page.locator('#label').fill('A-' + i);
        await page.locator('#xPosition').fill(String(25 + i * 15));
        await page.locator('#yPosition').fill('52');
        await submitAndExpect(
            page, page.locator('form[action$="' + seatsPath + '"] button[type="submit"]'),
            new RegExp('^' + layout + '$'), 'Create seat A-' + i
        );
    }

    await navigate(page, '/events/new', /Crear evento/);
    await page.locator('#title').fill(EVENT_NAME);
    const categoryOption = page.locator('#categoryId option').filter({ hasText: CATEGORY_NAME });
    await categoryOption.waitFor({ state: 'attached' });
    await page.locator('#categoryId').selectOption(await categoryOption.getAttribute('value'));
    await page.locator('#description').fill(
        'Evento sintético con asiento reservado y pago simulado; solo existe durante la ejecución CI.'
    );
    const organizerOption = page.locator('#organizerId option').filter({ hasText: 'ci_organizer@eventix.local' });
    await organizerOption.waitFor({ state: 'attached' });
    await page.locator('#organizerId').selectOption(await organizerOption.getAttribute('value'));
    const start = new Date(Date.now() + 35 * 24 * 60 * 60 * 1000);
    const end = new Date(start.getTime() + 3 * 60 * 60 * 1000);
    await page.locator('#startAt').fill(start.toISOString().slice(0, 16));
    await page.locator('#endAt').fill(end.toISOString().slice(0, 16));
    await page.locator('#status').selectOption('DRAFT');
    await page.locator('#venue').fill(VENUE_NAME);
    await page.locator('#address').fill('Recinto temporal de CI, Santo Domingo');
    await page.locator('#capacity').fill('3');
    await page.locator('#freeEvent').selectOption('false');
    await page.locator('#basePrice').fill('500');
    await submitAndExpect(
        page, page.locator('form[action$="/events"] button[type="submit"]'),
        /^\/events\/\d+$/, 'Create synthetic event'
    );
    const eventId = Number(new URL(page.url()).pathname.match(/^\/events\/(\d+)$/)?.[1]);
    assert.ok(eventId > 0, 'Expected created event ID');

    await navigate(page, '/events/' + eventId + '/ticket-types/new', /Nuevo tipo de entrada/);
    await page.locator('#category').selectOption('VIP');
    await page.locator('#name').fill('VIP CI con asiento');
    await page.locator('#price').fill('500');
    await page.locator('#capacity').fill('3');
    await submitAndExpect(
        page, page.locator('form[action$="/events/' + eventId + '/ticket-types"] button[type="submit"]'),
        new RegExp('^/events/' + eventId + '/ticket-types$'), 'Create reserved ticket type'
    );
    const ticketRow = page.locator('table tbody tr').filter({ hasText: 'VIP CI con asiento' });
    const ticketEdit = await ticketRow.locator('a[href$="/edit"]').getAttribute('href');
    const ticketTypeId = Number(pathFromHref(ticketEdit).match(/\/ticket-types\/(\d+)\/edit$/)?.[1]);
    assert.ok(ticketTypeId > 0, 'Expected created ticket type ID');

    const seatingPath = '/events/' + eventId + '/seating';
    await navigate(page, seatingPath, /Recinto y asientos/);
    await page.locator('#seatingMode').selectOption('RESERVED_SEATING');
    await page.locator('#venueId').selectOption(String(venueId));
    await submitAndExpect(
        page, page.locator('form[action$="' + seatingPath + '"] button[type="submit"]'),
        new RegExp('^' + seatingPath + '$'), 'Assign structured venue'
    );
    await page.locator('#sectionId').selectOption(String(sectionId));
    await page.locator('#ticketTypeId').selectOption(String(ticketTypeId));
    await submitAndExpect(
        page, page.locator('form[action$="' + seatingPath + '/pricing"] button[type="submit"]'),
        new RegExp('^' + seatingPath + '$'), 'Configure section pricing'
    );
    await submitAndExpect(
        page, page.locator('form[action$="' + seatingPath + '/inventory"] button[type="submit"]'),
        new RegExp('^' + seatingPath + '$'), 'Initialize real seat inventory'
    );
    const inventoryRows = page.locator('table tbody tr').filter({ hasText: 'A-' });
    assert.ok(await inventoryRows.count() >= 3, 'Inventory must contain at least three seats');

    await navigate(page, '/events/' + eventId + '/edit', /Editar evento/);
    await page.locator('#status').selectOption('PUBLISHED');
    await submitAndExpect(
        page, page.locator('form[action$="/events/' + eventId + '"] button[type="submit"]'),
        new RegExp('^/events/' + eventId + '$'), 'Publish the synthetic event'
    );
    return { eventId, ticketTypeId, title: EVENT_NAME, venueId, sectionId, paymentProvider: 'CARDNET' };
}

export async function completeSimulatedCheckout(page, demo, capture) {
    if (process.env.GITHUB_ACTIONS !== 'true' || demo.paymentProvider !== 'CARDNET') {
        throw new Error('The test must only use simulated CARDNET within CI');
    }
    const checkoutPath = '/my/checkout/events/' + demo.eventId;
    const seatPath = checkoutPath + '/seats?ticketTypeId=' + demo.ticketTypeId;

    await navigate(page, checkoutPath, /Comprar entradas/);
    const ticketOption = page.locator('input[name="ticketTypeId"][value="' + demo.ticketTypeId + '"]');
    assert.equal(await ticketOption.getAttribute('data-seat-required'), 'true');
    await capture('checkout-demo-antes-de-seleccion', checkoutPath, /Comprar entradas/);

    await navigate(page, '/my/checkout/events/' + demo.eventId + '/seats?ticketTypeId=' + demo.ticketTypeId,
        /Seleccionar asientos/);
    const seatCheckboxes = page.locator('.seat-map-option input[name="seatIds"]:not([disabled])');
    assert.equal(await seatCheckboxes.count(), 3, 'Expected three available and placed seats');
    await capture('asientos-demo-disponibles', seatPath, /Seleccionar asientos/);

    // The seat click exercises the real controller and active hold, not a mocked DOM state.
    await page.locator('.seat-map-option input[name="seatIds"]:not([disabled])').first().check();
    await submitAndExpect(
        page, page.locator('form[action$="/seats"] button[type="submit"]'),
        new RegExp('^/my/checkout/events/' + demo.eventId + '/seats$'), 'Hold the selected seat'
    );
    await page.locator('#seat-hold-checkout-link').waitFor({ state: 'visible' });
    assert.match(await page.locator('body').innerText(), /Retención activa/);
    await page.locator('#seat-hold-checkout-link').click();
    await page.waitForURL(url => url.pathname === checkoutPath, { timeout: 20_000 });

    assert.ok((await page.locator('input[name="holdToken"]').inputValue()).length >= 16,
        'Checkout must carry the real seat hold token');
    assert.equal(await page.locator('input[name="ticketTypeId"]:checked').inputValue(),
        String(demo.ticketTypeId));
    await page.locator('#quantity').fill('1');
    await page.locator('#firstName').fill('Comprador');
    await page.locator('#lastName').fill('Pruebas');
    await page.locator('#email').fill('ci_buyer@eventix.local');
    await page.locator('#phone').fill('8095550101');
    await page.locator('#provider').selectOption('CARDNET');
    assert.match(await page.locator('body').innerText(), /Entorno de demostración/);

    await submitAndExpect(
        page, page.locator('form[data-customer-checkout-form] button[type="submit"]'),
        /^\/my\/tickets$/, 'Complete simulated purchase'
    );

    let ticketHref = null;
    for (let attempt = 0; attempt < 8; attempt++) {
        const ticketCard = page.locator('article').filter({ hasText: demo.title });
        if (await ticketCard.count() > 0) {
            ticketHref = await ticketCard.locator('a[href^="/tickets/"]').first().getAttribute('href');
            if (ticketHref) break;
        }
        await page.reload({ waitUntil: 'domcontentloaded' });
    }
    assert.ok(/^\/tickets\/\d+$/.test(pathFromHref(ticketHref)),
        'Paid simulation must issue a real associated ticket');

    const ticketPath = pathFromHref(ticketHref);
    await navigate(page, ticketPath, /Boleta|Entrada|Detalles/i);
    await page.locator('.ticket-pass-qr-image').waitFor({ state: 'visible' });
    await page.locator('.ticket-pass-qr-image').evaluate(async img => {
        if (!img.complete) await new Promise(resolve => { img.addEventListener('load', resolve, { once: true }); });
    });
    assert.equal(await page.locator('.ticket-pass-qr-image')
        .evaluate(img => img.naturalWidth > 0), true, 'Ticket QR must be rendered');

    const qr = await page.request.get('http://127.0.0.1:8080' + ticketPath + '/qr');
    assert.equal(qr.status(), 200, 'Ticket QR must be accessible to owner');
    assert.match(qr.headers()['content-type'], /image\/png/);
    assert.ok((await qr.body()).byteLength > 100, 'QR PNG should have content');

    const pdf = await page.request.get('http://127.0.0.1:8080' + ticketPath + '/pdf');
    assert.equal(pdf.status(), 200, 'Ticket PDF must be accessible to owner');
    assert.match(pdf.headers()['content-type'], /application\/pdf/);
    const bytes = await pdf.body();
    assert.equal(bytes.subarray(0, 4).toString('ascii'), '%PDF');

    await capture('boleta-demo-qr', ticketPath, /Boleta|Entrada|Detalles/i);
    return {
        eventId: demo.eventId,
        ticketTypeId: demo.ticketTypeId,
        ticketId: Number(ticketPath.match(/\d+$/)[0]),
        actualSeatHold: true,
        paymentProvider: 'CARDNET',
        paymentMode: 'SimulatedPaymentGateway (ephemeral CI only)',
        qrHttpStatus: qr.status(),
        pdfHttpStatus: pdf.status()
    };
}

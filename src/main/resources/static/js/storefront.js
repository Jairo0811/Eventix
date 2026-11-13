/* Tienda pública: compartir / copiar enlace del evento. */
(function () {
    'use strict';

    var button = document.querySelector('[data-share-link]');
    if (!button) {
        return;
    }
    var label = button.querySelector('[data-share-label]');
    var original = label ? label.textContent : '';
    var flashTimer;
    if (navigator.share && label) {
        original = 'Compartir evento';
        label.textContent = original;
    }

    function flash(text) {
        if (!label) {
            return;
        }
        label.textContent = text;
        window.clearTimeout(flashTimer);
        flashTimer = window.setTimeout(function () { label.textContent = original; }, 2000);
    }

    function fallbackCopy(url) {
        var field = document.createElement('input');
        field.value = url;
        field.setAttribute('readonly', '');
        field.style.position = 'absolute';
        field.style.left = '-9999px';
        document.body.appendChild(field);
        field.select();
        var ok = false;
        try { ok = document.execCommand('copy'); } catch (error) { ok = false; }
        document.body.removeChild(field);
        flash(ok ? '¡Enlace copiado!' : 'No se pudo copiar');
    }

    button.addEventListener('click', function () {
        var url = button.getAttribute('data-url');
        var title = button.getAttribute('data-title');
        if (navigator.share) {
            navigator.share({ title: title, url: url }).catch(function (error) {
                if (!error || error.name !== 'AbortError') {
                    fallbackCopy(url);
                }
            });
        } else if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(url).then(
                function () { flash('¡Enlace copiado!'); },
                function () { fallbackCopy(url); });
        } else {
            fallbackCopy(url);
        }
    });
}());

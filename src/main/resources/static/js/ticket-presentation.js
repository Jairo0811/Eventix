/**
 * Progressive enhancement for the private ticket QR: without the dialog API,
 * the "Ampliar" link still opens the existing authenticated QR endpoint.
 * The dialog is never opened automatically and Escape closes it natively.
 */
(() => {
    const dialog = document.querySelector("[data-ticket-qr-dialog]");
    const trigger = document.querySelector("[data-ticket-qr-open]");

    if (!dialog || !trigger || typeof dialog.showModal !== "function") {
        return;
    }

    trigger.addEventListener("click", (event) => {
        event.preventDefault();
        if (!dialog.open) {
            dialog.showModal();
        }
    });

    dialog.addEventListener("close", () => {
        trigger.focus();
    });

    dialog.addEventListener("click", (event) => {
        if (event.target === dialog) {
            dialog.close();
        }
    });
})();

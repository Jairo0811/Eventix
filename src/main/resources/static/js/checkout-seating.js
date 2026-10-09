document.addEventListener("DOMContentLoaded", () => {
    const ticketOptions = Array.from(document.querySelectorAll(
        'input[name="ticketTypeId"][data-seat-required]'));
    const reservedPanel = document.getElementById("reserved-seat-panel");
    const quantityPanel = document.getElementById("quantity-panel");
    const quantityInput = document.getElementById("quantity");
    const seatSelectionLink = document.getElementById("seat-selection-link");

    if (ticketOptions.length === 0 || !reservedPanel || !quantityPanel || !quantityInput) {
        return;
    }

    const updateControls = () => {
        const selected = ticketOptions.find(option => option.checked);
        const requiresSeat = selected?.dataset.seatRequired === "true";

        reservedPanel.hidden = !requiresSeat;
        quantityPanel.hidden = requiresSeat;
        quantityInput.required = !requiresSeat;

        if (seatSelectionLink) {
            const baseHref = seatSelectionLink.dataset.baseHref || seatSelectionLink.href;
            seatSelectionLink.href = selected
                ? `${baseHref}?ticketTypeId=${encodeURIComponent(selected.value)}`
                : baseHref;
        }
    };

    ticketOptions.forEach(option => option.addEventListener("change", updateControls));
    updateControls();
});

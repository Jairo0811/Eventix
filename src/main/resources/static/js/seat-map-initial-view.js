document.addEventListener("DOMContentLoaded", () => {
    const map = document.querySelector("[data-seat-map-scroll]");
    if (!map) return;

    // On narrow screens the arena plane is intentionally wider than the
    // scroll region. Without an initial offset, all positioned seats can be
    // off-screen, making a populated map appear empty.
    requestAnimationFrame(() => {
        if (map.scrollWidth <= map.clientWidth + 1) return;

        const options = Array.from(map.querySelectorAll(".seat-map-option"));
        const firstAvailable = options.find(option => {
            const input = option.querySelector('input[type="checkbox"]');
            return input && !input.disabled;
        });
        const targetSeat = firstAvailable || options[0];
        if (!targetSeat) return;

        const seatBounds = targetSeat.getBoundingClientRect();
        const mapBounds = map.getBoundingClientRect();
        const desiredLeft = map.scrollLeft
            + seatBounds.left - mapBounds.left
            - (map.clientWidth - seatBounds.width) / 2;

        map.scrollLeft = Math.max(
            0,
            Math.min(map.scrollWidth - map.clientWidth, desiredLeft)
        );
    });
});

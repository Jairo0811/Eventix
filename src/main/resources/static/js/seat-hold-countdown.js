document.addEventListener("DOMContentLoaded", () => {
    const countdown = document.getElementById("seat-hold-countdown");
    const checkoutLink = document.getElementById("seat-hold-checkout-link");

    if (!countdown) {
        return;
    }

    const expiresAtValue = countdown.dataset.expiresAt;
    if (!expiresAtValue) {
        return;
    }

    const expiresAt = new Date(expiresAtValue);
    if (Number.isNaN(expiresAt.getTime())) {
        countdown.textContent = "Tiempo no disponible";
        return;
    }

    let reloadScheduled = false;

    const render = () => {
        const remainingMs = expiresAt.getTime() - Date.now();
        if (remainingMs <= 0) {
            countdown.textContent = "00:00";
            countdown.classList.remove("text-success");
            countdown.classList.add("text-danger");

            if (checkoutLink) {
                checkoutLink.classList.add("disabled");
                checkoutLink.setAttribute("aria-disabled", "true");
                checkoutLink.removeAttribute("href");
            }

            if (!reloadScheduled) {
                reloadScheduled = true;
                window.setTimeout(() => window.location.reload(), 1200);
            }
            return;
        }

        const totalSeconds = Math.ceil(remainingMs / 1000);
        const minutes = Math.floor(totalSeconds / 60);
        const seconds = totalSeconds % 60;
        countdown.textContent = `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
    };

    render();
    window.setInterval(render, 1000);
});

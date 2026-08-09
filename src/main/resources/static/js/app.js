document.addEventListener("DOMContentLoaded", () => {
    const menuButton = document.querySelector("[data-sidebar-toggle]");
    menuButton?.addEventListener("click", () => {
        document.body.classList.toggle("sidebar-open");
    });

    document.querySelector("[data-print-page]")?.addEventListener(
        "click",
        () => window.print()
    );

    document.querySelectorAll("[data-confirm]").forEach((form) => {
        form.addEventListener("submit", (event) => {
            if (form.dataset.confirmed === "true") {
                return;
            }

            event.preventDefault();
            const message = form.dataset.confirm || "¿Deseas continuar?";
            if (window.Swal) {
                window.Swal.fire({
                    title: "Confirma la operación",
                    text: message,
                    icon: "warning",
                    showCancelButton: true,
                    confirmButtonColor: "#15803d",
                    cancelButtonText: "Cancelar",
                    confirmButtonText: "Sí, continuar"
                }).then((result) => {
                    if (result.isConfirmed) {
                        form.dataset.confirmed = "true";
                        form.requestSubmit();
                    }
                });
                return;
            }

            if (window.confirm(message)) {
                form.dataset.confirmed = "true";
                form.requestSubmit();
            }
        });
    });

    const priceType = document.querySelector("[data-event-price-type]");
    const priceField = document.querySelector("[data-event-price-field]");
    const priceInput = priceField?.querySelector("input");

    const updatePriceVisibility = () => {
        if (!priceType || !priceField || !priceInput) {
            return;
        }

        const freeEvent = priceType.value === "true";
        priceField.classList.toggle("d-none", freeEvent);
        priceInput.disabled = freeEvent;

        if (freeEvent) {
            priceInput.value = "0.00";
        }
    };

    priceType?.addEventListener("change", updatePriceVisibility);
    updatePriceVisibility();

    const countdown = document.querySelector(
        "[data-reservation-expires-at]"
    );
    const countdownValue = countdown?.querySelector(
        "[data-countdown-value]"
    );

    if (countdown && countdownValue) {
        const expiration = new Date(
            countdown.dataset.reservationExpiresAt
        );

        const updateCountdown = () => {
            const remainingMilliseconds = expiration.getTime() - Date.now();
            if (Number.isNaN(remainingMilliseconds)) {
                countdownValue.textContent = "No disponible";
                return true;
            }
            if (remainingMilliseconds <= 0) {
                countdownValue.textContent = "Expirada";
                window.setTimeout(() => window.location.reload(), 1000);
                return true;
            }

            const totalSeconds = Math.floor(
                remainingMilliseconds / 1000
            );
            const minutes = Math.floor(totalSeconds / 60);
            const seconds = totalSeconds % 60;
            countdownValue.textContent = `${minutes}:${String(seconds)
                .padStart(2, "0")}`;
            return false;
        };

        if (!updateCountdown()) {
            const timer = window.setInterval(() => {
                if (updateCountdown()) {
                    window.clearInterval(timer);
                }
            }, 1000);
        }
    }
});

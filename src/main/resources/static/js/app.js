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

    const mapPreview = document.querySelector("[data-google-maps-preview]");
    if (mapPreview) {
        const apiKey = mapPreview.dataset.googleMapsApiKey || "";
        const venueInput = document.querySelector("#venue");
        const addressInput = document.querySelector("#address");
        const mapsUrlInput = document.querySelector("#googleMapsUrl");
        const frame = mapPreview.querySelector("[data-google-maps-frame]");
        const placeholder = mapPreview.querySelector(
            "[data-google-maps-placeholder]"
        );
        const status = mapPreview.querySelector("[data-google-maps-status]");
        const openLink = mapPreview.querySelector("[data-google-maps-open]");

        const isAllowedMapsHost = (hostname) => {
            const host = hostname.toLowerCase();
            return host === "google.com"
                || host === "www.google.com"
                || host === "maps.google.com"
                || host === "maps.app.goo.gl"
                || host === "goo.gl";
        };

        const parseMapsQuery = (rawUrl) => {
            if (!rawUrl) {
                return "";
            }

            try {
                const url = new URL(rawUrl);
                if (url.protocol !== "https:"
                        || !isAllowedMapsHost(url.hostname)) {
                    return "";
                }

                const query = url.searchParams.get("query")
                    || url.searchParams.get("q");
                if (query) {
                    return query;
                }

                const placeMatch = url.pathname.match(/\/place\/([^/]+)/i);
                if (placeMatch?.[1]) {
                    return decodeURIComponent(placeMatch[1])
                        .replaceAll("+", " ");
                }

                const coordinateMatch = rawUrl.match(
                    /@(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)/
                );
                if (coordinateMatch) {
                    return `${coordinateMatch[1]},${coordinateMatch[2]}`;
                }
            } catch (error) {
                return "";
            }

            return "";
        };

        const fallbackQuery = () => [
            venueInput?.value.trim(),
            addressInput?.value.trim()
        ].filter(Boolean).join(", ");

        const updateMapPreview = () => {
            if (!frame || !placeholder || !status || !openLink) {
                return;
            }

            const rawMapsUrl = mapsUrlInput?.value.trim() || "";
            let allowedMapsUrl = "";

            if (rawMapsUrl) {
                try {
                    const url = new URL(rawMapsUrl);
                    if (url.protocol === "https:"
                            && isAllowedMapsHost(url.hostname)) {
                        allowedMapsUrl = rawMapsUrl;
                    }
                } catch (error) {
                    allowedMapsUrl = "";
                }
            }

            openLink.classList.toggle("d-none", !allowedMapsUrl);
            if (allowedMapsUrl) {
                openLink.href = allowedMapsUrl;
            } else {
                openLink.removeAttribute("href");
            }

            const query = parseMapsQuery(allowedMapsUrl)
                || fallbackQuery();

            if (!query) {
                frame.classList.add("d-none");
                frame.removeAttribute("src");
                placeholder.classList.remove("d-none");
                status.textContent = "Esperando una ubicación.";
                return;
            }

            if (!apiKey) {
                frame.classList.add("d-none");
                frame.removeAttribute("src");
                placeholder.classList.remove("d-none");
                status.textContent = "Configura GOOGLE_MAPS_EMBED_API_KEY para activar el mapa interactivo.";
                return;
            }

            const params = new URLSearchParams({
                key: apiKey,
                q: query,
                language: "es",
                region: "DO"
            });

            frame.src = `https://www.google.com/maps/embed/v1/place?${params}`;
            frame.classList.remove("d-none");
            placeholder.classList.add("d-none");
            status.textContent = parseMapsQuery(allowedMapsUrl)
                ? "Vista previa generada desde el enlace de Google Maps."
                : "Vista previa generada desde el lugar y la dirección.";
        };

        [venueInput, addressInput, mapsUrlInput].forEach((input) => {
            input?.addEventListener("input", updateMapPreview);
            input?.addEventListener("change", updateMapPreview);
        });

        updateMapPreview();
    }

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

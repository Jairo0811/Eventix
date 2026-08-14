document.addEventListener("DOMContentLoaded", () => {
    const menuButton = document.querySelector("[data-sidebar-toggle]");
    menuButton?.addEventListener("click", () => {
        document.body.classList.toggle("sidebar-open");
    });

    document.querySelector("[data-print-page]")?.addEventListener(
        "click",
        () => window.print()
    );

    document.querySelectorAll("form").forEach((form) => {
        form.addEventListener("submit", (event) => {
            const submitter = event.submitter;
            const message = submitter?.dataset.confirm
                || form.dataset.confirm;

            if (!message || form.dataset.confirmed === "true") {
                form.dataset.confirmed = "false";
                return;
            }

            event.preventDefault();

            const submitConfirmedForm = () => {
                form.dataset.confirmed = "true";
                if (submitter instanceof HTMLElement) {
                    form.requestSubmit(submitter);
                    return;
                }
                form.requestSubmit();
            };

            if (window.Swal) {
                window.Swal.fire({
                    title: "Confirma la operación",
                    text: message,
                    icon: "warning",
                    showCancelButton: true,
                    confirmButtonColor: "#15803d",
                    cancelButtonText: "Cancelar",
                    confirmButtonText: "Sí, continuar",
                    focusCancel: true,
                    returnFocus: true
                }).then((result) => {
                    if (result.isConfirmed) {
                        submitConfirmedForm();
                    }
                });
                return;
            }

            if (window.confirm(message)) {
                submitConfirmedForm();
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
                || host.endsWith(".google.com")
                || host === "maps.app.goo.gl"
                || host === "goo.gl";
        };

        const allowedMapsUrl = (rawUrl) => {
            if (!rawUrl) {
                return "";
            }

            try {
                const url = new URL(rawUrl);
                return url.protocol === "https:"
                    && isAllowedMapsHost(url.hostname)
                    ? rawUrl
                    : "";
            } catch (error) {
                return "";
            }
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
                    || url.searchParams.get("q")
                    || url.searchParams.get("destination");
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

        const buildOfficialEmbedUrl = (query) => {
            const params = new URLSearchParams({
                key: apiKey,
                q: query,
                language: "es",
                region: "DO"
            });
            return `https://www.google.com/maps/embed/v1/place?${params}`;
        };

        const buildBasicEmbedUrl = (query) => {
            const params = new URLSearchParams({
                q: query,
                output: "embed",
                hl: "es"
            });
            return `https://www.google.com/maps?${params}`;
        };

        const buildSearchUrl = (query) => {
            const params = new URLSearchParams({
                api: "1",
                query
            });
            return `https://www.google.com/maps/search/?${params}`;
        };

        const updateMapPreview = () => {
            if (!frame || !placeholder || !status || !openLink) {
                return;
            }

            const rawMapsUrl = mapsUrlInput?.value.trim() || "";
            const safeMapsUrl = allowedMapsUrl(rawMapsUrl);
            const parsedQuery = parseMapsQuery(safeMapsUrl);
            const query = parsedQuery || fallbackQuery();

            if (!query) {
                frame.classList.add("d-none");
                frame.removeAttribute("src");
                placeholder.classList.remove("d-none");
                openLink.classList.add("d-none");
                openLink.removeAttribute("href");
                status.textContent = "Esperando un lugar o una dirección.";
                return;
            }

            frame.src = apiKey
                ? buildOfficialEmbedUrl(query)
                : buildBasicEmbedUrl(query);
            frame.classList.remove("d-none");
            placeholder.classList.add("d-none");

            openLink.href = safeMapsUrl || buildSearchUrl(query);
            openLink.classList.remove("d-none");

            if (parsedQuery) {
                status.textContent = apiKey
                    ? "Vista previa generada desde el enlace de Google Maps."
                    : "Vista previa básica generada desde el enlace de Google Maps.";
                return;
            }

            if (safeMapsUrl && !parsedQuery) {
                status.textContent = apiKey
                    ? "El enlace compartido se conserva; la vista previa usa el lugar y la dirección."
                    : "El enlace compartido se conserva; la vista previa básica usa el lugar y la dirección.";
                return;
            }

            status.textContent = apiKey
                ? "Vista previa generada desde el lugar y la dirección."
                : "Vista previa básica generada desde el lugar y la dirección.";
        };

        frame?.addEventListener("load", () => {
            if (status && !status.textContent.includes("cargado")) {
                status.dataset.loaded = "true";
            }
        });

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

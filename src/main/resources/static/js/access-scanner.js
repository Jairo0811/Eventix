document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("[data-scan-form]");
    if (!form) {
        return;
    }

    const token = form.querySelector("[name='token']");
    const device = form.querySelector("[data-device-identifier]");
    const video = document.querySelector("[data-qr-video]");
    const startButton = document.querySelector("[data-start-scanner]");
    const stopButton = document.querySelector("[data-stop-scanner]");
    const message = document.querySelector("[data-scanner-message]");
    let stream;
    let scanning = false;

    if (device) {
        device.value = navigator.userAgent.slice(0, 120);
    }

    const stop = () => {
        scanning = false;
        stream?.getTracks().forEach((track) => track.stop());
        stream = undefined;
        video?.classList.add("d-none");
        stopButton?.classList.add("d-none");
        startButton?.classList.remove("d-none");
    };

    const detect = async (detector) => {
        while (scanning) {
            try {
                const codes = await detector.detect(video);
                if (codes.length > 0 && codes[0].rawValue) {
                    token.value = codes[0].rawValue;
                    stop();
                    form.requestSubmit();
                    return;
                }
            } catch (error) {
                message.textContent = "No se pudo leer este cuadro; mantén el QR dentro de la cámara.";
            }
            await new Promise((resolve) => window.setTimeout(resolve, 250));
        }
    };

    startButton?.addEventListener("click", async () => {
        if (!("BarcodeDetector" in window)) {
            message.textContent = "Este navegador no ofrece lectura QR nativa. Pega el contenido del QR en el campo.";
            token.focus();
            return;
        }
        try {
            const detector = new window.BarcodeDetector({formats: ["qr_code"]});
            stream = await navigator.mediaDevices.getUserMedia({
                video: {facingMode: {ideal: "environment"}},
                audio: false
            });
            video.srcObject = stream;
            await video.play();
            scanning = true;
            video.classList.remove("d-none");
            startButton.classList.add("d-none");
            stopButton.classList.remove("d-none");
            message.textContent = "Cámara activa. Enfoca el código QR.";
            detect(detector);
        } catch (error) {
            stop();
            message.textContent = "No fue posible acceder a la cámara. Revisa el permiso o usa la entrada manual.";
        }
    });

    stopButton?.addEventListener("click", stop);
    window.addEventListener("pagehide", stop);
});

document.addEventListener("DOMContentLoaded", () => {
    const menuButton = document.querySelector("[data-sidebar-toggle]");
    menuButton?.addEventListener("click", () => {
        document.body.classList.toggle("sidebar-open");
    });

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
});


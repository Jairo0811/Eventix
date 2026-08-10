(() => {
    "use strict";

    const STORAGE_KEY = "eventix-theme";
    const THEMES = new Set(["light", "dark", "system"]);
    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");

    const storedTheme = () => {
        try {
            const value = window.localStorage.getItem(STORAGE_KEY);
            return THEMES.has(value) ? value : "system";
        } catch (error) {
            return "system";
        }
    };

    const resolvedTheme = (preference) => preference === "system"
        ? (mediaQuery.matches ? "dark" : "light")
        : preference;

    const themeIcon = (preference) => {
        if (preference === "light") return "bi-sun-fill";
        if (preference === "dark") return "bi-moon-stars-fill";
        return "bi-circle-half";
    };

    const themeLabel = (preference) => {
        if (preference === "light") return "Claro";
        if (preference === "dark") return "Oscuro";
        return "Sistema";
    };

    const applyTheme = (preference) => {
        const normalized = THEMES.has(preference) ? preference : "system";
        const resolved = resolvedTheme(normalized);
        const root = document.documentElement;
        root.dataset.theme = resolved;
        root.dataset.themePreference = normalized;
        root.setAttribute("data-bs-theme", resolved);
        root.style.colorScheme = resolved;

        document.querySelectorAll("[data-theme-option]").forEach((button) => {
            const active = button.dataset.themeOption === normalized;
            button.classList.toggle("active", active);
            button.setAttribute("aria-pressed", String(active));
        });
        document.querySelectorAll("[data-theme-current-icon]").forEach((icon) => {
            icon.className = `bi ${themeIcon(normalized)}`;
        });
        document.querySelectorAll("[data-theme-current-label]").forEach((label) => {
            label.textContent = themeLabel(normalized);
        });
    };

    const saveTheme = (preference) => {
        try {
            window.localStorage.setItem(STORAGE_KEY, preference);
        } catch (error) {
            // El tema sigue funcionando aunque el storage no esté disponible.
        }
        applyTheme(preference);
    };

    const createFloatingControl = () => {
        if (document.querySelector("[data-theme-control]")) return;
        const control = document.createElement("div");
        control.className = "theme-floating-control";
        control.dataset.themeControl = "true";
        control.setAttribute("aria-label", "Tema de Eventix");
        control.innerHTML = `
            <button type="button" data-theme-option="light" aria-label="Usar tema claro" title="Claro"><i class="bi bi-sun-fill" aria-hidden="true"></i></button>
            <button type="button" data-theme-option="dark" aria-label="Usar tema oscuro" title="Oscuro"><i class="bi bi-moon-stars-fill" aria-hidden="true"></i></button>
            <button type="button" data-theme-option="system" aria-label="Usar tema del sistema" title="Sistema"><i class="bi bi-circle-half" aria-hidden="true"></i></button>`;
        document.body.appendChild(control);
    };

    const initializeControls = () => {
        createFloatingControl();
        document.querySelectorAll("[data-theme-option]").forEach((button) => {
            button.addEventListener("click", () => saveTheme(button.dataset.themeOption));
        });
        applyTheme(storedTheme());
    };

    applyTheme(storedTheme());
    mediaQuery.addEventListener("change", () => {
        if (storedTheme() === "system") applyTheme("system");
    });
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initializeControls);
    } else {
        initializeControls();
    }
})();

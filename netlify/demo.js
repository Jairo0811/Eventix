(() => {
  "use strict";

  const search = document.getElementById("demo-search");
  const category = document.getElementById("demo-category");
  const action = document.querySelector("[data-demo-search]");
  const reset = document.querySelector("[data-demo-reset]");
  const result = document.getElementById("demo-results");
  const empty = document.getElementById("demo-empty");
  const entries = Array.from(document.querySelectorAll("[data-demo-event]"));
  const chips = Array.from(document.querySelectorAll("[data-demo-filter]"));
  const dialog = document.getElementById("demo-dialog");
  const dialogTitle = document.getElementById("demo-dialog-title");
  const dialogDetail = document.getElementById("demo-dialog-detail");
  let selectedPeriod = "all";
  let lastTrigger = null;

  const currentDay = () => {
    const parts = new Intl.DateTimeFormat("en-CA", {
      timeZone: "America/Santo_Domingo", year: "numeric", month: "2-digit", day: "2-digit"
    }).formatToParts(new Date());
    const value = (name) => parts.find((p) => p.type === name)?.value || "01";
    return [Number(value("year")), Number(value("month")), Number(value("day"))];
  };

  const matchesPeriod = (dateString) => {
    if (selectedPeriod === "all") return true;
    const [year, month, day] = currentDay();
    const today = new Date(Date.UTC(year, month - 1, day));
    const eventDate = new Date(dateString + "T12:00:00Z");
    if (Number.isNaN(eventDate.getTime())) return false;
    const days = Math.round((eventDate - today) / 86400000);
    if (selectedPeriod === "today") return days === 0;
    if (selectedPeriod === "month") return eventDate.getUTCFullYear() === year && eventDate.getUTCMonth() === month - 1 && days >= 0;
    if (selectedPeriod === "weekend") {
      const fridayOffset = (5 - today.getUTCDay() + 7) % 7;
      const nextFriday = new Date(today.getTime() + fridayOffset * 86400000);
      const end = new Date(nextFriday.getTime() + 3 * 86400000);
      return eventDate >= today && eventDate >= nextFriday && eventDate < end;
    }
    return true;
  };

  const updateChips = () => {
    chips.forEach((chip) => {
      const active = chip.dataset.demoPeriod === selectedPeriod && category.value === "";
      chip.classList.toggle("is-active", active);
      if (active) chip.setAttribute("aria-current", "true");
      else chip.removeAttribute("aria-current");
    });
  };

  const apply = () => {
    const term = search.value.trim().toLocaleLowerCase("es");
    const selectedCategory = category.value;
    let visible = 0;
    entries.forEach((entry) => {
      const matchTerm = !term || (entry.dataset.search || "").toLocaleLowerCase("es").includes(term);
      const matchCategory = !selectedCategory || selectedCategory === entry.dataset.category;
      const show = matchTerm && matchCategory && matchesPeriod(entry.dataset.date || "");
      entry.hidden = !show;
      if (show) visible += 1;
    });
    empty.hidden = visible !== 0;
    result.textContent = visible === 1
      ? "1 evento ilustrativo"
      : visible + " eventos ilustrativos";
    updateChips();
  };

  action?.addEventListener("click", () => {
    apply();
    document.getElementById("explorar")?.scrollIntoView({ behavior: "smooth", block: "start" });
  });
  search?.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
      event.preventDefault();
      action?.click();
    }
  });
  category?.addEventListener("change", apply);
  chips.forEach((chip) => {
    chip.addEventListener("click", (event) => {
      if (chip.dataset.demoPeriod) {
        event.preventDefault();
        selectedPeriod = chip.dataset.demoPeriod;
        category.value = "";
        apply();
        document.getElementById("explorar")?.scrollIntoView({ behavior: "smooth", block: "start" });
      }
    });
  });
  reset?.addEventListener("click", () => {
    search.value = "";
    category.value = "";
    selectedPeriod = "all";
    apply();
    search.focus();
  });

  document.querySelectorAll("[data-demo-details]").forEach((link) => {
    link.addEventListener("click", (event) => {
      if (!dialog || typeof dialog.showModal !== "function") return;
      event.preventDefault();
      lastTrigger = link;
      dialogTitle.textContent = link.dataset.title || "Evento de demostración";
      dialogDetail.textContent = link.dataset.detail || "Este evento es un ejemplo visual de Eventix.";
      dialog.showModal();
    });
  });
  dialog?.addEventListener("close", () => {
    lastTrigger?.focus();
  });
  dialog?.addEventListener("click", (event) => {
    if (event.target === dialog) dialog.close();
  });

  apply();
})();

/* Eventix public landing links.
 * Set trailer to the actual verified public URL of the uploaded official video
 * (YouTube/Instagram/etc.). Do not publish guessed URLs or auth endpoints.
 * Enable application ONLY after the Render site has a verified public HTTPS
 * deployment, isolated persistence and a closed commercial checkout.
 */
(() => {
  "use strict";

  const publishedLinks = Object.freeze({
    trailer: "",
    application: ""
  });

  const safePublicHttps = (value) => {
    if (typeof value !== "string" || !value.trim()) return null;
    try {
      const parsed = new URL(value.trim());
      if (parsed.protocol !== "https:" ||
          parsed.username || parsed.password ||
          parsed.hostname === "localhost" ||
          parsed.hostname.endsWith(".local") ||
          parsed.hostname === "127.0.0.1" ||
          parsed.hostname === "0.0.0.0") {
        return null;
      }
      return parsed.href;
    } catch {
      return null;
    }
  };

  const configure = (id, url) => {
    const anchor = document.getElementById(id);
    const safeUrl = safePublicHttps(url);
    if (!anchor || !safeUrl) return;
    anchor.href = safeUrl;
    anchor.hidden = false;
  };

  configure("eventix-trailer-link", publishedLinks.trailer);
  configure("eventix-app-link", publishedLinks.application);
})();

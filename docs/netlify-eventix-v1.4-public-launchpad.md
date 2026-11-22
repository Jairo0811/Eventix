# Eventix v1.4.0 — Netlify: portada pública y tráiler

> **Preparado en GitHub, no publicado aún.** Este documento no certifica
> el estado del sitio público, ni las funciones de pago ni un servidor Render.

## Situación observada el 22/11/2026

En una captura compartida del proyecto Netlify `eventixrd` se ve:

- Dominio publicado: `https://eventixrd.netlify.app/`.
- Última publicación visible en ese panel: **4 de noviembre**.
- Aviso: el equipo está funcionando con **operational credits**;
  **production deploys** y Agent Runners están **paused**.
- Un intento de producción asociado a un cambio posterior aparece
  **Skipped**; una Deploy Preview aparece publicada.
- El aviso dice que los sitios existentes permanecen en línea y que los
  nuevos despliegues se reanudarán al empezar el próximo ciclo de facturación
  o tras una actualización del plan.

Estos datos provienen de esa captura, **no** de acceso directo al panel
Netlify. El estado puede cambiar y debe volverse a comprobar antes de actuar.
No se contrató ningún plan ni se aumentó un límite de gasto.

Documentación oficial de límites:
https://docs.netlify.com/manage/accounts-and-billing/billing/billing-for-credit-based-plans/how-credits-work/

## Lo implementado en el repositorio

- La portada HTML conserva el catálogo ficticio y sus filtros funcionales,
  las advertencias de DEMO y la separación con la app Java.
- Nueva franja visible `Eventix v1.4.0 · En desarrollo`, enlazada a la
  [release publicada](https://github.com/Jairo0811/Eventix/releases/tag/v1.4.0).
- Nuevo apartado `#trailer`: miniatura oficial reutilizada del proyecto
  Remotion, información del lanzamiento y características del sistema.
  No se descargó ni volvió a generar la miniatura; se copió el **blob Git
  original** a `netlify/images/eventix-trailer-poster.jpg`.
- `netlify/public-links.js` permite configurar dos enlaces HTTPS públicos:
  `trailer` (al video oficial que ya fue publicado) y `application`
  (al backend de Render solo después de verificarlo).
- Ambos enlaces están **vacíos y ocultos por defecto**, para evitar URL
  adivinadas o un botón de compra que lleve a una plataforma no validada.
- No se agregan videos MP4 al repositorio ni se consumen ancho de banda
  de Netlify innecesariamente. El video se abre desde el lugar donde ya
  fue publicado, tras configurar su URL exacta.
- No se exponen nuevas rutas de login, checkout, pagos o transacciones.
- `netlify.toml` conserva su CSP y publica solo `netlify/`.

## Paso A — Revisar el bloqueo de créditos

1. Entrar a [Netlify](https://app.netlify.com/) y abrir el equipo que posee
   `eventixrd`.
2. Abrir el apartado de **Billing / Usage** y comprobar saldo de créditos,
   fecha de renovación y el recurso que más consume.
3. En el proyecto `eventixrd` → **Deploys**, inspeccionar el último
   deploy de producción. **Skipped** no significa éxito ni publicación.
4. No usar Agent Runners mientras los créditos de creación estén pausados.
   Evitar provocar nuevos deploys repetidamente.
5. Si se desea permanecer en Free, esperar la renovación de créditos de la
   cuenta, cuya fecha exacta debe consultarse en Netlify.
6. Subir de plan o recargar créditos **solo después de aprobación expresa**
   del propietario; no es un requisito para seguir haciendo código o PR.

En planes por créditos, la documentación indica que una publicación
productiva consume créditos; las previews suelen ser gratuitas, pero un
bloqueo de cuenta puede impedir crear nuevos despliegues igualmente.

## Paso B — Dejar bien configurada la publicación

En **Project configuration** revisar:

| Ajuste | Valor esperado |
| --- | --- |
| Repositorio | `Jairo0811/Eventix` |
| Rama de producción | `main` |
| Base directory | vacío |
| Build command | vacío |
| Publish directory | `netlify` (definido en `netlify.toml`) |
| Funciones | ninguna requerida |
| Variables de secretos | no requeridas para esta portada estática |

Verificar en **Deploy File Explorer** que el contenido desplegado tiene
`index.html`, `demo.js`, `public-links.js` y
`images/eventix-trailer-poster.jpg` en la ruta publicada.

El archivo `netlify.toml` pertenece a la raíz del repositorio: no moverlo
al directorio `netlify/`.

## Paso C — Activar el enlace al tráiler

El video ya fue publicado externamente, pero su **URL exacta no figura en
el repositorio**, por lo que no se ha inventado el enlace.

Cuando se disponga del enlace público:

1. Verificar que el video oficial se reproduce sin cuenta ni permiso especial.
2. Editar `netlify/public-links.js`:
   `trailer: "https://URL-REAL-DEL-VIDEO"`.
3. Mantener `application: ""` hasta comprobar el despliegue de Render.
4. Abrir PR, validar CI y revisar la preview; fusionar a `main` solo con
   autorización y créditos suficientes.
5. Comprobar que el botón «Ver tráiler oficial» abre el video auténtico.

No hacer embeds automáticos de Instagram/TikTok/YouTube sin revisar sus
requisitos de privacidad, cookies y políticas de contenido.

## Paso D — Aceptación después de reanudar Netlify

- [ ] Despliegue de producción **Published** para el commit aprobado.
- [ ] El dominio público muestra v1.4.0 **En desarrollo**.
- [ ] Aparece la sección `#trailer` con miniatura propia.
- [ ] El enlace al video funciona, una vez configurado.
- [ ] Ningún botón de app/compra real se presenta sin URL/función validada.
- [ ] Catálogo demo: cuatro tarjetas, búsquedas, categorías y ficha.
- [ ] Pruebas a 360, 390, 768, 1024 y 1440 px, sin overflow global.
- [ ] Teclado, foco, contraste y `prefers-reduced-motion`.
- [ ] Sin formularios de compra, sin datos personales, `noindex,nofollow`
      mientras siga siendo una demostración no comercial.
- [ ] Registrar fecha, URL del deploy y captura real antes de anunciarlo.

## Cuando iniciemos Render

Netlify seguirá siendo la **presentación pública**. Spring Boot y Thymeleaf
permanecerán juntos en Render y usarán SQL Server **externo y persistente**.
Solo después del despliegue autorizado se configurará `application` en
`netlify/public-links.js`. Ninguna configuración aquí habilita pagos reales.

El siguiente bloque se realiza sobre `render.staging.yaml` y
`docs/v1.4-reconciliation-and-staging.md`, con checkout productivo cerrado.

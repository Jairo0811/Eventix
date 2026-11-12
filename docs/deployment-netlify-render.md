# Despliegue web de Eventix — Netlify y Render

> Estado: **configuraciones listas para revisión**, no una publicación productiva automática.
> La aplicación Java + Thymeleaf se ejecuta en un servidor Spring Boot; Netlify no sirve su backend.

## 1. Corregir el 404 en `eventixrd.netlify.app`

La raíz de Eventix **no contiene un `index.html` estático**: la portada real está en
`src/main/resources/templates/home/index.html` y requiere Thymeleaf ejecutándose
dentro de Spring Boot. Publicar directamente el repositorio en Netlify termina en
una página inexistente.

El repositorio incluye ahora:

- `netlify.toml`: publica exclusivamente el directorio `netlify/`.
- `netlify/index.html`: portada **informativa** y responsive, sin login ni venta
  simulados; no afirma que los pagos, tickets o Wallet estén activos.
- La portada incluye `noindex` mientras sea temporal.

Pasos **en el proyecto Netlify existente**:

1. Verificar que `eventixrd` está conectado a `Jairo0811/Eventix` y a la rama
   correcta. No es posible deducir esas conexiones desde este repositorio.
2. Fusionar el PR de despliegue **después** de que el CI pase.
3. Configurar el directorio de publicación `netlify` (o permitir que
   `netlify.toml` lo controle). Dejar vacío el comando de build.
4. Ejecutar un nuevo deploy y revisar **Deploy File Explorer**. Debe contener
   `index.html` en la raíz publicada.
5. Abrir `https://eventixrd.netlify.app/` en una sesión privada: debe aparecer
   la portada temporal, **no** el 404 de Netlify. Confirmar desde el navegador.
6. Si Netlify sigue mostrando 404, comprobar base directory, publish directory,
   branch, deploy status, dominios asociados y reglas de redirección del panel.

**Importante:** esta corrección recupera la página informativa; **no publica la
aplicación funcional**. No crear un `/* /index.html 200` que oculte rutas inexistentes
`/login`, `/my` o `/tickets` como si fueran reales.

## 2. Qué hace cada archivo de Render

| Archivo | Uso | Base de datos | Apto para ventas reales |
|---|---|---|---|
| `render.yaml` | Demo existente, plan gratuito | H2 en memoria, schema recreado | **No** |
| `render.production.yaml` | Blueprint opcional separado y de pago | SQL Server externo persistente + Flyway | **Solo tras validación** |

No sustituir la demo por producción de forma accidental.

`render.production.yaml` usa Docker, `SPRING_PROFILES_ACTIVE=prod`, auto-deploy
desactivado, un plan con recursos suficientes para una primera prueba y un disco
persistente en `/app/data` para las portadas. Un servicio y disco de ese tipo
pueden generar **costos**, por lo que no crearlos ni activarlos sin autorización.

Render permite especificar un archivo de Blueprint alternativo al predeterminado
`render.yaml`. Al crear el Blueprint, seleccionar **`render.production.yaml`**.

## 3. Requisitos antes de crear un entorno real

- **SQL Server** compatible, persistente, con backups y recuperación ensayada.
  Se necesita conectividad desde Render; configurar firewall, TLS con certificado
  verificable y credenciales con privilegios mínimos. No usar `Developer` Edition
  para cargas comerciales. No exponer el puerto 1433 libremente a Internet.
- Cadena de conexión `DB_URL` del tipo
  `jdbc:sqlserver://HOST:1433;databaseName=EventixDb;encrypt=true;trustServerCertificate=false`.
  Cambiar `HOST` por el servidor autorizado y verificar compatibilidad del TLS.
- `DB_USERNAME` y `DB_PASSWORD` para operaciones de la aplicación.
- `FLYWAY_DB_USERNAME` y `FLYWAY_DB_PASSWORD` para migraciones con rol separado.
  El perfil `prod` reutiliza `DB_URL` para Flyway si no se indica otro
  `FLYWAY_DB_URL`; no se requieren datos de SQL Server dentro del repositorio.
- `APP_BASE_URL`: URL pública HTTPS **real y comprobada** del servicio, necesaria
  para enlaces, compartir eventos y Wallet. Si no existe todavía el hostname,
  completar este valor después de asignarlo y volver a desplegar.
- `TICKETING_SIGNING_KEY_ID`, `TICKETING_SIGNING_PRIVATE_KEY` y
  `TICKETING_SIGNING_PUBLIC_KEY`: materiales Ed25519 **persistentes**,
  almacenados como secretos. Guardar y respaldar la clave de verificación;
  no regenerar con cada reinicio o se perdería la continuidad criptográfica.
- `EVENTIX_ELIGIBILITY_HMAC_SECRET`: secreto estable para auditoría de elegibilidad.
- Inicialización segura del primer administrador, sin habilitar cuentas por
  defecto en un entorno de Internet. El Blueprint deja deshabilitado el bootstrap
  administrativo; diseñar y verificar un procedimiento de alta controlada antes
  de abrir registros reales.

El Blueprint deja **apagadas** las opciones externas de cobro, Wallet, correo
y recordatorios. Activarlas requiere configuración, contratos y pruebas reales;
no asumir que un adaptador o test con mocks significa cobros habilitados.

## 4. Comprobaciones de aceptación del backend

Tras configurar de manera privada el servicio y el SQL Server:

1. Verificar el certificado TLS y el healthcheck
   `/actuator/health/readiness`; debe responder correctamente.
2. Validar las migraciones Flyway sobre una **copia** de la base de datos
   y luego planificar el despliegue sin pérdida de información.
3. Reiniciar y redesplegar el servicio: comprobar persistencia del SQL Server,
   imágenes y claves de firma.
4. Probar accesibilidad del catálogo, autenticación, roles, seguridad de
   `/my` y restricción de tickets ajenos.
5. Verificar que pagos reales, Wallet y notificaciones se mantengan desactivados
   mientras no exista su configuración definitiva.
6. Confirmar backups, rotación de credenciales, alertas y plan de recuperación.
7. Recoger evidencia de pantalla en 360, 390, 768, 1024 y 1440 px.
8. No anunciar la plataforma como productiva sin estos resultados.

## 5. Qué hacer con el dominio Netlify después

**Opción A (inmediata):** mantener `eventixrd.netlify.app` como portada
informativa temporal y dirigir al repositorio de GitHub.

**Opción B (cuando Render funcione y esté verificado):** configurar una
redirección permanente o cambiar el dominio principal a la aplicación
Spring Boot. No establecer `302` o `301` hacia un hostname adivinado o
que todavía no responde.

La redirección debe preservar explícitamente las rutas necesarias y evitar
confusiones de origen de sesión, CSRF, OAuth y Wallet. Preferir un dominio único
para todas las operaciones privadas en producción.

## 6. Publicación: lo que sigue dependiendo de los paneles

El repositorio puede preparar código, configuración y CI, pero no revela las
configuraciones actuales de **Deploys** de Netlify ni de **Environment** de
Render. La persona administradora del proyecto debe revisar los paneles,
aprobar cualquier coste, proporcionar secretos por canales seguros, activar el
hosting y verificar el resultado público.

Nunca subir contraseñas, certificados privados o tokens en commits, issues,
capturas de pantalla ni archivos de documentación.

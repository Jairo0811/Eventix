# Eligibility & Benefits — Escuelas y colegios

## Objetivo

Permitir que Eventix verifique de forma rigurosa la pertenencia de una persona a una institución y promoción escolar antes de otorgar beneficios comerciales.

## Principio de seguridad

Ningún beneficio basado en identidad o pertenencia se concede por autodeclaración. La elegibilidad debe validarse contra un padrón cargado por una institución o administrador autorizado.

## Modelo

- `SchoolInstitution`: escuela/colegio autorizado.
- `SchoolPromotion`: promoción/año de graduación dentro de una institución.
- `PromotionMember`: integrante del padrón oficial.
- `EligibilityVerification`: resultado de verificación de un usuario contra un miembro del padrón.
- `RelationshipVerification`: vínculo aprobado entre un miembro verificado y un familiar/invitado.
- `EligibilityBenefit`: privilegio configurado para un evento/grupo.

## Cédula

La cédula completa solo se utiliza como dato de entrada para verificar identidad. Para búsquedas persistentes se utiliza un HMAC SHA-256 normalizado y se conservan únicamente los últimos cuatro dígitos como referencia operativa. No debe registrarse la cédula completa en logs, auditorías ni respuestas HTTP.

La clave HMAC se configura mediante `EVENTIX_ELIGIBILITY_HMAC_SECRET` y nunca se almacena en la base de datos ni en el repositorio.

## Estados

`PENDING`, `VERIFIED`, `REJECTED`, `MANUAL_REVIEW`, `REVOKED`.

`PENDING` y `MANUAL_REVIEW` nunca otorgan beneficios.

## Importación de padrones

La institución carga un CSV/XLSX autorizado por promoción. Cada importación debe registrar fuente, operador, fecha, cantidad de filas y checksum del archivo. Los registros duplicados o inválidos deben rechazarse o reportarse explícitamente.

## Familiares

La cédula del familiar no demuestra parentesco. El vínculo se concede únicamente mediante aprobación de un miembro verificado o revisión administrativa. Los beneficios deben permitir un límite máximo de familiares/invitados por miembro.

## Validación en checkout

Toda decisión de elegibilidad, descuento, inventario reservado y límite de compra se vuelve a validar en backend dentro de la transacción comercial. La interfaz nunca constituye una fuente de autorización.

## Privacidad

Los padrones no son navegables ni descargables por usuarios finales. Solo perfiles institucionales/autorizados pueden administrarlos. Los datos deben minimizarse y conservarse únicamente para la finalidad de verificación asociada a eventos autorizados.
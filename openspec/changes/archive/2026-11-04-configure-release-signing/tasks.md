# Tasks

## 1. Plantilla y exclusión de secretos

- [x] 1.1 Añadir `keystore.properties` a `.gitignore` (junto a las reglas `*.jks` / `*.keystore`) y
      verificar con `git status` que un `keystore.properties` de prueba en la raíz no aparece como
      modificable para commit.
- [x] 1.2 Crear `keystore.properties.example` en la raíz con las claves `storeFile`, `storePassword`,
      `keyAlias`, `keyPassword` y valores de ejemplo (sin secretos reales); verificar que el fichero
      se lista en `git status` como nuevo para commit (sí se versiona).

## 2. `signingConfig` de `release`

- [x] 2.1 Comprobar en rojo: ejecutar `./gradlew :app:assembleRelease` sin `keystore.properties` ni
      variables de entorno y confirmar que hoy termina en verde firmando con la clave de depuración
      (comportamiento actual a corregir, sin fallar).
- [x] 2.2 Añadir en `app/build.gradle.kts` la lectura de `keystore.properties` (si existe) con
      `java.util.Properties`, con fallback a las variables de entorno `RELEASE_KEYSTORE_PATH`,
      `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`, y declarar
      `signingConfigs.release` solo cuando las cuatro credenciales están disponibles.
- [x] 2.3 Aplicar `signingConfigs.release` a `buildTypes.release.signingConfig` y lanzar
      `GradleException` con un mensaje que nombre `keystore.properties.example` cuando se configura
      una tarea de la variante `release` y faltan credenciales; verificar ejecutando
      `./gradlew :app:assembleRelease` sin credenciales y comprobando que la build falla con ese
      mensaje (rojo esperado) en vez de firmar con la clave de depuración.
- [x] 2.4 Rellenar `keystore.properties` local (no versionado) con la ruta real del keystore
      (`C:\Users\dalmu\OneDrive\keys\fraseafrase\fraseafrase.jks`) y sus credenciales; verificar
      ejecutando `./gradlew :app:assembleRelease` y `./gradlew :app:bundleRelease`, que ahora
      terminan en verde firmando con el keystore de producción (comprobar con
      `apksigner verify --print-certs` o `jarsigner -verify` sobre el artefacto generado que el
      firmante coincide con el alias configurado).
- [x] 2.5 Verificar que `./gradlew :app:assembleDebug` sigue construyendo sin requerir
      `keystore.properties` ni variables de entorno (la exigencia de credenciales es solo para
      tareas de la variante `release`).

## 3. Documentación

- [x] 3.1 Crear `docs/release.md` con los pasos para generar/ubicar el keystore de producción y
      rellenar `keystore.properties` a partir de `keystore.properties.example`; verificar siguiendo
      el documento literalmente en un `keystore.properties` de prueba y confirmando que
      `./gradlew :app:assembleRelease` tiene éxito con esos valores.
- [x] 3.2 Actualizar el punto 6 de `docs/pendientes-mvp.md` a resuelto, enlazando este cambio.

## 4. Verificación final

- [x] 4.1 Ejecutar `./gradlew check` y confirmar que termina en verde (tests, detekt y ktlint no se
      ven afectados por el cambio de configuración).

## Workflow follow-up

- Archivar el cambio (`/opsx:archive`) una vez aprobado por `android-architect` y revisado por
  `code-reviewer`, en la rama del cambio y antes de la pull request.

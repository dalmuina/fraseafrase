# Design

## Context

`app/build.gradle.kts` no declara `signingConfigs`: `buildTypes.release` se firma hoy con la clave
de depuración por defecto de AGP. El usuario ha generado ya un keystore de producción fuera del
repositorio (`fraseafrase.jks`). Ver `proposal.md` - Why.

El proyecto usa convention plugins en `build-logic/` para la configuración común de Android
(`fraseafrase.android.application.compose`), pero el `signingConfig` de `release` es específico de
`:app` (único módulo que produce APK/AAB firmable) y no se repite en ningún otro módulo, así que
vive directamente en `app/build.gradle.kts`, igual que el resto de configuración de `buildTypes` en
ese fichero.

## Goals / Non-Goals

**Goals:**
- `./gradlew assembleRelease` / `bundleRelease` produce un artefacto firmado con el keystore de
  producción cuando las credenciales están disponibles localmente.
- Ningún secreto (contraseñas, alias, ruta absoluta del keystore del usuario) queda en el repositorio.
- Mensaje de error claro y temprano (fallo de configuración, no de firma a medias) si faltan credenciales.

**Non-Goals:**
- No se configura CI para publicar `release` (no existe pipeline de publicación hoy); solo se deja
  el mecanismo de variables de entorno para que un futuro cambio de CI pueda usarlo sin reabrir este.
- No se gestiona la distribución del keystore entre máquinas (backup, Play App Signing); queda a
  criterio del usuario, fuera del repositorio.
- No cambia `applicationId`, `minifyEnabled` ni ninguna otra configuración existente de `release`.

## Decisions

- **Fichero de credenciales `keystore.properties` en la raíz del proyecto, leído desde
  `app/build.gradle.kts` con `java.util.Properties`.** Alternativa descartada: variables de entorno
  únicamente — más incómodo para desarrollo local en Windows/PowerShell y pierde la ventaja de un
  fichero autocontenido con la ruta del keystore. Se combinan ambas: el fichero es la vía principal
  en local; las variables de entorno (`RELEASE_KEYSTORE_PATH`, `RELEASE_KEYSTORE_PASSWORD`,
  `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`) sirven de alternativa si el fichero no existe, para
  no bloquear un futuro CI.
- **Claves del fichero:** `storeFile` (ruta absoluta o relativa al proyecto), `storePassword`,
  `keyAlias`, `keyPassword`. Se añade `keystore.properties.example` versionado con las cuatro claves
  y valores de ejemplo, para que el usuario sepa qué rellenar.
- **`.gitignore`:** se añade `keystore.properties` junto a las reglas `*.jks` / `*.keystore` ya
  existentes.
- **Fallo explícito sin credenciales:** si ni el fichero ni las variables de entorno están completos
  y la tarea a ejecutar pertenece a la variante `release`, `app/build.gradle.kts` lanza
  `GradleException` con un mensaje que indica qué fichero o variables rellenar, en vez de dejar que
  AGP firme `release` con la clave de depuración en silencio. Para `debug` y otras tareas (p. ej.
  `./gradlew check`) la ausencia de credenciales no falla la build: la lectura del fichero/variables
  es perezosa y solo se exige al crear el `signingConfig` de `release`, que solo se evalúa si se
  ejecuta una tarea de esa variante.
- **Dónde vive:** en `app/build.gradle.kts`, no en un convention plugin de `build-logic/`, porque
  solo `:app` necesita firmar (ADR-0010: `:app` es el único módulo que conoce todas las
  implementaciones y produce el artefacto final).
- **Documentación:** `docs/release.md` nuevo, con los pasos para generar/ubicar el keystore y
  rellenar `keystore.properties` a partir de la plantilla.

## Risks / Trade-offs

- [Riesgo] Alguien podría commitear `keystore.properties` por error → Mitigación: entra en
  `.gitignore` desde este mismo cambio; `keystore.properties.example` (sin secretos) es la única
  versión en el repo.
- [Riesgo] Ruta del keystore fuera del repo (`C:\Users\dalmu\OneDrive\keys\fraseafrase\fraseafrase.jks`)
  es específica de esta máquina → Mitigación: el fichero de propiedades es local y no versionado,
  así que cada máquina apunta a su propia ruta sin tocar `app/build.gradle.kts`.
- [Trade-off] No se usa el plugin `com.android.application` signing wizard de Android Studio (que
  escribiría directamente en `build.gradle.kts` con la ruta en claro) para no versionar secretos ni
  depender de una herramienta gráfica no reproducible en CI.

## Migration Plan

- Cambio aditivo: no toca ninguna build existente de `debug`. `release` pasa de firmarse con la
  clave de depuración a exigir credenciales explícitas; cualquier build de `release` en curso que
  dependa de la firma de depuración (no hay ninguna conocida: `app/release/app-release.aab` del
  directorio de trabajo actual no es un artefacto de distribución) deja de generarse hasta rellenar
  `keystore.properties`.
- Sin rollback especial: revertir el commit devuelve `release` a firmarse con la clave de depuración.

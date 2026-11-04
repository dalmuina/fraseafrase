# Proposal

## Why

La variante `release` no tiene `signingConfig`: Gradle la firma con la clave de depuración, lo que
impide generar un APK/AAB instalable y verificable como publicación real, y además impide instalar
`debug` y `release` a la vez con garantías de que `release` usa su propia firma. El usuario ya ha
generado el keystore de producción (`fraseafrase.jks`, fuera del repositorio). Falta cablear Gradle
para que `release` lo use sin versionar secretos. Es el punto 6 pendiente en `docs/pendientes-mvp.md`.

## What Changes

- Añadir `signingConfigs.release` en `app/build.gradle.kts`, con las credenciales (ruta del keystore,
  contraseñas y alias) leídas desde un fichero local `keystore.properties` (no versionado, fuera de
  `app/build.gradle.kts`), con variables de entorno como alternativa para un futuro CI.
- Aplicar ese `signingConfig` a `buildTypes.release`.
- Fallar la configuración con un mensaje claro si se pide ensamblar `release` y no hay credenciales
  disponibles (ni fichero ni variables de entorno), en vez de firmar con la clave de depuración.
- Documentar en `docs/release.md` cómo rellenar `keystore.properties` localmente a partir de la plantilla.
- Añadir `keystore.properties` a `.gitignore` y una plantilla `keystore.properties.example` versionada
  con las claves esperadas y valores de ejemplo.

## Capabilities

### New Capabilities

(ninguna)

### Modified Capabilities

(ninguna: no cambia ningún comportamiento observable del dominio ni de las especificaciones de
práctica. Es configuración de build/infraestructura — `skip_specs: true` en `.openspec.yaml`.)

## Impact

- `app/build.gradle.kts`: nuevo `signingConfigs.release` y su aplicación a `buildTypes.release`.
- Nuevo fichero `keystore.properties.example` (versionado) y `keystore.properties` (local, ignorado).
- `.gitignore`: añadir `keystore.properties`.
- Nueva `docs/release.md` con las instrucciones de configuración local.
- No afecta a ningún módulo de dominio, datos, plataforma, presentación ni design system.
- No afecta a ningún ADR existente; no se requiere un ADR nuevo (decisión de infraestructura de
  build, no de arquitectura de la app).

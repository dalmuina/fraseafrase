# ADR-0011: detekt 1.23 con el daemon de Gradle en JDK 21

- **Estado:** Aceptado
- **Fecha:** 2026-10-30

## Contexto
El ADR-0007 exige análisis estático con detekt y ktlint. Al validar el esqueleto:

- detekt 2.0 (`dev.detekt`) es compatible con Kotlin 2.4 y JDK 25, pero en esta fecha solo existe en versión alfa,
  y el proyecto usa únicamente versiones estables.
- detekt 1.23.8 es la última versión estable. Se ejecuta dentro del daemon de Gradle y falla con JDK 25
  (el JDK incluido en Android Studio), porque su compilador de Kotlin embebido (2.0.21) no reconoce esa versión de Java.
- Configurar `jdkHome` en las tareas de detekt, `JAVA_HOME` o `-Dorg.gradle.java.home` no resuelve el problema:
  Android Studio ya fija el JDK del daemon mediante `gradle/gradle-daemon-jvm.properties`.

## Decisión
- Se usa **detekt 1.23.8** (`io.gitlab.arturbosch.detekt`) con **`detekt-formatting`** (ktlint integrado).
- El **daemon de Gradle se ejecuta con JDK 21**, fijado en el repositorio mediante `gradle/gradle-daemon-jvm.properties`:

  ```properties
  toolchainVersion=21
  ```

  Afecta por igual a la terminal, a Android Studio y a la CI. El JDK con el que se lanza `gradlew` (launcher) es indiferente.
- La CI instala JDK 21 con `actions/setup-java`. No se configuran repositorios de descarga de toolchains.
- La configuración de detekt (`config/detekt/detekt.yml`) usa la sintaxis de 1.23 (`formatting:`) e ignora las funciones
  `@Preview` / `@PreviewLightDark` en `UnusedPrivateMember` y `MagicNumber`.

## Consecuencias
- **Positivas:** análisis estático con versiones estables y un comportamiento idéntico en todos los entornos.
- **Negativas:**
  - detekt 1.23 analiza con un parser de Kotlin 2.0. Sintaxis de Kotlin 2.2 o posterior (parámetros de contexto,
    guardas en `when`, interpolación con varios `$`) puede producir errores de análisis; se evita en el código
    mientras este ADR esté vigente.
  - Cambiar el criterio "Gradle JVM" en Android Studio reescribe el fichero y rompe detekt. Ese fichero no se modifica sin un ADR nuevo.
- **Revisión:** cuando se publique **detekt 2.0 estable**, se migra a `dev.detekt` mediante un ADR que sustituya a este y se
  vuelve a evaluar el criterio de JDK del daemon.
- **Descartado:** detekt 2.0 alfa (no estable); eliminar detekt y usar solo ktlint (se pierden las reglas de complejidad y
  olores de código).

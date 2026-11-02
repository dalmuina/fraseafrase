# Spec Delta

## Purpose

Define qué identificador de aplicación tiene cada variante de build, de modo que la build de depuración pueda instalarse junto a la de publicación sin compartir su identidad de aplicación.

## ADDED Requirements

### Requirement: La build de depuración usa un identificador propio
La variante `debug` de `:app` SHALL tener el identificador de aplicación `com.dalmuina.fraseafrase.debug`.

#### Scenario: Variante debug
- **WHEN** se evalúa la configuración de la variante `debug` de `:app`
- **THEN** el identificador de aplicación es `com.dalmuina.fraseafrase.debug`
- **Verificación:** test JVM `BuildVariantTest` (tarea 1.1), ejecutado en `testDebugUnitTest`.

### Requirement: La build de publicación conserva su identificador
La variante `release` de `:app` SHALL tener el identificador de aplicación `com.dalmuina.fraseafrase`.

#### Scenario: Variante release
- **WHEN** se evalúa la configuración de la variante `release` de `:app`
- **THEN** el identificador de aplicación es `com.dalmuina.fraseafrase`
- **Verificación:** test JVM `BuildVariantTest` (tarea 1.1), ejecutado en `testReleaseUnitTest`.

### Requirement: Los identificadores de depuración y publicación son distintos
El identificador de la variante `debug` SHALL ser distinto del de la variante `release`, de modo que ambas puedan instalarse en el mismo dispositivo.

#### Scenario: Identificadores distintos
- **WHEN** se comparan los identificadores de las variantes `debug` y `release` de `:app`
- **THEN** son distintos
- **Verificación:** derivada de los dos escenarios anteriores, que fijan cada identificador por separado.

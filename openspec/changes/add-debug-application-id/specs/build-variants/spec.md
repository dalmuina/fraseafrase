# Spec Delta

## Purpose

Define qué identificador de aplicación instala cada variante de build y qué datos de usuario comparten. Permite instalar una build de desarrollo junto a la de uso diario sin que los tests instrumentados borren el progreso de estudio.

## ADDED Requirements

### Requirement: La build de depuración usa un identificador propio
La build `debug` SHALL instalarse con el identificador de aplicación `com.dalmuina.fraseafrase.debug`. La build `release` SHALL conservar el identificador `com.dalmuina.fraseafrase`.

#### Scenario: Build de depuración instalada
- **WHEN** se genera la build `debug` de `:app`
- **THEN** el identificador de aplicación resultante es `com.dalmuina.fraseafrase.debug`

#### Scenario: Build de publicación sin cambios
- **WHEN** se genera la build `release` de `:app`
- **THEN** el identificador de aplicación resultante es `com.dalmuina.fraseafrase`

### Requirement: Las variantes no comparten datos de usuario
Cada variante SHALL guardar sus datos de usuario (progreso, tanda activa y ajustes) en su propio almacenamiento, de modo que las dos variantes puedan instalarse a la vez sin compartir ni sobrescribir la base de datos de la otra.

#### Scenario: Instalación simultánea
- **WHEN** la build `debug` y la build `release` están instaladas en el mismo dispositivo
- **THEN** cada una mantiene su propio progreso de estudio y ninguna lee ni modifica los datos de la otra

### Requirement: Los tests instrumentados de la app solo afectan a la build de depuración
Los tests instrumentados de `:app` SHALL ejecutarse contra la build `debug`. Al terminar su ejecución, la desinstalación automática SHALL afectar únicamente a la build `debug`.

#### Scenario: Ejecución de tests instrumentados
- **WHEN** se ejecutan los tests instrumentados de `:app`
- **THEN** la aplicación probada tiene el identificador `com.dalmuina.fraseafrase.debug`

#### Scenario: Progreso de la build de publicación tras los tests
- **GIVEN** la build `release` instalada con progreso de estudio guardado
- **WHEN** termina una ejecución de `connectedDebugAndroidTest`
- **THEN** la build `release` sigue instalada y conserva su progreso

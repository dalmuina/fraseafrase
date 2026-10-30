# ADR-0005: Clean Architecture modular

- **Estado:** Sustituido por [ADR-0010](0010-clean-architecture-modular-design-system-y-data-sources.md)
- **Fecha:** 2026-10-29

## Contexto
El proyecto debe ser mantenible, testeable con TDD y comprensible para la IA. Un único módulo
permite que cualquier clase dependa de cualquier otra, y esas violaciones de capas no se detectan
hasta que el código ya está acoplado.

## Decisión
Clean Architecture con **módulos Gradle** que hacen cumplir las dependencias en tiempo de compilación:

```
:app                    → punto de entrada, navegación, grafo de DI
:feature:session        → UI y ViewModel de la sesión diaria
:feature:settings       → UI y ViewModel de ajustes
:core:ui                → design system, componentes Compose comunes
:core:data              → implementaciones de repositorios (Room, DataStore, assets)
:core:platform          → adaptadores de Android: TTS, reconocimiento de voz, reloj
:core:domain            → entidades, casos de uso, interfaces (puertos). Kotlin puro.
:core:testing           → fakes, builders y utilidades de test compartidas
```

**Regla de dependencias** (solo hacia dentro):

- `:core:domain` no depende de ningún otro módulo ni de Android (`kotlin("jvm")`, sin `android.*`).
- `:core:data` y `:core:platform` dependen de `:core:domain` e implementan sus interfaces.
- `:feature:*` dependen de `:core:domain` y `:core:ui`, **nunca** de `:core:data` ni `:core:platform`.
- Solo `:app` conoce todas las implementaciones y las une mediante DI.
- Las features no dependen entre sí.

**Convenciones:**

- Un caso de uso = una clase con una única operación pública (`operator fun invoke`).
- Los repositorios se definen como interfaces en el dominio.
- Las entidades del dominio son inmutables (`data class` / `value class`), sin anotaciones de Room ni de serialización; los mapeos viven en `:core:data`.
- Los errores esperables se modelan como tipos (`sealed interface`), no como excepciones.
- El tiempo se inyecta mediante un puerto `Clock` para que la lógica dependiente de fechas sea testeable.
- La configuración de build común se centraliza en **convention plugins** (`build-logic/`) y versiones en `gradle/libs.versions.toml`.

## Consecuencias
- **Positivas:** las violaciones de capas no compilan; el dominio se prueba con tests JVM rápidos; responsabilidades claras (SRP, DIP).
- **Negativas:** más ficheros de build y algo más de código de mapeo.
- **Descartado:** módulo único con separación por paquetes (no impone las reglas).

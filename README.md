# Frase a Frase

App Android para aprender portugués europeo (pt-PT) desde español repitiendo cada día
entre 15 y 30 frases con contexto claro, autoevaluación y repetición espaciada.

## Cómo se desarrolla

- **Spec-Driven Development con OpenSpec** ([ADR-0002](docs/adr/0002-desarrollo-guiado-por-especificaciones-openspec.md)):
  ningún cambio funcional empieza sin su propuesta en `openspec/changes/`.
- **TDD obligatorio** ([ADR-0007](docs/adr/0007-estrategia-de-testing-tdd.md)): rojo → verde → refactor.
- **Clean Architecture modular** ([ADR-0010](docs/adr/0010-clean-architecture-modular-design-system-y-data-sources.md)).
- Todas las decisiones de arquitectura están en [`docs/adr/`](docs/adr/README.md).

## Módulos

```
:app                    navegación y grafo de DI
:feature:session        sesión diaria de frases
:feature:settings       ajustes
:core:designsystem      tema, tokens, motion y componentes Ds*
:core:data              data sources y repositorios
:core:platform          TTS, reconocimiento de voz, reloj
:core:domain            entidades, casos de uso y puertos (Kotlin puro)
:core:testing           fakes y utilidades de test
```

La configuración de build común vive en `build-logic/` como convention plugins:

| Plugin | Uso |
|---|---|
| `fraseafrase.android.application(.compose)` | `:app` |
| `fraseafrase.android.library(.compose)` | módulos `:core` Android |
| `fraseafrase.android.feature` | módulos `:feature:*` (añade dominio, design system, Hilt y ViewModel) |
| `fraseafrase.jvm.library` | módulos Kotlin puros |
| `fraseafrase.hilt` | Hilt con KSP |

Todos aplican detekt (con ktlint) y Kover.

## Requisitos

- Android Studio con soporte para AGP 9.3.
- JDK 17 o superior (Android Studio incluye uno).

## Comandos

| Comando | Qué hace |
|---|---|
| `./gradlew assembleDebug` | Compila el APK de depuración |
| `./gradlew test` | Tests unitarios de todos los módulos |
| `./gradlew detekt` | Análisis estático y formato |
| `./gradlew check` | Tests, detekt y lint de Android |
| `./gradlew :koverHtmlReport` | Informe de cobertura agregado (`build/reports/kover/html`) |
| `./gradlew koverVerify` | Comprueba los mínimos de cobertura |

La CI (`.github/workflows/ci.yml`) ejecuta lo mismo en cada pull request, con los avisos
del compilador tratados como errores.

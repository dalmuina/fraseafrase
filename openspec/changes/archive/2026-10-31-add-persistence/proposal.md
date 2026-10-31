# Proposal

## Why

La lógica de tandas, rachas y composición ya existe en `:core:domain`, pero no hay nada que guarde el progreso, la tanda activa ni la cola de arrastradas. Sin persistencia la app no puede usarse entre sesiones, y el catálogo de frases tampoco existe como datos reales. Este cambio introduce el almacenamiento local del catálogo, del progreso y de los ajustes, con el cierre de tanda como operación atómica.

## What Changes

- Catálogo de frases empaquetado como JSON en los assets de `:core:data`, cargado en memoria. Cada frase tiene `id`, `level` (CEFR), `topic`, `target` y `source`. Los locales (`targetLocale`, `sourceLocale`) se declaran en la raíz del JSON y se copian a cada frase al cargarla.
- Reglas de ampliación del catálogo: los ids son permanentes, no se reutilizan ni se borran, y las frases nuevas se añaden al final. Se recogen en `CLAUDE.md`, sección "No hacer".
- Estado del usuario en Room 3: progreso por frase, tanda activa y cola de arrastradas en una tabla `phrase_progress` con posiciones nullable, y una tabla `active_batch` de una sola fila. Los CHECK y los índices UNIQUE hacen cumplir la regla "una frase en un único lugar".
- Ajustes del usuario (nivel CEFR y tamaño de tanda N) en DataStore Preferences. Un valor corrupto o fuera de rango cae al valor por defecto (A1 y 20).
- Puerto `PracticeRepository` con `transact`: la transición del dominio se ejecuta dentro de una única transacción de Room, de modo que el cierre (persistir resultados, cola y tanda nueva) es atómico.
- Casos de uso `ObtainActiveBatch`, `AnswerPhrase` y `CloseActiveBatch`. Este último recibe `expectedUncertified` para que el aviso mostrado coincida con el cierre; si no coincide, devuelve `Stale`.
- Reloj del dispositivo `DeviceClock` en `:core:platform`, enlazado a `Clock` mediante Hilt en `:app`.
- Test de contrato en `:core:data` sobre el JSON real de assets (ids únicos y no vacíos, CEFR válido, textos no vacíos, topic no vacío, locales presentes).
- **Decisión de arquitectura (ADR-0013):** ADR-0013 sustituye por completo al ADR-0004 y cambia el almacenamiento del catálogo de "base de datos precargada" a JSON en memoria. ADR-0013 queda `Propuesto` durante el cambio y pasa a `Aceptado` al cerrarlo, en el mismo commit en que el ADR-0004 pasa a `Sustituido por ADR-0013`. La excepción a la regla de no editar ADR aceptados se escribe en `CLAUDE.md`: un ADR `Propuesto` puede editarse, y uno `Aceptado` solo admite el cambio de su línea de estado.
- Configuración de detekt: `ignoreAnnotated: ['Database']` en `UnnecessaryAbstractClass` dentro de `config/detekt/detekt.yml`, sin `@Suppress`. La exención se justifica en ADR-0013.
- Actualización de `openspec/config.yaml` y de la tabla de `docs/adr/README.md` para reflejar los ADR vigentes (ADR-0001 exige que `config.yaml` los referencie).
- Dependencias nuevas, con versiones en `gradle/libs.versions.toml`: Room 3.0.3 (plugin `androidx.room3`, `schemaDirectory` versionado), `sqlite-bundled` 2.7.1 (driver en producción y en tests JVM), DataStore Preferences 1.3.0, `kotlinx-serialization-json` 1.12.0 con el plugin `org.jetbrains.kotlin.plugin.serialization` 2.4.21, y KSP 2.3.13.

**Sin BREAKING changes**: no hay datos previos que migrar.

## Capabilities

### New Capabilities

- `phrase-catalog`: contenido del catálogo de frases: formato del JSON, locales, topic, nivel CEFR y reglas de ampliación (ids permanentes, sin reutilización ni borrado, alta al final).
- `practice-persistence`: persistencia del progreso, la tanda activa y la cola de arrastradas; cierre atómico de la tanda; carga de la tanda activa; coherencia de posiciones.
- `user-settings`: persistencia de nivel CEFR y tamaño de tanda N, con fallback al valor por defecto si el valor guardado es corrupto o está fuera de rango.

### Modified Capabilities

Ninguna. `practice-batch` y `spaced-repetition` no cambian sus requisitos: las reglas de composición, cierre y certificación se mantienen.

Sí queda **superada** la decisión D3 de `add-practice-batch` (las frases nuevas no se persisten hasta responderlas o cerrarlas). Este cambio persiste las frases nuevas al componer la tanda, porque la tanda activa necesita posiciones persistidas. Es un cambio de diseño, no de comportamiento observable de las specs vigentes, y queda explícito en `design.md`.

## Fuera de alcance

- Corrección por voz y reconocimiento de voz (cambio posterior, según ADR-0012).
- Política de descarte o retirada de frases del catálogo: los ids son permanentes y no se borran.
- Migraciones de Room: la base de datos empieza en versión 1 con esquema exportado.
- Sincronización, cuentas, variantes pt-BR u otros idiomas.
- UI de las features `:feature:session` y `:feature:settings`.
- Audio TTS para escuchar la frase en portugués (ADR-0012, punto 4 del MVP): cambio posterior.
- Contenido completo del catálogo A1–C1 en pt-PT (ADR-0012, punto 1). Este cambio solo incluye frases de ejemplo para validar el formato; el contenido es un cambio posterior.

## Impact

- **Módulos:** `:core:domain` (puerto `PracticeRepository`, casos de uso, `CatalogPhrase` con nuevos campos, `Topic` y `LanguageTag` como value class), `:core:data` (Room, DataStore, lectura de assets, mapeos), `:core:platform` (`DeviceClock`), `:core:testing` (fakes en memoria con `Mutex` y builders), `:app` (grafo de Hilt).
- **Build:** `gradle/libs.versions.toml`, convention plugins de `build-logic/` (exclusión de código generado de Room en Kover), `core/data/build.gradle.kts`.
- **Configuración:** `config/detekt/detekt.yml`, `CLAUDE.md` (sección "No hacer").
- **Documentación:** `docs/adr/0013-*.md` nuevo; `docs/adr/0004-*.md` solo cambia su estado a "Sustituido por 0013".
- **Dependencias:** Room 3.0.3, sqlite-bundled 2.7.1, DataStore 1.3.0, kotlinx-serialization 1.12.0, KSP 2.3.13. Justificación detallada en `design.md`.

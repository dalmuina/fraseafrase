# Frase a Frase

App Android para aprender portugués europeo (`pt-PT`) desde español: cada día el usuario practica
entre 10 y 30 frases con contexto claro, revela la traducción, se autoevalúa y las frases falladas
vuelven antes mediante repetición espaciada. Offline-first, sin backend ni IA en tiempo de ejecución.

## Reglas de oro

1. **Spec first.** Ningún código de producción sin una tarea en un `tasks.md` aprobado de `openspec/changes/`.
   Si te piden algo que no está en una tarea, propón primero un cambio con `/opsx:propose`.
2. **TDD siempre.** Rojo → verde → refactor. Escribe el test, ejecútalo y comprueba que **falla por el motivo esperado**
   antes de escribir el código. Nunca escribas test y código de producción en el mismo paso.
3. **Los ADR mandan.** Lee `docs/adr/` antes de proponer o diseñar. Si algo contradice un ADR aceptado,
   para y propón un ADR nuevo que lo sustituya; no lo ignores ni lo edites.
4. **Alcance del MVP: ADR-0009.** Lo que esté fuera es un cambio posterior, no una mejora "de paso".
5. **No amplíes el alcance.** Implementa exactamente lo que dice la tarea. Si ves algo mejorable, anótalo y avisa.

## Flujo de trabajo (OpenSpec)

| Paso | Comando | Subagente |
|---|---|---|
| Alcance poco claro | `/opsx:explore` | – |
| Proponer el cambio | `/opsx:propose` | – |
| Revisar propuesta y diseño **antes de aprobar** | – | `android-architect` |
| Implementar tareas | `/opsx:apply` | `tdd-implementer` (un grupo de tareas por invocación) |
| Revisar el código | – | `code-reviewer` |
| Verificar contra la especificación | `/opsx:verify` | – |
| Cerrar | `/opsx:archive` | – |

Especificaciones vigentes: `openspec/specs/<dominio>/spec.md`. Contexto y reglas de los artefactos: `openspec/config.yaml`.

## Arquitectura (ADR-0010)

```
:app                → navegación y grafo de DI. Único módulo que conoce todas las implementaciones.
:feature:session    → sesión diaria
:feature:settings   → ajustes
:core:designsystem  → AppTheme, tokens, motion, componentes Ds*. No depende de ningún módulo del proyecto.
:core:data          → data sources y repositorios (Room, DataStore, assets)
:core:platform      → TTS, reconocimiento de voz, reloj
:core:domain        → entidades, casos de uso, puertos. Kotlin puro, sin Android.
:core:testing       → fakes, builders, MainDispatcherExtension
```

- Dependencias solo hacia dentro. Las features dependen **solo** de `:core:domain` y `:core:designsystem`
  (las añade el plugin `fraseafrase.android.feature`; no declares dependencias de proyecto en una feature).
- Las features nunca dependen entre sí, ni de `:core:data` ni de `:core:platform`.
- Paquete base: `com.dalmuina.fraseafrase.<módulo>` (p. ej. `com.dalmuina.fraseafrase.core.domain`).

## Convenciones

**Dominio**
- Un caso de uso = una clase con `operator fun invoke`, anotada con `@Inject constructor` (javax.inject).
- Entidades inmutables (`data class` / `value class`), sin anotaciones de Room ni de serialización.
- Errores esperables como `sealed interface`, no excepciones.
- El tiempo llega por el puerto `Clock`; los dispatchers se inyectan. Nunca `System.currentTimeMillis()` ni `Dispatchers.IO` directos en lógica.
- Idiomas como etiquetas BCP-47 (`pt-PT`, `es-ES`). Ningún valor de idioma fijo en el dominio (ADR-0003).

**Datos**
- El dominio declara siempre interfaces `XxxRepository`.
- Una fuente → `<Tecnología><Entidad>DataSource` implementa directamente la interfaz (`RoomPhraseDataSource : PhraseRepository`).
- Varias fuentes → `<Estrategia><Entidad>Repository` compone data sources (`OfflineFirstProgressRepository`).
- Los mapeos entre modelos de framework y del dominio viven en `:core:data`.

**Presentación (ADR-0006)**
- Un `StateFlow<XxxUiState>` inmutable por pantalla; acciones con `sealed interface XxxAction` y un único `onAction()`.
- `XxxRoute` (conecta con el ViewModel) + `XxxScreen` (sin estado, recibe `uiState` y `onAction`).
- Los ViewModels dependen solo de casos de uso.

**Design system**
- En las features no hay colores, `dp`, tipografías ni duraciones literales: usa `MaterialTheme` y `AppTheme.spacing` / `AppTheme.motion`.
- Componentes compartidos con prefijo `Ds`, sin estado, sin tipos del dominio, `modifier: Modifier = Modifier`
  como primer parámetro opcional y `@PreviewLightDark`.
- Un componente se promociona a `:core:designsystem` cuando lo necesita una segunda feature.

**Tests (ADR-0007)**
- JUnit 6 (Jupiter), aserciones de Kotest, Turbine para `Flow`, `runTest`. MockK solo en fronteras: preferir fakes de `:core:testing`.
- Nombres en inglés con comillas invertidas: `` `given a failed phrase when building next session then it is included` ``.
- Estructura Given / When / Then, una idea por test. Cada test referencia el escenario de la especificación en un comentario.
- ViewModels: registra `MainDispatcherExtension` con `@RegisterExtension`.
- Cobertura mínima (Kover): dominio 90 %, presentación 80 %, global 70 %.

**Idioma**
- Documentación, especificaciones, ADR y comentarios explicativos en español.
- Código, identificadores, nombres de test y mensajes de commit en inglés (Conventional Commits).

## Comandos

```
./gradlew test                      # tests unitarios
./gradlew :core:domain:test         # tests de un módulo
./gradlew detekt                    # análisis estático y formato (ktlint)
./gradlew check                     # tests + detekt + lint
./gradlew koverVerify               # mínimos de cobertura
./gradlew :koverHtmlReport          # informe agregado en build/reports/kover/html
./gradlew assembleDebug             # APK de depuración
```

Antes de dar una tarea por terminada: `./gradlew check` en verde. Antes de cerrar un cambio: también `./gradlew koverVerify`.

## No hacer

- No modifiques `gradle/gradle-daemon-jvm.properties` ni el JDK del daemon (ADR-0011).
- No uses sintaxis de Kotlin 2.2+ que detekt 1.23 no analiza: parámetros de contexto, guardas en `when`, interpolación con varios `$` (ADR-0011).
- No añadas librerías sin justificarlo en el `design.md` del cambio; las versiones van en `gradle/libs.versions.toml`.
- No apliques `org.jetbrains.kotlin.android`: con AGP 9 Kotlin viene integrado en el plugin de Android.
- No desactives reglas de detekt ni bajes umbrales de cobertura para que algo pase; corrige el código o propón un ADR.
- No edites ADR aceptados ni especificaciones de `openspec/specs/` a mano: los cambios de especificación llegan por deltas de OpenSpec.

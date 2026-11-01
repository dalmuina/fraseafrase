# Frase a Frase

App Android para aprender portugués europeo (`pt-PT`) desde español. El usuario practica **tandas**
de 10 a 30 frases en una lista circular, revela la traducción y se autoevalúa. Una frase con 3 aciertos
seguidos queda certificada y se espacia con repetición espaciada; el usuario decide cuándo empezar una
tanda nueva y las no certificadas se arrastran. Reglas completas en el ADR-0012.
Offline-first, sin backend ni IA en tiempo de ejecución.

## Reglas de oro

1. **Spec first.** Ningún código de producción sin una tarea en un `tasks.md` aprobado de `openspec/changes/`.
   Si te piden algo que no está en una tarea, propón primero un cambio con `/opsx:propose`.
2. **TDD siempre.** Rojo → verde → refactor. Escribe el test, ejecútalo y comprueba que **falla por el motivo esperado**
   antes de escribir el código. Nunca escribas test y código de producción en el mismo paso.
3. **Los ADR mandan.** Lee `docs/adr/` antes de proponer o diseñar. Si algo contradice un ADR aceptado,
   para y propón un ADR nuevo que lo sustituya; no lo ignores ni lo edites.
4. **Método y alcance del MVP: ADR-0012.** Usa su terminología (tanda, nivel, racha, certificada, pendiente, arrastrada). Lo que esté fuera es un cambio posterior, no una mejora "de paso".
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
| Cerrar (en la rama del cambio, antes de la PR) | `/opsx:archive` | – |

Especificaciones vigentes: `openspec/specs/<dominio>/spec.md`. Contexto y reglas de los artefactos: `openspec/config.yaml`.

## Ramas y commits

- Cada cambio se trabaja en su propia rama, que crea el usuario desde `develop` con el formato
  `<versión>/<nombre-del-cambio>`, por ejemplo `1.0.0/add-practice-batch`. La versión es la de la
  release objetivo (actual: `1.0.0`) y el nombre coincide con el del cambio de OpenSpec.
- Antes de cada commit, comprueba con `git branch --show-current` que no estás en `develop` ni en `main`.
  Si lo estás, para y avisa.
- Los commits van a la rama actual: uno por grupo de tareas, siempre en verde y con Conventional Commits
  en inglés. No se commitean rojos ni se reescribe el historial (sin rebase, sin amend de commits ya
  publicados, sin force push).
- Al terminar el cambio, en su misma rama y antes de la PR: ejecuta `/opsx:archive` y haz un último commit con el
  archivo del cambio y las specs sincronizadas. Después haz push de la rama actual (`git push -u origin HEAD`) y
  para. El usuario abre la pull request a `develop`, la revisa con la CI y la mergea. El archivo nunca va en una
  rama aparte ni después de la integración en `develop`.
- No crees ni borres ramas, no hagas merge ni abras pull requests.

## Arquitectura (ADR-0010)

```
:app                → navegación y grafo de DI. Único módulo que conoce todas las implementaciones.
:feature:session    → práctica de la tanda activa
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

- No hagas commit, push ni merge en `develop` ni en `main`.
- No modifiques `gradle/gradle-daemon-jvm.properties` ni el JDK del daemon (ADR-0011).
- No uses sintaxis de Kotlin 2.2+ que detekt 1.23 no analiza: parámetros de contexto, guardas en `when`, interpolación con varios `$` (ADR-0011).
- No añadas librerías sin justificarlo en el `design.md` del cambio; las versiones van en `gradle/libs.versions.toml`.
- No apliques `org.jetbrains.kotlin.android`: con AGP 9 Kotlin viene integrado en el plugin de Android.
- No desactives reglas de detekt ni bajes umbrales de cobertura para que algo pase; corrige el código o propón un ADR.
- No edites ADR aceptados: solo se cambia su línea de estado a `Sustituido por ADR-XXXX` cuando un ADR nuevo lo sustituye (ver `docs/adr/README.md`). Un ADR `Propuesto` puede editarse.
- No borres ni reutilices ids del catálogo de frases, y añade las frases nuevas siempre al final de la lista `phrases` (ADR-0013; test de contrato en `:core:data`).
- No edites `openspec/specs/` a mano: los requisitos y escenarios solo cambian mediante deltas de OpenSpec; la única edición manual permitida es la sección `## Purpose`, que OpenSpec no gestiona con deltas, y siempre sin añadir comportamiento.

## Contexto y sesiones

- Las decisiones del usuario se escriben en el artefacto que corresponda (spec delta, `design.md` o `tasks.md`) antes de continuar con cualquier otra tarea.
- `.claude/handoff.md` (en `.gitignore`, nunca se commitea) se actualiza en cada parada con: cambio y rama, grupo y tarea actual, siguiente paso exacto, decisiones pendientes y notas de entorno. Tras un `/clear` o al empezar sesión, es lo primero que se lee, junto con `tasks.md` y `git log`.
- `handoff.md` lleva siempre dos campos de estado:
  - **Fase:** `explore` | `propose` | `architect` | `apply grupo N` | `review` | `verify` | `archive` | `pr`.
  - **Veredicto android-architect:** `pendiente` | `APROBADO` + fecha.
- Cada parada termina con una línea de estado de contexto:
  - ✅ Seguro para `/clear`: todo commiteado, `tasks.md` y `handoff.md` al día, nada solo en la conversación.
  - ⚠️ Mejor `/compact`: trabajo a medias o decisión todavía no escrita.
  - ⛔ No limpies: cambios sin commitear o comando en curso.
- Regla práctica: `/clear` tras el commit de un grupo; `/compact` a mitad de grupo.

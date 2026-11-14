# Frase a Frase

App Android para aprender portugués europeo (`pt-PT`) desde español. El usuario practica **tandas**
de 10 a 30 frases en una lista circular, revela la traducción y se autoevalúa. Una frase con 3 aciertos
seguidos queda certificada y se espacia con repetición espaciada; el usuario decide cuándo empezar una
tanda nueva y las no certificadas se arrastran. Reglas completas en el ADR-0012.
Offline-first, sin backend ni IA en tiempo de ejecución.

## Reglas de oro

0. **Clasifica antes de actuar.** Decide primero si la petición va por la **vía rápida** o por el **flujo OpenSpec**
   (ver "Vía rápida"). Las reglas 1 y 2 aplican al flujo OpenSpec; la vía rápida tiene sus propias reglas.
1. **Spec first.** Ningún código de producción sin una tarea en un `tasks.md` aprobado de `openspec/changes/`,
   salvo en la vía rápida. Si te piden algo que no está en una tarea y no cumple los criterios de la vía rápida,
   propón primero un cambio con `/opsx:propose`.
2. **TDD siempre que haya comportamiento.** Rojo → verde → refactor. Escribe el test, ejecútalo y comprueba que
   **falla por el motivo esperado** antes de escribir el código. Nunca escribas test y código de producción en el
   mismo paso. Excepción: los cambios puramente visuales de Compose (tamaño, forma, posición, color, espaciado)
   no exigen un test rojo previo; se verifican con `@PreviewLightDark` o un test de captura cuando aporte valor.
3. **Los ADR mandan.** Lee `docs/adr/` antes de proponer o diseñar. Si algo contradice un ADR aceptado,
   para y propón un ADR nuevo que lo sustituya; no lo ignores ni lo edites.
4. **Método y alcance del MVP: ADR-0012.** Usa su terminología (tanda, nivel, racha, certificada, pendiente, arrastrada). Lo que esté fuera es un cambio posterior, no una mejora "de paso".
5. **No amplíes el alcance.** Implementa exactamente lo que dice la tarea. Si ves algo mejorable, anótalo y avisa.

## Vía rápida (sin OpenSpec ni subagentes)

**Cuándo.** Se cumplen todas estas condiciones:
- Cambio solo visual o de layout (tamaño, forma, posición, estilo, tokens de diseño), de textos o un refactor
  sin cambio de comportamiento.
- Toca un único módulo (o una feature más `:core:designsystem`).
- No añade ni cambia lógica de dominio, casos de uso, puertos, persistencia, navegación ni escenarios de
  `openspec/specs/`.
- No contradice ningún ADR ni añade librerías.

Si dudas, pregunta al usuario antes de abrir un cambio de OpenSpec. Si durante el trabajo aparece lógica nueva,
para y pasa al flujo OpenSpec.

**Cómo.**
1. Implementa directamente en la sesión principal, sin delegar en `tdd-implementer` ni `android-architect`.
2. Respeta igualmente la arquitectura, el design system y las convenciones de este fichero.
3. Si cambia algún test existente (semántica, `testTag`), actualízalo; si el cambio tiene comportamiento
   observable (p. ej. un botón que deja de mostrarse), cúbrelo con un test.
4. Verifica con `./gradlew :<módulo>:check` de los módulos tocados.
5. Opcional: una sola ronda de `code-reviewer` si el diff no es trivial.
6. Un commit con Conventional Commits en la rama que haya creado el usuario (el hook ejecuta `./gradlew check`).
   No hay archive, así que el hook de PR no se dispara: para y deja el push y la PR al usuario.

## Flujo de trabajo (OpenSpec)

| Paso | Comando | Subagente |
|---|---|---|
| Decidir y ejecutar el siguiente paso | `/next` | – |
| Alcance poco claro | `/opsx:explore` | – |
| Proponer el cambio | `/opsx:propose` | – |
| Revisar propuesta y diseño **antes de aprobar** | – | `android-architect` |
| Implementar tareas | `/opsx:apply` | `tdd-implementer` (un grupo de tareas por invocación) |
| Revisar el código | – | `code-reviewer` |
| Verificar contra la especificación | `/opsx:verify` | – |
| Cerrar (en la rama del cambio, antes de la PR) | `/opsx:archive` | – |

Normalmente se invoca `/next`, que diagnostica en qué paso de esta tabla está el cambio y lo ejecuta;
usa los comandos `/opsx:*` sueltos solo cuando ya sabes exactamente en qué paso estás.

**Delegación en `tdd-implementer`:**
- Los grupos de tareas se ejecutan **de uno en uno**, nunca en paralelo: comparten `build/`, el daemon de Gradle
  y la caché de KSP.
- En el prompt de cada invocación pasa el nombre del cambio, los números de tarea, los módulos afectados y las
  rutas de los escenarios de `specs/**` que satisfacen esas tareas, para que el subagente no tenga que explorar.
- El subagente verifica solo los módulos tocados (`./gradlew :<módulo>:check`). El `./gradlew check` global lo
  ejecuta el hook al hacer el commit del grupo; no lo repitas a mano.

Especificaciones vigentes: `openspec/specs/<dominio>/spec.md`. Contexto y reglas de los artefactos: `openspec/config.yaml`.

## Ramas y commits

- Cada cambio se trabaja en su propia rama, que crea el usuario desde `develop` con el formato
  `<versión>/<nombre-del-cambio>`, por ejemplo `1.0.0/add-practice-batch`. La versión es la de la
  release objetivo (actual: `1.0.0`) y el nombre coincide con el del cambio de OpenSpec.
- Los commits van a la rama actual: uno por grupo de tareas, con Conventional Commits en inglés. No se reescribe
  el historial (sin rebase, sin amend de commits ya publicados, sin force push). Un hook del proyecto bloquea
  el commit si la rama es `develop`/`master` o si `./gradlew check` falla, así que no hace falta comprobarlo a mano.
- Al terminar el cambio, en su misma rama y antes de la PR: ejecuta `/opsx:archive` y haz un último commit con el
  archivo del cambio y las specs sincronizadas. El archivo nunca va en una rama aparte ni después de la
  integración en `develop`.
- Ese commit de archive (`docs(openspec): archive <cambio> and sync specs`) dispara automáticamente el hook
  `post-archive-pr.sh`: hace push de la rama actual y abre la pull request a `develop` con `gh pr create`
  (título y descripción a partir del cambio de OpenSpec). No hagas push ni `gh pr create` a mano después de ese
  commit. Después para: el usuario revisa la PR ya abierta con la CI y la mergea.
- Los artefactos de OpenSpec se commitean en cuanto el cambio recibe el APROBADO de android-architect, antes de
  `/opsx:apply`, con un commit `docs(openspec): ...` propio. Así no se pierden si se cambia de rama o se limpia el árbol.
- No crees ramas por iniciativa propia: es el usuario quien las crea. Puedes crear la rama solo si el usuario lo
  pide explícitamente en su mensaje (p. ej. "crea la rama"), y siempre con el formato `<versión>/<nombre-del-cambio>`
  de este apartado. No borres ramas ni hagas merge en ningún caso, ni siquiera a petición explícita.

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
  **Excepción en tests instrumentados (`androidTest`):** `givenX_whenY_thenZ` (camelCase con guiones bajos). El DEX de
  minSdk 26 no admite espacios en nombres de método. Ejemplo: `givenDraft_whenSaving_thenItIsPersisted`.
- Estructura Given / When / Then, una idea por test. Cada test referencia el escenario de la especificación en un comentario.
- ViewModels: registra `MainDispatcherExtension` con `@RegisterExtension`.
- Cobertura mínima (Kover): dominio 90 %, presentación 80 %, global 70 %.

**Idioma**
- Documentación, especificaciones y ADR en español.
- Código, identificadores, nombres de test, comentarios del código y mensajes de commit en inglés (Conventional Commits).

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

Antes de dar una tarea por terminada: `./gradlew :<módulo>:check` en verde en los módulos tocados; el
`./gradlew check` global lo ejecuta el hook en cada commit. Antes de cerrar un cambio: también `./gradlew koverVerify`.

Para no llenar el contexto con logs, ejecuta Gradle con `-q --console=plain` y recorta la salida
(`2>&1 | tail -n 40`). En tests instrumentados filtra siempre por clase
(`-Pandroid.testInstrumentationRunnerArguments.class=<clase>`); no lances `connectedAndroidTest` completo.

## Presupuesto de revisión

- **Niveles de hallazgo:**
    - **Bloqueante:** solo comportamiento incorrecto; escenario de spec sin test; contradicción entre artefactos que haría implementar algo mal; riesgo de pérdida de datos, cierre de la app o seguridad. Detiene el avance.
    - **Corregir:** errores reales sin impacto en el funcionamiento (redacción, formato, evidencias, comentarios). Se corrigen en una sola pasada, sin nueva revisión.
    - **Descartar:** lo que no encaja en los anteriores. No se hace y no va a `docs/deuda-tecnica.md`, salvo que tenga un valor claro.
- **Máximo dos rondas por fase.** La segunda verifica solo los bloqueantes de la primera, recibe el texto de las observaciones anteriores y no admite hallazgos nuevos, salvo pérdida de datos, cierre de la app o seguridad.
- **Rigor proporcional.**
    - Vía rápida (ver su sección): sin `android-architect`; como mucho una ronda de `code-reviewer`.
    - Cambio ligero (configuración o infraestructura, sin lógica de dominio, diff pequeño): una ronda de `android-architect` y una de `code-reviewer`.
    - Cambio normal: flujo completo con el límite de dos rondas.
- **Evidencias en `tasks.md`** solo cuando son la única prueba de un escenario (por ejemplo, una mutación). En lo demás, el commit y la build en verde son la evidencia.
- **Subagentes:** cada subagente de revisión recibe este presupuesto en su prompt.

## No hacer

- No hagas commit, push ni merge en `develop` ni en `master`.
- No modifiques el JDK del daemon (ADR-0011); un permiso del proyecto bloquea editar `gradle/gradle-daemon-jvm.properties`.
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
    - **Fase:** `vía rápida` | `explore` | `propose` | `architect` | `apply grupo N` | `review` | `verify` | `archive` | `pr`.
    - **Veredicto android-architect:** `pendiente` | `APROBADO` + fecha.
- Cuando un cambio llega a **Fase: `pr`** con la PR ya abierta por el hook y nada pendiente salvo que
  el usuario mergee, `handoff.md` se resetea a una plantilla mínima (cambio y rama: ninguno activo, con
  una referencia de una línea al último cambio cerrado; fase: ninguna; veredicto: pendiente; siguiente
  paso: el usuario decide el próximo cambio; decisiones pendientes: ninguna). No se acumula el histórico
  de cambios cerrados: ya lo cuentan `git log`, las PRs y `openspec/changes/archive/`.
- Cada parada termina con una línea de estado de contexto:
    - ✅ Seguro para `/clear`: todo commiteado, `tasks.md` y `handoff.md` al día, nada solo en la conversación.
    - ⚠️ Mejor `/compact`: trabajo a medias o decisión todavía no escrita.
    - ⛔ No limpies: cambios sin commitear o comando en curso.
- Regla práctica: `/clear` tras el commit de un grupo; `/compact` a mitad de grupo.

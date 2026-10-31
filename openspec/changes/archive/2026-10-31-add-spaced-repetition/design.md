# Design

## Context

El proyecto no tiene todavía código de dominio ni especificaciones en `openspec/specs/`. Los módulos
`:core:domain` (Kotlin puro, sin Android) y `:core:testing` existen como esqueleto. Las reglas vienen
de ADR-0012 y se traducen a los requisitos de `specs/spaced-repetition/spec.md`. Ver proposal.md - Why
para la motivación.

## Goals / Non-Goals

**Goals:**
- Modelar la frase en la tanda, nivel, racha y certificación como tipos inmutables con invariantes explícitas (nivel 0–5, racha ≥ 0).
- Implementar las transiciones de racha, la resolución al cerrar la tanda y el cálculo de repasos como funciones puras.
- Mantener el cálculo de fechas independiente del reloj del sistema, recibiendo la fecha como dato.

**Non-Goals:**
- Composición de la tanda nueva, límite N y orden de la lista circular (especificación `practice-batch`, cambio posterior).
- Persistencia (Room / DataStore) y puertos `XxxRepository`: no hay datos que guardar todavía.
- Implementación de `Clock` del sistema en `:core:platform` y su cableado en `:app`: se añadirá en un cambio posterior.
- Casos de uso para predicados: `isPendingOn` es una función de la entidad, no un caso de uso.

## Decisions

**Módulos y paquetes.** Todo el cambio vive en `:core:domain`. `:core:testing` aporta `FixedClock`, y `:core:domain`
ya tiene `testImplementation(projects.core.testing)`.

| Clase / función | Módulo | Paquete |
|---|---|---|
| `Clock` (puerto) | `:core:domain` | `com.dalmuina.fraseafrase.core.domain.time` |
| `PhraseId`, `Level`, `Streak`, `PhraseProgress`, `SelfAssessment`, `ReviewSchedule`, `BatchClosureOutcome` | `:core:domain` | `com.dalmuina.fraseafrase.core.domain.spacedrepetition` |
| `RecordSelfAssessment`, `ResolveBatchClosure` | `:core:domain` | `com.dalmuina.fraseafrase.core.domain.spacedrepetition` |
| `FixedClock` (fake) | `:core:testing` | `com.dalmuina.fraseafrase.core.testing` |

No hay tareas en `:core:data`, `:core:platform`, `:core:designsystem`, `:feature:*` ni `:app`: no aplican a este cambio.

**Puerto `Clock`.** Interfaz `Clock` con `fun today(): LocalDate`, que devuelve la fecha local del dispositivo.
Se usa `java.time.LocalDate` (JDK, sin librería nueva, disponible en minSdk 26 sin desugaring). Alternativa
descartada: `kotlinx-datetime`, que añadiría una dependencia sin necesidad en esta fase. Como el nombre
colisiona con `java.time.Clock`, el código del dominio no importa esa clase.

**Fake de reloj.** `class FixedClock(private val date: LocalDate) : Clock`, inmutable. Un test que necesite otra
fecha crea otra instancia.

**Entidades inmutables.**
- `PhraseId` como `value class` sobre `String`.
- `Level` como `value class` con un `Int` validado en 0..`MAX_LEVEL`, donde `MAX_LEVEL = 5` es una constante con
  nombre en `companion object`. Fuera de rango lanza `IllegalArgumentException` porque es un error de programación,
  no un fallo esperable.
- `Streak` como `value class` con `Int` ≥ 0 y operaciones `afterHit()` y `afterMiss()`. Alternativa descartada:
  un `Int` suelto, que dispersaría la regla de racha por los casos de uso.
- `PhraseProgress(id: PhraseId, level: Level, streak: Streak, answeredInBatch: Boolean, nextReview: LocalDate?)`:
  representa una frase dentro de la tanda activa. Expone `isCertified` (racha ≥ 3, constante
  `CERTIFICATION_STREAK = 3`) e `isPendingOn(today: LocalDate): Boolean` (nivel ≥ 1 y `nextReview` ≤ `today`).
  Son propiedades y funciones puras de la entidad.
  **Invariantes** (requisito "Invariantes de PhraseProgress" de la spec, se validan en el constructor con `require`):
  nivel 0 ⇔ `nextReview == null`; nivel ≥ 1 ⇒ `nextReview != null`; racha > 0 ⇒ `answeredInBatch == true`.
  Con estas invariantes, `isPendingOn` nunca queda indefinido.

**Autoevaluación.** `enum class SelfAssessment { HIT, MISS }`.

**Función pura de repaso.** `ReviewSchedule` es un `object` con
`fun nextReviewAfter(level: Level, from: LocalDate): LocalDate?`, que devuelve `null` para nivel 0 y
`from.plusDays(n)` con `n` leído de `INTERVALS_DAYS`. `INTERVALS_DAYS` es una propiedad `private val` declarada en el cuerpo del `object`, con
los valores 1, 3, 7, 14 y 30 para los niveles 1–5. Alternativa descartada: un `when` repetido en cada uso, que
duplicaría el mapeo. Los literales van en declaraciones de propiedad porque `config/detekt/detekt.yml` solo
ignora `MagicNumber` en ese caso (`ignorePropertyDeclaration: true`); `min(nivel + 1, 5)` usa `MAX_LEVEL`, no el literal.

**Casos de uso** con `operator fun invoke` y `@Inject constructor`:
- `RecordSelfAssessment.invoke(progress: PhraseProgress, assessment: SelfAssessment): PhraseProgress`:
  aplica la autoevaluación, actualiza la racha y marca `answeredInBatch = true`. No depende del reloj.
- `ResolveBatchClosure.invoke(batch: List<PhraseProgress>): List<BatchClosureOutcome>`: recibe `Clock` por
  constructor (`@Inject constructor(private val clock: Clock)`), obtiene la fecha de cierre con `clock.today()` y
  aplica las reglas a cada frase. Preserva el orden de entrada, que `practice-batch` necesita para arrastrar las
  frases "en el orden que tenían" (ADR-0012). Con lista vacía devuelve lista vacía.

**Resultado de cierre sellado.** `BatchClosureOutcome` tiene dos variantes:
- `Certified(progress: PhraseProgress)`: nivel `min(nivel + 1, MAX_LEVEL)`, con `MAX_LEVEL = 5`, y próximo repaso desde la fecha de cierre
  con `ReviewSchedule`; sale de la tanda.
- `Carried(progress: PhraseProgress)`: arrastrada a la siguiente tanda. Nivel 0 y sin repaso si fue respondida y no
  certificada; nivel y repaso sin cambios si nunca fue respondida.

**Regla general de cierre.** Todo `BatchClosureOutcome` (`Certified` y `Carried`) deja `streak = 0` y
`answeredInBatch = false` en su `progress`. Así una frase certificada que vuelve en una tanda posterior empieza con
racha 0, sin necesidad de reglas adicionales aquí. Las tres situaciones de ADR-0012 se reducen a dos variantes
porque "respondida no certificada" y "nunca respondida" se arrastran igual; solo cambia el nivel, y eso se expresa
en `Carried`.

**Restricción verificada por búsqueda.** El código de `:core:domain` no debe leer el reloj del sistema
(`System.`, `.now(`, `Instant.`, `Clock.system`, `systemUTC`, `systemDefaultZone`, `java.time.Clock`). No se añade ninguna regla de detekt ni test de escaneo: la comprobación es el
comando `rg` del paso 5.4 y la revisión de `code-reviewer`. Las reglas automáticas quedan reservadas al cambio de
Konsist.

**Sin dependencias nuevas.** Solo la biblioteca estándar del JDK y los frameworks de test ya presentes en el version catalog.

## Términos ADR → código

| Término (ADR-0012) | Tipo o función |
|---|---|
| Tanda | `List<PhraseProgress>` de la tanda activa |
| Nivel | `Level` |
| Racha | `Streak` dentro de `PhraseProgress` |
| Certificada | `PhraseProgress.isCertified` / `BatchClosureOutcome.Certified` |
| Respondida | `PhraseProgress.answeredInBatch` |
| Pendiente | `PhraseProgress.isPendingOn(today)` |
| Arrastrada | `BatchClosureOutcome.Carried` |

## Estrategia de tests

- Tests unitarios JVM en `:core:domain` con JUnit 6 (Jupiter) y aserciones de Kotest. No hay Turbine ni
  corrutinas, y no se usa MockK porque no hay fronteras que simular.
- `FixedClock` en `:core:testing` es la única fuente de la fecha de hoy en los tests; las fechas de los escenarios
  se escriben como `LocalDate.of(...)` porque son datos deterministas.
- Sin tests de integración ni de UI: este cambio no tiene datos ni presentación.
- Cada test lleva el nombre del escenario en inglés con comillas invertidas y un comentario con el escenario de la spec (ADR-0007).
- `PhraseId` no tiene comportamiento propio; se ejercita a través de los tests de `PhraseProgress`.

## Risks / Trade-offs

- **[Ambigüedad de "respondida"]** → Se define como "al menos una autoevaluación en la tanda" en la especificación.
- **[Restricción de reloj no automatizada]** → Comprobación por `rg` y revisión manual por `code-reviewer`. Los tests
  fijan la fecha con `FixedClock`, así que una lectura directa del reloj del sistema no rompería los tests de forma explícita.
- **[Límites de nivel]** → Subir de 5 se clampa con `min`, no lanza error; así el caso de nivel 5 no es excepcional.

## Migration Plan

No aplica: no hay datos persistidos ni cambios de esquema. Rollback = revertir el commit.

## Open Questions

Ninguna que afecte a especificaciones, enfoque o tareas.

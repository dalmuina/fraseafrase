# Design

## Context

Todo vive en `:core:domain`, paquete `com.dalmuina.fraseafrase.core.domain.spacedrepetition` y
`...practice`. Hoy:

- `PhraseProgress.record(assessment: SelfAssessment)` es una función pura sin fecha: en un fallo solo
  reinicia la racha (`streak.afterMiss()`) y marca `answeredInBatch = true`. El nivel y el próximo repaso
  no se tocan aquí.
- `PracticeBatch.answer(id, assessment)` llama a `entries.first { it.id == id }.record(assessment)` y
  mueve la frase al final de la lista. También es pura, sin fecha.
- `RecordSelfAssessment` (caso de uso `@Inject constructor()`, sin `Clock`) envuelve
  `progress.record(assessment)`. No se usa desde ningún caso de uso real: solo tiene tests propios
  (`RecordSelfAssessmentTest`) y lo usa `ResolveBatchClosureTest` como atajo para preparar un `PhraseProgress`
  ya fallado. Mantenerlo compilando, consistente con el resto del dominio, está dentro de este cambio
  porque su firma depende directamente de `PhraseProgress.record()`; no se amplía su uso real.
- `AnswerPhrase` (caso de uso real, `:core:domain/practice`) es el único llamador de producción de
  `PracticeBatch.answer()`. Hoy no inyecta `Clock`.
- `ResolveBatchClosure` (inyecta `Clock`, llama a `clock.today()` una vez) resuelve el cierre de la tanda.
  Su `carried()` fuerza nivel 0 y `nextReview = null` cuando `answeredInBatch` es verdadero - la regla que
  ADR-0015 sustituye.
- Patrón ya establecido en el dominio (`ResolveBatchClosure`, `ReviewSchedule.nextReviewAfter`,
  `PhraseProgress.isPendingOn`): las funciones puras de entidad reciben la fecha como parámetro; solo la
  capa de casos de uso inyecta `Clock` y la obtiene con `clock.today()`.

Ver proposal.md - Why, y ADR-0015 (`docs/adr/0015-bajada-instantanea-de-nivel-ante-un-fallo.md`), que
sustituye a ADR-0012 en esta regla.

## Goals / Non-Goals

**Goals:**
- Que un fallo en una frase de nivel ≥ 1 baje su nivel y recalcule su próximo repaso en el momento de la
  autoevaluación, siguiendo el patrón ya establecido (fecha como parámetro en las funciones puras,
  `Clock` solo en los casos de uso).
- Que el cierre de la tanda deje de forzar nivel 0 en las frases respondidas y no certificadas, porque ya
  quedó fijado durante la práctica.

**Non-Goals:**
- No cambia la UI de `:feature:session` ni `:feature:settings`: ya muestran nivel y racha leyendo
  `PhraseProgress`, que no cambia de forma (mismos campos).
- No cambia la persistencia (`:core:data`, Room): el esquema de `PhraseProgress` no cambia.
- No introduce las insignias de progreso (P4): este cambio solo deja el nivel correcto para que P4 las
  derive de él.
- No limpia `RecordSelfAssessment` como caso de uso huérfano (no es parte de ADR-0015; si se quiere
  eliminar o usar de verdad, es un cambio aparte).

## Decisions

### D1: La fecha de la autoevaluación llega como parámetro a las funciones puras

`PhraseProgress.record(assessment, today: LocalDate)`, `PracticeBatch.answer(id, assessment,
today: LocalDate)` y `RecordSelfAssessment` reciben `today` en vez de leer el reloj ellos mismos.
Alternativa descartada: que `PhraseProgress`/`PracticeBatch` dependan de `Clock` directamente - rompería
que son entidades sin estado de infraestructura y es inconsistente con `isPendingOn(today)` y
`nextReviewAfter(level, from)`, que ya reciben la fecha como dato.

### D2: Solo `AnswerPhrase` inyecta `Clock` en esta ruta

`AnswerPhrase` (único llamador de producción) pasa a inyectar `Clock` y llama a
`batch.answer(id, assessment, clock.today())` dentro de la transacción. `RecordSelfAssessment` también
inyecta `Clock` (para seguir compilando con la nueva firma de `record()`), igual que hace
`ResolveBatchClosure` hoy.

### D3: `PhraseProgress.record()` aplica la bajada solo en `MISS` y solo si `level.value >= 1`

```
MISS con level >= 1 -> newLevel = Level(level.value - 1)
                        nextReview = ReviewSchedule.nextReviewAfter(newLevel, today)
MISS con level == 0 -> sin cambios de nivel ni de repaso (ya es 0 / null)
HIT                 -> sin cambios (streak.afterHit(), answeredInBatch = true)
```

Reutiliza `ReviewSchedule.nextReviewAfter`, que ya devuelve `null` para nivel 0: no hace falta una rama
aparte para "nivel 1 baja a nivel 0". Varios fallos en la misma tanda se acumulan solos, porque cada
llamada parte del `level` ya actualizado por la anterior (sin estado adicional que mantener).

### D4: `ResolveBatchClosure.carried()` deja de forzar nivel 0

Pasa a ser solo `progress.copy(streak = Streak(0), answeredInBatch = false)`: el nivel y el próximo
repaso de una frase respondida y no certificada ya están fijados por D3 durante la práctica; una frase
nunca respondida ya los conservaba sin cambios. La rama `if (progress.answeredInBatch) ... else ...` que
distinguía ambos casos deja de ser necesaria.

## Risks / Trade-offs

- [Cambiar la firma de `PhraseProgress.record()`, `PracticeBatch.answer()` y `RecordSelfAssessment` rompe
  la compilación de sus tests actuales] → Es el coste esperado de un cambio de dominio con TDD: cada test
  roto se corrige para pasar la fecha, y los tests nuevos de D3/D4 se añaden en rojo antes de implementar.
- [Una frase puede subir y bajar de nivel varias veces en la misma tanda, lo que ya advierte ADR-0015
  como consecuencia negativa] → Sin mitigación en este cambio: es el comportamiento decidido, no un bug.
- [`RecordSelfAssessment` sigue sin usarse en producción tras este cambio, ahora con `Clock` inyectado sin
  necesidad real] → Aceptado como Non-Goal; no se amplía su alcance para "arreglarlo" en este cambio.

## Estrategia de test

- `PhraseProgressTest`: nuevos casos para D3 (fallo en nivel ≥ 1 baja nivel y recalcula repaso; fallo en
  nivel 1 baja a nivel 0 sin repaso; fallo en nivel 0 no cambia nada; acierto no cambia nivel ni repaso;
  dos fallos encadenados el mismo día bajan el nivel dos veces, acumulando sobre el nivel ya reducido por
  la llamada anterior). Unitario puro, sin fakes: todo recibe `today` como `LocalDate` literal.
- `PracticeBatchTest`: añade `today` a las llamadas a `answer()` existentes y un caso que compruebe que la
  bajada de nivel se propaga desde `PhraseProgress.record()`.
- `RecordSelfAssessmentTest`: añade `today` a las llamadas; reutiliza `FixedClock` de `:core:testing` donde
  haga falta, igual que `ResolveBatchClosureTest`.
- `ResolveBatchClosureTest`: actualiza los tres escenarios de "respondida no certificada" para partir de
  un `PhraseProgress` ya bajado de nivel (resultado de un `record()` previo) y comprobar que el cierre no
  lo vuelve a tocar.
- `AnswerPhraseTest`: usa `FixedClock` de `:core:testing` al construir `AnswerPhrase`, y un
  nuevo test que compruebe que un fallo en una frase de nivel ≥ 1 baja su nivel a través del caso de uso
  completo (repositorio fake incluido).

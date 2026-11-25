# Tasks

## 1. Estado vacío sin repasos futuros en los tests (`:core:domain`, `:feature:session`)

> Nota: grupo solo de tests y neutro (design D4): los datos nuevos pasan con el código actual y con el de
> ADR-0026. Toca dos módulos para que el hook de commit siga en verde en los grupos siguientes. Mecánico.

- [x] 1.1 `ObtainPracticeSessionTest` (`:core:domain`), escenario "Catálogo agotado tras cerrar" de
  `specs/practice-session/spec.md`: construir el catálogo sin frases del nivel CEFR elegido en vez de usar
  repasos futuros. Verificación: el test pasa sin cambios de producción.
- [x] 1.2 `CloseThenObtainTest` (`:core:domain`), escenario "Cierre sin composición posible deja la tanda sin
  activar" (spec `practice-persistence`, según el comentario del test): mismo ajuste (catálogo sin frases del
  nivel elegido). Verificación: pasa sin cambios de producción.
- [x] 1.3 `PracticeSessionCloseTest` (`:feature:session`), escenario "Sin frases disponibles al pedir el cierre":
  mismo ajuste.
- [x] 1.4 `PracticeSessionCloseTest` (`:feature:session`), escenario "Generar sesión muestra la tanda siguiente con
  sus arrastradas primero" de `specs/practice-session/spec.md`: catálogo con 4 frases nuevas y sin otra frase con
  progreso del nivel elegido; el orden esperado sigue siendo A, N1–N4, C. Verificación:
  `./gradlew :core:domain:check :feature:session:check` en verde sin cambios de producción.

## 2. Acierto sobre una frase aún no pendiente (`:core:domain`)

> Nota: 2.4 puede tocar `:feature:session` solo en datos de test, para que el hook de `check` global siga en verde.

Escenarios: `specs/spaced-repetition/spec.md` → requisito "Acierto sobre una frase aún no pendiente" y "Acierto
suma uno a la racha".
Ficheros: `spacedrepetition/PhraseProgress.kt` / `PhraseProgressTest.kt`, `PracticeBatchTest.kt`. Design D2.

- [x] 2.1 Test rojo en `PhraseProgressTest`: "Acierto en repaso adelantado no suma", "Acierto en repaso
  adelantado con racha 2 no certifica" y "Un segundo acierto en la misma sesión no suma". Verificación: fallan
  porque hoy la racha sube.
- [x] 2.2 Implementar en `PhraseProgress.recordHit`: con `dueAtSession > currentSession`, solo
  `answeredInBatch = true`; actualizar el KDoc de `record` (ADR-0026). Verificación: los tests de 2.1 pasan.
- [x] 2.3 Tests de caracterización "Fallo en repaso adelantado cuenta" (`PhraseProgressTest`) y "Revertir un
  acierto en repaso adelantado deja el progreso igual" (`PracticeBatchTest`). Verificación: pasan sin más cambios.
- [x] 2.4 Si algún test existente de `:core:domain` o `:feature:session` acierta una frase con `dueAtSession`
  futuro sin querer probar el repaso adelantado, ajustar solo sus datos. Verificación:
  `./gradlew :core:domain:check :feature:session:check` en verde.

## 3. Tanda llena hasta N con repaso adelantado (`:core:domain`)

> Nota: 3.5 toca `:feature:session` (y, si fallan, `:core:data` o `:app`) solo en datos de test: el cambio de
> composición los rompe en el mismo commit y el hook exige `check` global en verde.

Escenarios: `specs/practice-batch/spec.md` → "Orden del repaso adelantado", "Composición de la tanda o ausencia
de tanda", "Candidatas a la tanda nueva", "Número de frases nuevas según el aprendizaje", "Reparto de las nuevas
en la tanda"; `specs/practice-persistence/spec.md` → "Primer uso crea la primera tanda";
`specs/practice-session/spec.md` → "Frases con repaso futuro sin tanda".
Ficheros: `spacedrepetition/ComposeNextBatch.kt` / `ComposeNextBatchTest.kt`, `practice/ObtainActiveBatchTest.kt`,
`practice/ObtainPracticeSessionTest.kt`. Design D1.

- [ ] 3.1 Tests rojos en `ComposeNextBatchTest` para "Composición de la tanda o ausencia de tanda" y "Candidatas
  a la tanda nueva": reescribir los que contradicen ADR-0026 (primera tanda, sesión futura, nivel alto sin
  vencer, catálogo agotado) y añadir los nuevos. Verificación: fallan por tamaño o contenido de la tanda.
- [ ] 3.2 Tests rojos en `ComposeNextBatchTest` para "Número de frases nuevas según el aprendizaje", "Orden del
  repaso adelantado" y "Las nuevas se reparten también entre el repaso adelantado". Verificación: fallan por
  falta de repaso adelantado o de nuevas de relleno.
- [ ] 3.3 Tests rojos de casos de uso: "Primer uso crea la primera tanda" (`ObtainActiveBatchTest`, 15 frases) y
  "Frases con repaso futuro sin tanda" (`ObtainPracticeSessionTest`, muestra P). Verificación: fallan con el
  compositor actual.
- [ ] 3.4 Implementar los cuatro tramos de design D1 en `ComposeNextBatch`, con una sola función de prioridad
  para pendientes y repaso adelantado, y KDoc con ADR-0026. Verificación: 3.1–3.3 pasan y
  `./gradlew :core:domain:check` en verde.
- [ ] 3.5 `:feature:session`, escenarios de `specs/practice-session/spec.md`: "Frases con repaso futuro sin tanda"
  (`PracticeSessionViewModelTest`: esperar la tanda con P en vez de `NothingToPractice`) y "Generar sesión con
  todas certificadas deja la tanda siguiente sin arrastradas" (`PracticeSessionCloseTest`, `given all phrases
  cooling down…`: catálogo A, B, N1, N2 y tanda esperada A, N1, N2, B con su progreso intacto); ajustar solo datos de otros tests que fijen el tamaño de
  la primera tanda. Verificación: `./gradlew :feature:session:check :core:data:check` en verde.

## 4. Cierre y verificación (sesión principal)

- [ ] 4.1 Líneas de estado (ADR-0001): ADR-0026 → `Aceptado`; ADR-0025 → `Sustituido por ADR-0026`; en
  `docs/adr/README.md`, fila de 0026 → `Aceptado` y fila de 0025 → `Sustituido por 0026`.
  Verificación: el README y las cabeceras coinciden.
- [ ] 4.2 `./gradlew check` y `./gradlew koverVerify` en verde (dominio 90 %, presentación 80 %, global 70 %).
- [ ] 4.3 Comprobación manual con `assembleDebug`, N = 10 y 3 o 4 sesiones: la primera tanda tiene 10 frases y
  las siguientes también mientras el nivel tenga 10 o más frases. Si no hay dispositivo, indicarlo en la tarea.

## Workflow follow-up

- `code-reviewer` (hasta dos rondas), `/opsx:verify`, `/opsx:archive` y commit de archive (abre la PR por hook).

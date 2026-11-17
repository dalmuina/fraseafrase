# Tasks

## 1. Dominio: filtro CEFR exacto en ComposeNextBatch

- [x] 1.1 En `ComposeNextBatchTest.kt`, cambiar el test
      `given a reinforce and mastered phrase of a higher level when composing then both enter` a
      `given a reinforce and mastered phrase of a higher level when composing then neither enters`
      (mismo escenario del spec delta "El filtro CEFR admite niveles iguales o superiores al
      elegido en reforzar y dominadas", ahora con el resultado corregido): mismos datos de entrada
      (reforzar B1, dominada C1, nivel elegido A1), pero `result.composed().batch.entries` vacío de
      "R" y "M". Ejecutar `./gradlew :core:domain:test --tests "*ComposeNextBatchTest*" -q
      --console=plain` y comprobar que falla por aserción (las frases siguen entrando), no por
      error de compilación.
- [x] 1.2 Añadir el test `given a reinforce and mastered phrase of the exact chosen level when
      composing then both enter` (escenario "El filtro CEFR admite solo el nivel elegido en
      reforzar y dominadas"): reforzar y dominada ambas A1, nivel elegido A1, ambas entran.
      Ejecutar el mismo filtro de test y comprobar que also falla antes de implementar (si ya
      pasara, documentarlo y pasar a la siguiente tarea sin marcarla como roja).
- [x] 1.3 En `ComposeNextBatch.kt`, cambiar `catalogById.getValue(it.id).level >= cefrLevel` por
      `catalogById.getValue(it.id).level == cefrLevel` en los filtros de `reinforceCandidates` y
      `masteredCandidates`. Ejecutar `./gradlew :core:domain:test --tests "*ComposeNextBatchTest*"
      -q --console=plain` y comprobar que 1.1 y 1.2 pasan en verde.
- [x] 1.4 Revisar el comentario del test `given only below level material when composing then
      there is no batch` (escenario "Sin material del nivel elegido o superior la tanda sale más
      pequeña") y, si hace falta, aclarar que "nivel elegido o superior" ahora significa
      simplemente "nivel elegido" (el filtro ya no admite superiores). Verificar con
      `./gradlew :core:domain:test --tests "*ComposeNextBatchTest*" -q --console=plain` que el
      conjunto de tests del fichero sigue en verde.

## 2. Verificación del módulo

- [x] 2.1 Ejecutar `./gradlew :core:domain:check -q --console=plain` y comprobar que termina en
      verde (tests, detekt y Kover del módulo), confirmando que no queda ningún otro test que
      dependiera del comportamiento "igual o superior" fuera de `ComposeNextBatchTest.kt`
      (`./gradlew :core:domain:test -q --console=plain` ya ejercita todo el módulo).

## Workflow follow-up

- Revisar la propuesta y el ADR-0021 con `android-architect` antes de `/opsx:apply`, porque el
  cambio contradice la regla de ADR-0017 sobre el alcance del filtro CEFR.
- Tras la aprobación, aceptar el ADR-0021 (cambiar su estado a `Aceptado` y actualizar la línea de
  ADR-0017 en `docs/adr/README.md` a `Sustituido por 0021` si corresponde según el proceso del
  proyecto) como parte del commit de artefactos de OpenSpec previo a `/opsx:apply`.
- Ejecutar `/opsx:verify` y `/opsx:archive` en la rama del cambio antes de abrir la pull request.

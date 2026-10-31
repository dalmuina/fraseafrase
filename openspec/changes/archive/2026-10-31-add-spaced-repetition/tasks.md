# Tasks

Cada tarea de implementación va precedida de su test, que debe fallar por el motivo esperado antes de escribir código de producción (TDD). Cada test lleva el escenario de la especificación que cubre. Los tests van en `:core:domain` y el fake del reloj en `:core:testing`.

## 1. Puerto Clock, niveles y repaso

- [x] 1.1 Escribir el test de `FixedClock` en `core/domain/src/test/kotlin/com/dalmuina/fraseafrase/core/domain/time/FixedClockTest.kt`: `FixedClock(10-03-2026).today()` devuelve 10-03-2026. Ejecutarlo y comprobar que falla porque `Clock` y `FixedClock` no existen. (Andamiaje sin escenario de spec; ver design.md, Estrategia de tests)
- [x] 1.2 Implementar el puerto `Clock` (`fun today(): LocalDate`) en `com.dalmuina.fraseafrase.core.domain.time` y `class FixedClock(private val date: LocalDate) : Clock` en `:core:testing`; verificar que el test de 1.1 pasa.
- [x] 1.3 Escribir el test parametrizado de `Level`, escenarios "Nivel fuera de rango se rechaza" (-1 y 6 lanzan `IllegalArgumentException`) y "Niveles límite se aceptan" (0 y 5 se aceptan). Ejecutarlo y comprobar que falla porque `Level` no existe. (Requisito: Invariantes de PhraseProgress)
- [x] 1.4 Implementar `Level` como `value class` validada con `MAX_LEVEL = 5`; verificar que el test de 1.3 pasa.
- [x] 1.5 Escribir el test de `ReviewSchedule.nextReviewAfter`, uno por escenario: "Intervalo del nivel 1", "Intervalo del nivel 2", "Intervalo del nivel 3", "Intervalo del nivel 4", "Intervalo del nivel 5", "Nivel 0 no tiene próximo repaso", "Cambio de año en el intervalo del nivel 1", "Cambio de año en el intervalo del nivel 5" y "Año bisiesto en el intervalo del nivel 1". Ejecutarlo y comprobar que falla porque `ReviewSchedule` no existe. (Requisito: Próximo repaso según el nivel)
- [x] 1.6 Implementar `ReviewSchedule` como `object` con `nextReviewAfter(level, from)` y `private val INTERVALS_DAYS` en el cuerpo del `object`; verificar que el test de 1.5 pasa.

## 2. Racha, certificación e invariantes de la frase

- [x] 2.1 Escribir el test de `Streak`: escenarios "Acierto suma uno a la racha" (racha 1 → 2) y "Fallo reinicia la racha" (racha 2 → 0). Ejecutarlo y comprobar que falla porque `Streak` no existe. (Requisito: Racha de aciertos dentro de la tanda)
- [x] 2.2 Implementar `Streak` con `afterHit()` y `afterMiss()`; verificar que el test de 2.1 pasa.
- [x] 2.3 Escribir el test de `PhraseProgress`, uno por escenario: "Racha de tres certifica la frase al cerrar" (racha 3 → `isCertified`), "Racha de dos no certifica la frase al cerrar" (racha 2 → no), "Nivel 0 con próximo repaso se rechaza", "Nivel 1 sin próximo repaso se rechaza" y "Racha mayor que 0 sin respuesta se rechaza". Ejecutarlo y comprobar que falla porque no existen `PhraseId` ni `PhraseProgress`. (Requisitos: Certificación por racha al cerrar la tanda e Invariantes de PhraseProgress)
- [x] 2.4 Implementar `PhraseId` (`value class`), `PhraseProgress` con las invariantes y `isCertified` (`CERTIFICATION_STREAK = 3`); verificar que el test de 2.3 pasa.
- [x] 2.5 Escribir el test de `RecordSelfAssessment` con `SelfAssessment.HIT` y `MISS`: escenarios "Acierto suma uno a la racha" y "Fallo reinicia la racha" en forma de caso de uso, y "Frase con un fallo cuenta como respondida", solo en su aserción de `answeredInBatch == true` tras un MISS. Ejecutarlo y comprobar que falla porque el caso de uso no existe. (Requisito: Frase respondida en la tanda)
- [x] 2.6 Implementar `RecordSelfAssessment` (`operator fun invoke`, `@Inject constructor`); verificar que el test de 2.5 pasa.
- [x] 2.7 Escribir el test de `Streak`, escenario "Racha negativa se rechaza": `Streak(-1)` lanza `IllegalArgumentException`. Ejecutarlo y comprobar que falla por aserción (no se lanza excepción) con `Streak` sin validación. (Requisito: Invariantes de PhraseProgress)
- [x] 2.8 Implementar `require(value >= 0)` en `Streak`; verificar que el test de 2.7 pasa.

## 3. Resolución de la tanda al cerrarla

- [x] 3.1 Escribir el test de clasificación en `ResolveBatchClosure` (con `Clock` por constructor): escenarios "Racha de tres certifica la frase al cerrar", "Racha de dos no certifica la frase al cerrar", "Un fallo tras tres aciertos impide la certificación" (usando `RecordSelfAssessment` de 2.6), "Tanda vacía devuelve lista vacía", y que el resultado conserva el orden de entrada (decisión de design.md). Ejecutarlo y comprobar que falla porque `ResolveBatchClosure` no existe. (Requisitos: Certificación por racha al cerrar la tanda y Cierre de una tanda sin frases)
- [x] 3.2 Implementar `BatchClosureOutcome` sellado (`Certified` / `Carried`) y `ResolveBatchClosure` con la clasificación y el orden; verificar que el test de 3.1 pasa.
- [x] 3.3 Escribir el test de cierre certificado, uno por escenario: "Certificada de nivel 0 pasa a nivel 1", "Certificada de nivel 1 pasa a nivel 2", "Certificada de nivel 2 pasa a nivel 3", "Certificada de nivel 3 pasa a nivel 4", "Certificada de nivel 4 pasa a nivel 5" y "Certificada de nivel 5 se mantiene en nivel 5". Cada uno aserta además racha 0 y `answeredInBatch` falso ("Frase certificada sale con racha 0 y sin respuestas"). Ejecutarlo y comprobar que falla por el nivel, el repaso o la racha esperados. (Requisitos: Resolución de frase certificada al cerrar la tanda y Estado de la frase tras cerrar la tanda)
- [x] 3.4 Implementar en la rama `Certified` la subida de nivel `min(nivel + 1, MAX_LEVEL)`, el próximo repaso con `ReviewSchedule` y el reinicio de racha y `answeredInBatch`; verificar que el test de 3.3 pasa.
- [x] 3.5 Escribir el test de cierre respondida no certificada, uno por escenario: "Respondida no certificada de nivel 4 vuelve a nivel 0", "Respondida no certificada de nivel 5 vuelve a nivel 0", "Respondida no certificada de nivel 0 permanece en nivel 0" y "Frase con un fallo cuenta como respondida" (la frase sale `Carried` en nivel 0 sin repaso). Ejecutarlo y comprobar que falla. (Requisitos: Resolución de frase respondida no certificada al cerrar la tanda y Frase respondida en la tanda)
- [x] 3.6 Implementar en la rama `Carried` para frases respondidas no certificadas el nivel 0 y el repaso nulo, sin tocar la racha; verificar que el test de 3.5 pasa.
- [x] 3.7 Escribir el test de cierre nunca respondida, uno por escenario: "Frase sin autoevaluaciones cuenta como nunca respondida", "Nunca respondida conserva su nivel y su repaso" y "Nivel nunca baja de 0". Ejecutarlo y comprobar que falla. (Requisitos: Resolución de frase nunca respondida al cerrar la tanda y Niveles de consolidación)
- [x] 3.8 Escribir el test de reinicio de la rama `Carried`: "Frase arrastrada queda con racha 0 y sin respuestas" (racha 2 → 0 y `answeredInBatch` falso). Ejecutarlo y comprobar que falla porque 3.6 no reinicia la racha. (Requisito: Estado de la frase tras cerrar la tanda)
- [x] 3.9 Implementar en la rama `Carried` el reinicio de racha y `answeredInBatch`, y la conservación de nivel y repaso para las nunca respondidas; verificar que los tests de 3.7 y 3.8 pasan.

## 4. Frases pendientes

- [x] 4.1 Escribir el test de `PhraseProgress.isPendingOn`, uno por escenario: "Repaso de hoy está pendiente", "Repaso futuro no está pendiente", "Cambio de día hace pendiente una frase" y "Nivel 0 nunca está pendiente". Ejecutarlo y comprobar que falla porque la función no existe. (Requisito: Frase pendiente de repaso)
- [x] 4.2 Implementar `isPendingOn(today: LocalDate)` en `PhraseProgress`; verificar que el test de 4.1 pasa.

## 5. Verificación de integración

- [x] 5.1 Ejecutar `./gradlew :core:domain:test` y comprobar que todos los tests pasan.
- [x] 5.2 Ejecutar `./gradlew detekt` y comprobar que no hay errores de ktlint ni de detekt.
- [x] 5.3 Ejecutar `./gradlew koverVerify` y comprobar que la cobertura de `:core:domain` es al menos del 90 %.
- [x] 5.4 Buscar en `core/domain/src/main` los patrones `System\.|\.now\(|Instant\.|Clock\.system|systemUTC|systemDefaultZone|java\.time\.Clock` con `rg -n -e` (o la herramienta Grep) y comprobar que no hay coincidencias, según la restricción de design.md. Es un paso de verificación manual, no un test ni una regla de CI.
- [x] 5.5 Revisar con `code-reviewer` el resultado de 5.4 y el resto del cambio.
- [x] 5.6 Ejecutar `./gradlew check` y comprobar que termina en verde.

## Workflow follow-up

- Verificar el cambio con `/opsx:verify` antes de archivar.
- Archivar con `/opsx:archive` cuando la revisión de `code-reviewer` esté completa.
- Próximos cambios (fuera de este): `practice-batch` (composición de la tanda, lista circular, catálogo agotado, primera tanda y racha de frases certificadas al volver en una tanda posterior), persistencia en `:core:data`, el adaptador `SystemClock` en `:core:platform` y las reglas automáticas de arquitectura con Konsist.

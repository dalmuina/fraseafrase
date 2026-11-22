# Proposal

## Why

Al probar la app, la penalización por fallo resulta demasiado severa: hoy un fallo pone la racha a 0 **y**
baja un nivel de consolidación (ADR-0017, regla 3, heredada de ADR-0012/ADR-0015). Una frase en Plata con
racha 2 ("1 para Oro") que se falla una vez acaba en Bronce con racha 0 ("3 para Plata"): pierde de golpe
cinco aciertos de trabajo. El usuario quiere que un fallo cueste **un solo acierto**.

## What Changes

- Un fallo deja de reiniciar la racha y de bajar siempre un nivel. En su lugar resta **un acierto** en una
  escala continua de tres aciertos por nivel:
  - Racha mayor que 0: la racha baja en 1 y el nivel no cambia.
  - Racha 0 y nivel ≥ 1: el nivel baja en 1 y la racha queda en 2 (a un acierto de recuperar el nivel).
  - Racha 0 y nivel 0: sin cambios.
- Ejemplo confirmado por el usuario: Plata con racha 0 ("3 para Oro") → fallo → Bronce con racha 2
  ("1 para Plata").
- Sin cambios: `dueAtSession` tras un fallo sigue siendo la sesión actual + 1; la certificación por racha 3,
  el enfriamiento, la reversión de la última autoevaluación, el desglose de tanda completada y el indicador
  de cambio de insignia (que solo aparece si el nivel cambia).
- Nuevo **ADR-0024** "Un fallo resta un acierto, no un nivel", que sustituye a ADR-0017 solo en la regla de
  fallo (punto 3 de "Resolución al instante" y la frase "un fallo la pone a 0" heredada de ADR-0012). Se crea
  en estado `Propuesto` con esta propuesta y pasa a `Aceptado` en la tarea de cierre.

ADR afectados: ADR-0017 (parcialmente sustituido), ADR-0012/ADR-0015 (ya sustituidos; su regla de fallo
dejaba de estar vigente a través de ADR-0017). Respeta el alcance del MVP (ADR-0012 y ADR-0018): solo cambia
una regla del método, no añade funcionalidad.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `spaced-repetition`: la regla de racha ante un fallo y la resolución del fallo (racha − 1 o bajada de
  nivel con racha 2), y el escenario de reversión de un fallo.
- `practice-session`: los escenarios del contador de aciertos para el próximo nivel, de la insignia y del
  indicador de cambio de insignia que hoy asumen que un fallo siempre baja de nivel.

## Fuera de alcance

- Cambiar el umbral de certificación (3), el enfriamiento por nivel o las cuotas de composición.
- Penalizaciones distintas según el nivel o el número de fallos seguidos.
- Migrar el progreso ya guardado: los valores actuales de nivel y racha siguen siendo válidos.
- Cambios visuales en el contador o en el indicador de insignia.

## Impact

- `:core:domain`: `PhraseProgress.recordMiss` y `Streak.afterMiss`, y sus tests.
- `:feature:session`: tests del ViewModel que fijan el resultado de un fallo (contador e indicador de
  insignia); sin cambios de código de producción previstos.
- `docs/adr/`: ADR-0024 nuevo; línea de estado de ADR-0017 y tabla del README al cerrar.
- Sin cambios en `:core:data` (mismo esquema de Room), sin librerías nuevas.

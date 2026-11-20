# Proposal

## Why

`ObtainActiveBatch` nunca recompone la tanda activa de un idioma destino cuando sus ajustes aplicados
están sin registrar (`appliedSettings == null`): devuelve la tanda existente y no llama a
`recordAppliedSettings`. El propio diseño que introdujo este caso (`remove-settings-save-button`, D2)
asumía que el primer registro llegaría "en una recomposición futura por un cambio de ajustes
posterior a esta actualización" — pero esa recomposición nunca ocurre, porque la propia rama que
debía dar paso a ella es la que siempre devuelve `Existing` sin registrar nada. El resultado,
confirmado en campo (dispositivo físico con una tanda activa de `pt-PT` anterior a esa función, frente
a un emulador con instalación limpia donde sí se registra desde el primer uso): cambiar el nivel CEFR
o el tamaño de tanda (N) deja de disparar nunca una regeneración para ese idioma destino, de forma
permanente. Cambiar de idioma destino sí funciona porque un idioma sin tanda previa siempre pasa por
la composición que registra sus ajustes aplicados.

## What Changes

- Cuando `ObtainActiveBatch` encuentra una tanda activa sin ajustes aplicados registrados
  (`appliedSettings == null`), además de devolver la tanda existente sin recomponerla, registra los
  ajustes guardados vigentes como sus ajustes aplicados, en la misma operación.
- A partir de la siguiente obtención, la comparación ya tiene una base válida: un cambio posterior de
  nivel CEFR o de N vuelve a disparar la recomposición con normalidad.
- No se introduce ninguna recomposición en el primer encuentro con `appliedSettings == null`: solo se
  registra. El comportamiento de "no perder progreso ni recomponer de golpe en una instalación
  existente" (D2/Risks de `remove-settings-save-button`) se mantiene igual.

## Capabilities

### Modified Capabilities

- `practice-persistence`: el requisito "Sin ajustes aplicados registrados todavía no se recompone"
  pasa a registrar los ajustes aplicados en esa misma obtención, sin dejar de devolver la tanda
  existente sin recomponer.

## Impact

- `core/domain/src/main/kotlin/.../practice/ObtainActiveBatch.kt`: la rama `applied == null` pasa de
  "no hacer nada" a "registrar y continuar devolviendo la tanda existente".
- Ningún cambio de esquema ni de puertos: `SettingsRepository.recordAppliedSettings` ya existe y se
  reutiliza.
- Afecta solo a instalaciones con una tanda activa de antes de `remove-settings-save-button` (o
  cualquier otro camino futuro que deje `appliedSettings` sin registrar); el comportamiento de una
  instalación nueva no cambia, porque en ella `appliedSettings` ya queda registrado desde el primer
  uso.
- No contradice el ADR-0012 (no cambia el método de tandas/rachas/certificación) ni el ADR-0013
  (persistencia): usa el mismo almacén y las mismas claves ya definidas para "ajustes aplicados". No
  requiere un ADR nuevo.

## Fuera de alcance

- No se migra ni se hace backfill de `appliedSettings` para idiomas destino que no se vuelvan a
  obtener: el registro ocurre de forma perezosa, en la primera obtención de la tanda activa tras este
  cambio, igual que cualquier otra composición.
- No se cambia la decisión de no recomponer en el primer encuentro con `appliedSettings == null`: eso
  sigue siendo intencional (no perder progreso de una instalación existente) y no es parte de este
  bug.

# Proposal

## Why

En el primer arranque, la pantalla de práctica obtiene la tanda activa dos veces a la vez: la carga inicial del
ViewModel y la acción `Resume` que envía la pantalla al mostrarse. Las dos obtenciones ven "sin tanda" antes de que
cualquiera escriba, y las dos componen. Room serializa las dos transacciones, así que la segunda vacía la tanda que
acaba de guardar la primera y recompone con el progreso que esa primera tanda ya creó. Con progreso previo, la
segunda composición trata como pendientes las frases nuevas de la primera y añade nuevas de más: la tanda cambia y
quedan con progreso frases que el usuario nunca vio (por ejemplo, 14 frases con progreso en lugar de 10). Sin
progreso previo, las reglas de ADR-0026 recomponen la misma tanda, pero se escribe dos veces. La misma carrera existe
con dos obtenciones simultáneas tras un cambio de ajustes, un riesgo ya anotado en `remove-settings-save-button`.
El "18 en vez de 10" observado no se reproduce con las reglas actuales en un primer uso sin progreso; este cambio no
pretende explicarlo, solo cerrar la carrera que lo permitía.

Además, al volver a Practicar tras cambiar el idioma destino, la pantalla compara la tanda nueva con la cargada solo
por id y texto de origen. Como los ids de las frases son comunes a todos los catálogos (ADR-0013), una tanda del
idioma nuevo con los mismos ids y el mismo origen se toma por la misma, y la pantalla sigue mostrando el idioma
anterior.

## What Changes

- La recomposición de la tanda activa (primer uso o ajustes aplicados distintos) solo ocurre si, dentro de la
  transacción de escritura, la tanda activa sigue siendo la que se observó al decidir recomponer. Si otra
  obtención ya la cambió, se devuelve la tanda vigente como `Existing`, sin tocar tanda, progreso ni contador.
- Dos obtenciones concurrentes de la tanda activa de un idioma destino producen una única composición: una sola tanda
  y progreso solo para sus frases.
- Al volver a Practicar, la pantalla considera distinta la tanda si cambió el idioma destino, aunque coincidan ids,
  orden y textos de origen.
- No cambia el ViewModel en cuanto a cuántas veces lee la sesión: la carga inicial y `Resume` siguen pudiendo
  coincidir; la corrección está en el dominio.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `practice-persistence`: requisito nuevo de obtención concurrente de la tanda activa (una sola composición).
- `practice-session`: requisito nuevo para que un cambio de idioma destino se refleje al volver a Practicar.

## Impact

- `:core:domain` (`practice`): `ObtainActiveBatch` y `RegenerateActiveBatchForLevel`; la transición compartida de
  `PracticeTransitions.kt` no cambia.
- `:core:data` (solo tests): test de integración con Room real y dos obtenciones concurrentes.
- `:feature:session`: comparación de la tanda cargada en `PracticeSessionViewModel`.
- Sin cambios de esquema de Room, puertos, dependencias ni navegación.
- ADR afectados: ADR-0026 (composición de la tanda, sin cambios), ADR-0023 (ids comunes entre catálogos), ADR-0010
  (módulos), ADR-0006 (ViewModel) y la atomicidad en una única transacción de escritura (decisión de ADR-0013
  mantenida por ADR-0019). Respeta el alcance del método de práctica (ADR-0012 y sus sucesores): no cambia el método,
  solo evita una composición duplicada. No requiere ADR nuevo.

## Fuera de alcance

- Serializar o deduplicar las lecturas del ViewModel (carga inicial y `Resume`): se descarta por redundante con la
  corrección en el dominio.
- Reparar instalaciones que ya tengan progreso de frases nunca vistas por esta carrera: no hay migración.
- Textos de ayuda CEFR y reglas antiguas en `CLAUDE.md` y comentarios (vía rápida posterior).
- Algoritmo anti-bloqueo (ADR futuro).

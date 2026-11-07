# Proposal

## Why

La prueba manual del cambio anterior reveló dos fallos reales: (1) cambiar el nivel CEFR en Ajustes puede
mezclar frases de niveles muy distintos, porque hoy solo se filtra por CEFR el bloque de frases nuevas; (2)
ninguna frase llega nunca a certificarse, porque la racha se reinicia a 0 cada vez que se compone una tanda y
cada tanda solo ofrece cada frase una vez: con eso, racha 3 (el umbral de certificación) es matemáticamente
imposible, así que el nivel de consolidación nunca sube, nunca hay pendientes ni dominadas, y cada sesión
generada acaba siendo casi toda nueva. Además, el sistema de repaso actual mide los intervalos en días
naturales (ADR-0015), lo que no tiene sentido para una app donde el usuario puede generar muchas sesiones en un
mismo día.

## What Changes

- **BREAKING**: la racha de una frase deja de reiniciarse al componer una tanda nueva: persiste de una sesión
  generada a la siguiente, y solo se reinicia a 0 ante un fallo real. Esto permite que una frase acumule 3
  aciertos a lo largo de varias sesiones generadas, no solo dentro de una.
- **BREAKING**: el repaso espaciado deja de medirse en días naturales y pasa a medirse en **sesiones
  generadas**: cada frase guarda la sesión a partir de la cual vuelve a estar disponible (`dueAtSession`), en
  vez de una fecha (`nextReview`). Un contador global de sesiones generadas, persistido, sustituye al reloj como
  base del repaso.
- **BREAKING**: se retira la **cola de arrastradas** (`CarryQueue`) como estructura de datos independiente. Ya
  no hace falta: con la racha persistente y `dueAtSession`, el propio progreso de cada frase basta para decidir
  si debe reaparecer pronto. Se sustituye por un reparto más directo: una frase recién respondida (en racha, o
  recién fallada) queda disponible para la sesión siguiente; una frase que certifica y sube de nivel queda en
  "enfriamiento" un número de sesiones creciente con el nivel alcanzado.
- La composición de la tanda extiende el filtro de nivel CEFR a los tres bloques (nuevas, a reforzar,
  dominadas): ninguno ofrece ya una frase de un nivel CEFR por debajo del elegido. Si falta material del nivel
  elegido, la tanda sale más pequeña en vez de rellenar con niveles inferiores.
- Las cuotas de composición cambian de ~40 % nuevas / ~40 % a reforzar / ~20 % dominadas a **20 % nuevas / 55 %
  a reforzar / 25 % dominadas**, para dar más peso a la repetición que a la novedad.
- **BREAKING**: al no haber ya una "resolución de cierre" distinta de la resolución al instante de cada
  autoevaluación, cambiar el nivel CEFR y cambiar el tamaño de tanda N en Ajustes pasan a comportarse igual:
  ambos recomponen la tanda activa de inmediato sin tocar racha, nivel ni `dueAtSession` de ninguna frase (antes
  solo CEFR hacía esto; N certificaba y resolvía como un cierre).
- `docs/adr/0017-repeticion-por-contador-de-sesiones.md` (nuevo, `Propuesto`), que sustituye en parte a
  ADR-0015 (intervalos en días) y a ADR-0016 (cuotas, alcance del filtro CEFR).

## Fuera de alcance

- No cambia la certificación por racha ≥ 3 ni la bajada instantánea de nivel ante un fallo (ADR-0015): un
  fallo sigue bajando el nivel al instante; lo que cambia es cuándo y cómo vuelve a aparecer la frase después.
- No cambia el modelo de "generar sesión" en un solo paso, ni el mensaje de tanda completada, ni el indicador
  "N aciertos para `<insignia>`" (`replace-batch-closure-with-session-generation`, ya archivado).
- No cambia la pantalla de Progreso ni las insignias de nivel de consolidación en sí.
- No introduce niveles de usuario globales ni cambia el catálogo de frases.
- No migra datos de usuarios reales: el proyecto está en desarrollo pre-1.0.0, sin usuarios en producción, así
  que la migración de Room puede ser destructiva para las tablas afectadas (ver `design.md`).

## ADR

Este cambio sustituye, en parte, a ADR-0015 (reglas de intervalo de repaso en días naturales) y a ADR-0016
(cuotas de composición, alcance del filtro CEFR). Mantiene vigentes de ambos: la certificación por racha ≥ 3,
la bajada instantánea de nivel ante un fallo, el modelo de "generar sesión" en un solo paso y el resto del
modelo de tanda. Se propone `docs/adr/0017-repeticion-por-contador-de-sesiones.md`, estado `Propuesto`, que se
redacta y revisa junto con esta propuesta.

## Capabilities

### New Capabilities

(ninguna)

### Modified Capabilities

- `spaced-repetition`: sustituye los intervalos de repaso en días naturales por intervalos en sesiones
  generadas (`dueAtSession` en vez de `nextReview`); la racha deja de resetearse al componer una tanda y pasa a
  persistir hasta un fallo real.
- `practice-batch`: sustituye las cuotas 40/40/20 por 20/55/25; extiende el filtro de nivel CEFR a los tres
  bloques; sustituye la cola de arrastradas por la selección directa de "a reforzar" sobre el progreso
  persistido (sin estructura de cola independiente).
- `practice-persistence`: retira la persistencia de la cola de arrastradas (`carry_position`) y del cierre con
  expectativa de aviso (`expectedUncertified`/`Stale`/`NoActiveBatch`), que ya no se usan desde
  `replace-batch-closure-with-session-generation` pero seguían especificados; añade la persistencia del
  contador de sesiones generadas.

- `settings-screen`: unifica el efecto de cambiar el nivel CEFR y de cambiar N — ambos recomponen la tanda
  activa de inmediato sin tocar el progreso de ninguna frase, en vez de que solo N "cierre con resolución
  completa". Es una consecuencia directa de que la resolución ya no ocurre al cerrar una tanda, sino al
  instante de cada autoevaluación (`spaced-repetition`).
- `practice-session`: el requisito "Mensaje de tanda completada y generación de sesión" decía que las
  arrastradas aparecen "en su orden de cola"; sin cola de arrastradas (`practice-batch`), el orden real de
  las frases a reforzar es el de catálogo. Se detectó al implementar los grupos de tareas de este cambio:
  sin este ajuste, un escenario ya archivado quedaría describiendo un comportamiento imposible.

## Impact

- `:core:domain`: `PhraseProgress` (sustituye `nextReview: LocalDate?` por `dueAtSession: Int` no nulo; la
  racha deja de resetearse por tanda y de exigir `answeredInBatch`), `ReviewSchedule` (pasa a un enfriamiento
  en sesiones, 1/2/4/8/16 por nivel), `PhraseProgress.record()` (resuelve certificación, bajada de nivel y
  `dueAtSession` al instante, sin esperar a una resolución de cierre aparte). Se retiran `CarryQueue`,
  `ResolveBatchClosure`, `BatchClosureOutcome` y `ForceCloseActiveBatch` (sin reemplazo con el mismo nombre:
  su función pasa a `RegenerateActiveBatchForLevel`, generalizada para cualquier cambio de ajustes).
  `ComposeNextBatch` pasa a tomar `progress`, catálogo, CEFR, N y la sesión actual (sin `CarryQueue`), con
  cuotas 20/55/25 y filtro CEFR en los tres bloques. `GenerateSession` incrementa el contador de sesiones y
  compone; `RegenerateActiveBatchForLevel` solo recompone, sin incrementarlo. `PracticeSnapshot` sustituye
  `carry: CarryQueue` por `sessionCount: Int`.
- `:core:data`: esquema de Room (retira la tabla/columna de la cola de arrastradas; añade la columna del
  contador de sesiones generadas; migración, probablemente destructiva dado el estado pre-1.0.0 del
  proyecto, sin usuarios en producción).
- `:feature:settings`: `SettingsViewModel.save()` deja de distinguir N de CEFR; llama siempre a
  `RegenerateActiveBatchForLevel` (generalizado) cuando algo cambió.
- `openspec/specs/spaced-repetition`, `practice-batch`, `practice-persistence`, `settings-screen` (deltas).
- `docs/adr/0017-repeticion-por-contador-de-sesiones.md` (nuevo).

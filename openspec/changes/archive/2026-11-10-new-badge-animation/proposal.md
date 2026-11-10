# Proposal

## Why

El cambio `add-level-change-animation` (archivado) especificó e implementó el indicador de
cambio de insignia como una capa centrada en pantalla que aparece **sobre la frase siguiente**,
después de que la pantalla ya haya avanzado a ella. Al usarlo, la insignia nunca transiciona
visualmente (solo un halo de 150 ms al 25 % de opacidad, ausente en bajadas de nivel): el
usuario ve "un icono que aparece" sin percibir ninguna animación. Esto no es un fallo de
implementación respecto a esa especificación: es la especificación la que no captura lo que se
necesita. Hace falta que la insignia que ya está junto al nivel CEFR de la frase que se acaba de
responder transicione en su propio sitio, de la insignia anterior a la nueva, y que la pantalla
avance a la frase siguiente solo cuando esa transición termine.

## What Changes

- El indicador de cambio de insignia deja de mostrarse como una capa centrada sobre la frase
  siguiente: la insignia junto al nivel CEFR de la **frase respondida** transiciona visiblemente
  de la insignia anterior a la resultante, en su propia posición.
- La pantalla **BREAKING** (de comportamiento, no de API pública) deja de avanzar a la frase
  siguiente inmediatamente tras autoevaluar cuando hay cambio de nivel: ahora espera a que esa
  transición termine. Sin cambio de nivel, avanza igual que hoy, sin esperar nada.
- `PracticeSessionViewModel` reordena `answer()`/`consumeBadgeChange()` para que el avance
  (`reload()`) lo dispare el consumo del indicador, no la respuesta en sí.
- `BadgeChange` pasa a llevar también el nivel de partida (`fromLevel`), no solo el resultante.
- `DsLevelBadgeChange` (`:core:designsystem`) pasa a recibir `fromLevel`/`toLevel` y renderiza un
  `Crossfade` + "pop" de escala entre ambos, en vez de una insignia estática con un halo apenas
  perceptible; corrige además el disparador de la animación (`LaunchedEffect` mal cacheado, que
  no se repetía en cambios sucesivos de la misma dirección).

## Capabilities

### New Capabilities

(ninguna)

### Modified Capabilities

- `practice-session`: cambia dónde y cuándo se muestra la transición de insignia (en la frase
  respondida, antes de avanzar, no sobre la frase siguiente), y por tanto el requisito "El
  indicador no bloquea la interacción con la frase siguiente" deja de aplicar tal cual (se
  absorbe como escenario del requisito principal).

## Impact

- **`:feature:session`**: `PracticeSessionViewModel.answer()`/`consumeBadgeChange()` (reordena
  cuándo se llama a `reload()`), `PracticeSessionUiState.BadgeChange` (nuevo campo `fromLevel`),
  `PracticeSessionScreen.kt` (el indicador se renderiza dentro de `PhraseCard`, no en un `Box`
  centrado aparte).
- **`:core:designsystem`**: `DsLevelBadgeChange` cambia su firma (`fromLevel`/`toLevel` en vez de
  `level`) y su animación interna (Compose nativo: `Crossfade`, `animateFloatAsState`,
  `graphicsLayer`; sin dependencias nuevas).
- **ADR afectados**: ninguno requiere un ADR nuevo. No se toca ADR-0015 ni ADR-0017: la
  resolución del nivel en el dominio sigue siendo instantánea; solo cambia cuándo y dónde se
  presenta esa transición en la UI. El cambio respeta el alcance del MVP de ADR-0012.

## Fuera de alcance

- Cualquier animación en el cierre de tanda, la generación de sesión o la pantalla de Progreso.
- Haptics o sonido: solo animación visual.
- Cambiar los umbrales de certificación, enfriamiento o bajada de nivel, o la instantaneidad de
  su resolución en el dominio.
- Añadir una librería de animación: la transición se construye con Compose nativo.

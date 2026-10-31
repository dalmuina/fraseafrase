# Proposal

## Why

La persistencia y las reglas de la tanda ya existen (`add-persistence`, `add-practice-batch`, `add-spaced-repetition`), pero la app todavía no muestra nada: `FraseAFraseApp` es un marcador. Sin una pantalla de práctica el usuario no puede hacer la única acción del MVP (practicar la tanda activa, autoevaluarse y cerrarla). Este cambio añade esa pantalla con el flujo mínimo completo: ver la tanda, practicar, responder y cerrar con aviso.

## What Changes

- Pantalla de práctica en `:feature:session` que muestra la tanda activa como lista circular (ADR-0012):
  - Muestra la frase en español (`source`) y, bajo demanda, revela la frase en portugués (`target`).
  - Tras revelar, el usuario se autoevalúa con **acierto** o **fallo**. La respuesta llama a `AnswerPhrase`, que mueve la frase al final de la tanda.
  - Muestra la racha de la frase dentro de la tanda y marca como "sabida" las frases con racha ≥ 3, sin sacarlas de la lista.
- Botón para **cerrar la tanda**. Antes de cerrar muestra un aviso con el número de frases no certificadas que pasarán a la siguiente tanda (valor del aviso de `practice-batch`). El usuario puede confirmar, que llama a `CloseActiveBatch` con ese número como expectativa, o cancelar y seguir practicando.
- Tras cerrar, la pantalla pide la siguiente tanda con `ObtainActiveBatch` y la muestra. Si no hay nada que componer, muestra un estado vacío que lo indica. Si el catálogo no carga, muestra un estado de error.
- Cierres rechazados por `Stale` o `NoActiveBatch` se resuelven recargando la tanda activa; no se muestran como error.
- Integración en `:app`: la pantalla de práctica es el destino inicial de la app y se cablean sus casos de uso con Hilt.

## Capabilities

### New Capabilities
- `practice-session`: pantalla de práctica de la tanda activa: revelar, autoevaluar, aviso y confirmación de cierre, y estados vacío y de error. Es el comportamiento visible que da acceso a las reglas de `practice-batch` y `spaced-repetition`.

### Modified Capabilities
- Ninguna. Las reglas de dominio no cambian; este cambio solo consume los casos de uso existentes.

## Impact

- `:core:domain`: un caso de uso de lectura para la pantalla que devuelve la tanda activa con el texto de cada frase (ver `design.md`). No cambian entidades ni reglas.
- `:core:data`: `AssetsCatalogDataSource` memoiza la carga del catálogo (ADR-0013, "cargado en memoria"). El puerto no cambia.
- `:feature:session`: `PracticeSessionViewModel`, `PracticeSessionUiState`, `PracticeSessionAction`, `PracticeSessionRoute` y `PracticeSessionScreen`, con tests unitarios y Compose UI Tests.
- `:core:designsystem`: sin cambios. Los componentes son privados de la feature (ver `design.md`, D6).
- `:app`: destino inicial de navegación y verificación del cableado de Hilt.
- Sin dependencias nuevas en `gradle/libs.versions.toml`. Los UI tests usan `ui-test-junit4`, que ya está en el catálogo.
- ADR: ninguno nuevo. Respeta ADR-0012 (alcance y método), ADR-0010 (dependencias hacia dentro: la feature solo depende de `:core:domain` y `:core:designsystem`) y ADR-0006 (UiState, Action y Route/Screen).

## Fuera de alcance

Fuera de este cambio, pero dentro del MVP de ADR-0012: cada uno es un cambio propio de OpenSpec posterior.

- Audio TTS de la frase (ADR-0012, punto 4): necesita `:core:platform` y el tratamiento de errores de voz, que no existen todavía.
- Pantalla de ajustes con nivel CEFR y tamaño de tanda N (ADR-0012, punto 5), y su navegación. Este cambio usa los ajustes guardados sin editarlos.
- Progreso básico: certificadas en la tanda activa y total por nivel (ADR-0012, punto 6). La pantalla solo muestra la racha de la tanda activa.

Fuera del MVP, según ADR-0012:

- Corrección automática por voz (es el primer cambio tras el MVP).
- Dirección inversa (portugués → español) y otros modos de práctica.
- Variante `pt-BR` u otros idiomas.

Otras exclusiones:

- Cambios en las reglas de composición, cierre o intervalos.
- Disparador de la primera tanda en el arranque: la pantalla la pide con `ObtainActiveBatch` como cualquier otra visita.

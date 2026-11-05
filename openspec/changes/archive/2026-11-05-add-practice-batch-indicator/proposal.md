# Proposal

## Why

En la pantalla de práctica el usuario no sabe cuántas frases tiene la tanda activa ni en qué
posición está, ni qué nivel CEFR es la frase que está viendo; y el control de escuchar es un
botón de solo texto que no se lee como una acción de audio. Son tres carencias de orientación y
afordancia visual detectadas al revisar el MVP actual, de baja complejidad y sin impacto en el
dominio, por lo que se abordan primero dentro de la hoja de ruta de mejoras acordada.

## What Changes

- La pantalla de práctica muestra un indicador de posición "X de Y" (frase actual / tamaño de la
  tanda activa).
- La pantalla de práctica muestra el nivel CEFR (A1, A2, B1, B2 o C1) de la frase actual.
- El control de escuchar añade un icono (altavoz/boca hablando) junto a su texto, manteniendo su
  comportamiento actual (visible solo al revelar, deshabilitado si `busy` o con el aviso de cierre
  abierto, sin manejo de error de TTS visible).

Fuera de alcance de este cambio (quedan en la hoja de ruta para cambios posteriores, ver
`design.md`):
- Feedback visual al guardar ajustes y navegación de Ajustes a Practicar al cambiar el nivel.
- Forzar una tanda nueva al cambiar el nivel CEFR.
- Sustituir la racha visible por una insignia de categoría (bronce/plata/oro/platino/diamante) y
  reagrupar la pantalla de progreso por esa categoría — depende de una decisión de ADR aparte
  sobre cómo cambia el nivel de consolidación con un fallo.
- Cualquier rediseño visual de iconos o badges a medida.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `practice-session`: la pantalla de práctica, además de lo que ya expone, MUST mostrar la
  posición de la frase actual dentro de la tanda ("X de Y") y el nivel CEFR de esa frase.
- `phrase-audio`: el control de escuchar, además de su comportamiento actual, MUST mostrar un
  icono de audio junto a su texto en todos los estados en los que ya es visible.

## Impact

- `:feature:session`: `PracticeSessionUiState.Practicing` gana `index: Int`, `total: Int` y
  `cefrLevel: CefrLevel`; `PracticeSessionScreen` pinta el indicador de posición, el chip de nivel
  y el icono del botón de escuchar.
- `:core:domain`: `PracticeItem` (en `practice/PracticeSessionResult.kt`) gana `cefrLevel:
  CefrLevel`, poblado por `ObtainPracticeSession` a partir de `CatalogPhrase.level` al unir la
  tanda con el catálogo. El tamaño total de la tanda ya está disponible en
  `PracticeSessionResult.Ready.items`.
- `:core:designsystem`: no se añade ningún componente `Ds*` nuevo en esta iteración (se usan
  `Text`/`Icon` de Material 3 en la propia feature); si una segunda pantalla necesitara el mismo
  chip de nivel, se promocionaría entonces, según la convención del proyecto.
- Ningún cambio en `:core:data`, `:core:platform`, Room, DataStore ni en el catálogo JSON.
- Ningún ADR afectado: es una exposición en la UI de datos que ya existen (`CatalogPhrase.level`,
  tamaño de la tanda); no cambia ninguna regla de ADR-0012.

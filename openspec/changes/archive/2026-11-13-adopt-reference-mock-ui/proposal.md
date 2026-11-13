# Proposal

## Why

El usuario ha entregado un mock de referencia completo y pixel-exacto (HTML de las 5 pantallas, tokens
de color/tipografía/forma, iconografía vectorial e imágenes de referencia) para que la app coincida con
él con la máxima fidelidad. El diseño visual actual es el resultado de un ciclo de rediseños previos
(`new-style`, `fix-style-pass-two`, `fix-ui-pass-three`, `redesign-app-ui`, `fix-redesign-ui-mismatches`)
basados en mocks anteriores, y aunque comparte algunos tokens con el nuevo mock (el acento índigo
`#5B3DF5` ya coincide), difiere en tipografía, en varios tokens de color y forma, y usa iconos Material
donde el mock exige iconografía vectorial propia. Conviene formalizar ahora la adopción completa de este
mock como fuente de verdad visual, en vez de seguir ajustando parcialmente.

## What Changes

- Sustituir la tipografía actual (Baloo 2 + Plus Jakarta Sans) por Space Grotesk + Inter, empaquetadas
  como fuentes locales (offline-first, ADR-0013), con los tamaños/pesos/tracking exactos del mock.
- Completar los tokens de color de `:core:designsystem` con los del mock (`accentStrong`, `accentSoft`,
  `textSecondary` unificado, `textOnLevel`, `track`, `trackSoft`, `dashedBorder`, `disabled`, `hintBg`/
  `hintText`, `rankUpText`, `fabContent`, los tres tonos de éxito y de error, y los colores exactos de
  cada insignia de rango) y corregir `background` a `#F7F5FF`.
- Añadir los radios y dimensiones que faltan (panel/zona discontinua 20dp, FAB 18dp, 14dp para
  controles de ajustes y mensaje de rango, alto y medidas exactas de la barra de navegación, del botón
  Escuchar, del FAB de revertir y de los controles de Ajustes).
- **BREAKING**: sustituir los iconos Material (`Search`, `Close`, `Check`, `Refresh`, `Undo`,
  `CheckCircle`) por los vectores propios del mock (`ic_eye`, `ic_close`, `ic_check*`, `ic_new_session`,
  `ic_undo_previous`, insignia compuesta, `ic_volume`, `ic_nav_*`, `ic_plus`/`ic_minus`), importados como
  `VectorDrawable` desde `design/mock/icons/android-drawable/`.
- Añadir un cuadro con borde discontinuo y el texto de ayuda "Traduce la frase mentalmente y luego
  comprueba la traducción" en la pantalla de práctica, visible solo mientras la traducción está oculta.
- Rehacer la insignia de "sesión completada" como composición de hexágonos en vez de un icono simple.
- Sustituir el selector de nivel CEFR de Ajustes (segmented control de Material 3) por una rejilla de
  botones independientes, y ajustar las medidas y colores del stepper de frases por tanda.
- Ajustar colores y medidas exactas de: cabecera de práctica, tarjeta de frase, panel de traducción,
  botones de autoevaluación, FAB de revertir, tarjeta hero y filas de rango de Progreso, y barra de
  navegación inferior (incluida la unificación del gris de etiqueta inactiva, inconsistente en el propio
  mock entre `#5A5670` y `#5A6070`).
- Copiar el mock a `design/mock/` en el repositorio como fuente de verdad citable.

## Capabilities

### New Capabilities

(ninguna; este cambio no introduce comportamiento de sistema nuevo, solo ajusta la presentación visual
de capacidades existentes)

### Modified Capabilities

- `practice-session`: nuevo requisito — la pantalla MUST mostrar un mensaje de ayuda para traducir
  mentalmente la frase mientras la traducción está oculta, y MUST dejar de mostrarlo al revelarla. Es un
  cambio de comportamiento visible (contenido nuevo en pantalla), no solo de estilo.

## Impact

- **Código**: `core/designsystem` (tema, tokens, formas, dimensiones, nuevos recursos de fuente y de
  icono), `feature/session` (cabecera, tarjeta de frase, nuevo cuadro de ayuda, panel de traducción,
  botones de autoevaluación, FAB de revertir, insignia de sesión completada), `feature/settings`
  (selector de nivel, stepper), y el componente de navegación inferior compartido (módulo `:app` o
  donde viva).
- **Specs**: delta en `openspec/specs/practice-session/spec.md` (nuevo requisito del cuadro de ayuda).
  El resto de capacidades (`settings-screen`, `progress-overview`, `level-badges`, `phrase-audio`,
  `app-navigation`) no cambian de requisitos, solo de implementación visual.
- **Dependencias**: añade fuentes Space Grotesk e Inter (`.ttf`) y recursos `VectorDrawable` nuevos; no
  añade librerías.
- **ADR**: ninguno queda contradicho. Respeta el alcance de ADR-0012 (no cambia el modelo de tandas,
  niveles ni racha) y la arquitectura de ADR-0010 (los cambios de tokens/componentes viven en
  `:core:designsystem`, las features siguen dependiendo solo de `:core:domain` y `:core:designsystem`).

## Fuera de alcance

- Cualquier cambio de comportamiento del modelo de práctica, niveles, racha o repetición espaciada
  (ADR-0012 y sucesivos) — este cambio es puramente de presentación salvo el cuadro de ayuda señalado.
- Tema oscuro: el mock no define una variante oscura; el esquema oscuro actual se mantiene derivado
  automáticamente hasta que exista un mock específico.
- Infraestructura de tests de captura (Roborazzi/Paparazzi) si el proyecto no la tiene ya configurada;
  se señalará como fuera de alcance en `design.md`/`tasks.md` si no existe, en vez de añadirla aquí.
- Niveles CEFR por encima de C1 (el mock solo ilustra A1-C1 en Ajustes); si el catálogo real admite más
  niveles, se documentará en `design.md` sin ampliar aquí el alcance del propio catálogo.

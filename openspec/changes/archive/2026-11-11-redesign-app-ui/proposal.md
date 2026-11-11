# Proposal

## Why

El usuario ha diseñado un mock visual (paleta, tipografía, formas) para las pantallas de Practicar,
Progreso y Ajustes, y una pantalla de sesión completada con desglose de resultados que hoy no existe
(hoy solo hay un texto y un botón). Queremos acercar la app a ese mock reutilizando lo que ya está
construido (insignias de nivel, nivel CEFR, navegación inferior) en vez de dejar la interfaz actual,
puramente Material3 sin estilo propio, como la cara visible del producto.

## What Changes

- Actualiza los tokens de `:core:designsystem` (`AppTheme`: color, tipografía, radios de forma) para
  adoptar la paleta e identidad visual del mock.
- Añade componentes `Ds*` nuevos en `:core:designsystem`, reutilizados por al menos dos pantallas:
  tarjeta de estadística (recuento con etiqueta), fila de nivel con barra de progreso proporcional
  (envuelve `DsLevelBadge`), pastilla de posición "X de Y", y selector segmentado de nivel CEFR.
- Reestiliza `feature:session` (tarjeta de práctica, botón de revelar, botón flotante de revertir),
  `feature:progress` (tarjeta destacada y filas por nivel) y `feature:settings` (selector de nivel
  CEFR, stepper de tamaño de tanda) con los nuevos tokens y componentes, sin alterar su
  comportamiento salvo donde se indica a continuación.
- **Nuevo**: al completar la tanda activa, la pantalla de práctica muestra el número de aciertos, de
  fallos y de frases que subieron de nivel de consolidación durante esa tanda, además del mensaje
  positivo y la opción de generar sesión que ya existen; añade también un enlace a la pantalla de
  Progreso desde ese mismo mensaje.
- Corrige `ObtainProgressOverview`/`ProgressOverviewResult` en `:core:domain`, que todavía exponen la
  forma pre-ADR-0018 (`certified`/`total` de la tanda activa) en vez de la forma que exige el spec
  `progress-overview` ya vigente (total disponible del nivel CEFR vigente + recuento por nivel). Esto
  no es un requisito nuevo: es terminar de implementar un spec que ya estaba aprobado.

## Fuera de alcance

- No se cambia el método de práctica: composición de tanda por cuotas (ADR-0016/0017), resolución
  instantánea de nivel (ADR-0015/0017) ni repetición por contador de sesiones (ADR-0017). Ningún ADR
  de método queda contradicho.
- No se introduce ningún concepto nuevo de nivel, racha o insignia: el mock reutiliza el nivel CEFR
  (`CefrLevel`, A1-C1) y las insignias de nivel de consolidación (`level-badges`,
  Sin rango/Bronce/Plata/Oro/Platino/Diamante) que ya existen.
- No se cambia el flujo de revelar-traducción-y-autoevaluar: el spec `practice-session` ya exige que
  revelar muestre la traducción y ofrezca acierto/fallo en la misma pantalla; adoptar el layout del
  artboard "Alternativa B · audio en la tarjeta" del mock es un cambio de estilo (dónde vive cada
  elemento dentro de la tarjeta), no de flujo.
- El control de escuchar (`phrase-audio`) sigue mostrando icono y etiqueta de texto visibles, tal como
  exige su spec vigente; el mock lo mueve a la cabecera de la tarjeta pero conserva una etiqueta
  visible (una pastilla icono+texto, igual que las insignias de CEFR y rango de la misma cabecera),
  así que `phrase-audio` no necesita un delta.
- No se toca la pantalla de Ajustes más allá del estilo: los límites de N (10-30), la validación y la
  alineación del stepper con el campo de texto (requisito ya vigente) se conservan tal cual.
- No se crea ningún ADR nuevo: ningún ADR aceptado queda contradicho por este cambio.

## Capabilities

### New Capabilities

(ninguna)

### Modified Capabilities

- `practice-session`: el mensaje de tanda completada añade el desglose de aciertos, fallos y subidas
  de nivel de la tanda que acaba de completarse, y un enlace a la pantalla de Progreso.

## Impact

- `:core:designsystem` — tokens de `AppTheme` (color, tipografía, radios) y componentes `Ds*` nuevos
  (tarjeta de estadística, fila de nivel con barra, pastilla de posición, selector segmentado de CEFR).
- `:core:domain` — nuevo cálculo de aciertos/fallos/subidas de nivel de la tanda completada (apoyado en
  `practice/*` y `spacedrepetition/*`); corrección de `progress/ObtainProgressOverview.kt` y
  `ProgressOverviewResult.kt` a la forma del spec vigente.
- `:feature:session` — `PracticeSessionScreen.kt`, `PracticeSessionUiState.kt`,
  `PracticeSessionViewModel.kt` (nuevo estado con el desglose de tanda completada y navegación a
  Progreso).
- `:feature:progress` — `ProgressScreen.kt` (barra por fila, tarjeta destacada con los nuevos
  componentes).
- `:feature:settings` — `SettingsScreen.kt` (selector segmentado de CEFR con los nuevos componentes).
- `:app` — ninguna pantalla ni destino nuevo; la navegación de la bottom bar (`FraseAFraseApp.kt`) no
  cambia. El nuevo enlace "Ver mi progreso" reutiliza la navegación a la pestaña Progreso que ya existe.

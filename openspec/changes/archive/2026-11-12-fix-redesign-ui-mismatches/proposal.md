# Proposal

## Why

El cambio `redesign-app-ui` (ya archivado) rediseñó la app según un mock visual, pero tras varios
intentos el resultado no se parece al mock y tiene incongruencias concretas: botones sin icono ni
altura adecuada, un mensaje de progreso sin caja destacada, un control de escuchar que debería ser
solo un icono, títulos de pantalla que no encajan con el resto del diseño, la tarjeta de "frases
disponibles" sin su tratamiento visual de fondo, chips de resultado sin colores diferenciados y de
alturas distintas, y un botón de deshacer sin su icono correcto y mal posicionado. Se corrigen antes
de seguir construyendo sobre este rediseño.

## What Changes

- **Practicar**: los botones "Mostrar traducción", "Fallo" y "Acierto" pasan a tener icono, 56dp de
  alto y margen inferior ajustado respecto a la barra de navegación.
- **Practicar**: el mensaje "Te faltan N aciertos para subir a X" se muestra dentro de una caja con
  color de fondo e icono de insignia, con las partes numéricas/de nivel en negrita.
- **Practicar**: el control de escuchar la traducción pasa de pastilla con icono + texto a un botón
  de icono simple (altavoz). **BREAKING** (de cara al requisito existente de `phrase-audio` que
  exige texto visible junto al icono: ver Modified Capabilities).
- **Practicar**: el botón de generar sesión nueva pasa a tener icono y 56dp de alto.
- **Practicar**: el botón flotante de deshacer la última autoevaluación usa un icono de
  recarga/deshacer en vez de una flecha de "volver atrás", y se reubica para quedar apilado encima
  del botón principal en vez de superpuesto.
- **Practicar**: el botón flotante de deshacer se oculta también al revelar la frase siguiente, no
  solo al autoevaluar de nuevo o generar sesión (nuevo comportamiento acordado: revelar implica que
  el usuario ya no quiere deshacer la respuesta anterior).
- **Progreso** y **Ajustes**: los títulos de cabecera dejan la `TopAppBar` de Material 3 por defecto
  (fondo blanco, tipografía pequeña) por el mismo patrón de título grande sin barra propia que ya usa
  Practicar.
- **Progreso**: la tarjeta de "Frases disponibles" pasa de un `DsStatTile` centrado a una tarjeta con
  fondo de color, decoración y texto alineado a la izquierda, como en el mock.
- **Practicar**: los chips de aciertos, fallos y subidas de rango del mensaje de tanda completada
  pasan a usar colores diferenciados (verde, rojo, naranja) y a tener la misma altura entre sí.
- **Ajustes**: el botón "Guardar" pasa a tener icono, igual que el resto de botones primarios.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `phrase-audio`: el requisito "Botón de escuchar tras revelar" exige hoy que el control muestre
  "un icono de audio junto a su texto, en todos los estados en los que el control es visible". El
  mock pide un icono simple sin texto visible; se modifica el requisito para que el control sea
  icono-solo, con descripción accesible equivalente en vez de texto visible.
- `practice-session`: el requisito "Botón flotante para revertir la última autoevaluación" lista
  las condiciones en las que el botón no se muestra (antes de la primera autoevaluación, en
  `busy`, durante la transición de insignia). Se añade una condición nueva: tampoco se muestra
  mientras la frase actual está revelada.

## Impact

- `:feature:session`: `PracticeSessionScreen.kt`, `PracticeSessionPhraseCard.kt`,
  `PracticeSessionRevertButton.kt`, `PracticeSessionViewModel.kt`, recursos `strings.xml`.
- `:feature:settings`: `SettingsScreen.kt` (icono del botón "Guardar").
- `:feature:progress`: `ProgressScreen.kt` (cabecera y tarjeta de "Frases disponibles").
- `:core:designsystem`: componentes nuevos para el mensaje con caja/icono y para la tarjeta
  destacada de Progreso; parámetro de color en `DsStatTile` (o variante) para los chips de
  resultado.
- Tests unitarios e instrumentados de `:feature:session`, `:feature:progress` y
  `:feature:settings` afectados por estos cambios de UI y de `PracticeSessionViewModel`.
- Ningún ADR se contradice: el método de práctica (ADR-0012) y la arquitectura de módulos
  (ADR-0010) no cambian, solo la presentación y una condición adicional de visibilidad de un
  control ya existente.

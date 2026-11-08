# Proposal

## Why

Tres defectos de interfaz afectan hoy al uso real de la app:

1. En Ajustes, los botones +/- del tamaño de tanda quedan por debajo de la línea del campo de
   texto en vez de alineados con él.
2. En Practicar, cuando la frase o la traducción son largas, los botones de revelar o autoevaluar
   pueden quedar fuera de la pantalla visible, bloqueando el avance de la tanda.
3. En Progreso, el total por nivel de consolidación cuenta frases del catálogo de cualquier nivel
   CEFR, no solo del nivel vigente en ajustes, lo que muestra un número que no corresponde al
   nivel que el usuario está practicando.

## What Changes

- Alinear los botones +/- de Ajustes con la línea visible del campo de texto del tamaño de tanda,
  en vez de centrarlos respecto a la altura total del control (que incluye el hueco reservado para
  el texto de ayuda).
- En Practicar, hacer que el contenido de la frase y su traducción se desplace con scroll cuando no
  cabe en pantalla, de forma que el control de revelar o los de autoevaluar permanezcan siempre
  visibles y pulsables, sea cual sea la longitud del texto.
- En Progreso, calcular el total por nivel de consolidación y el total de frases disponibles solo
  sobre las frases del catálogo del nivel CEFR vigente en ajustes, en vez de sobre todo el catálogo.

## Capabilities

### New Capabilities

(ninguna)

### Modified Capabilities

- `settings-screen`: el stepper +/- del tamaño de tanda debe alinearse con la línea del campo de
  texto, no con la altura total del control.
- `practice-session`: la pantalla debe mantener siempre visibles y pulsables los controles de
  revelar y autoevaluar, permitiendo scroll sobre la frase y la traducción cuando no caben.
- `progress-overview`: el total por nivel de consolidación y el total de frases disponibles deben
  calcularse solo sobre las frases del catálogo del nivel CEFR vigente, no sobre todo el catálogo.

## Impact

- `:feature:settings` - `SettingsScreen.kt` (disposición del stepper +/- junto al campo de texto).
- `:feature:session` - `PracticeSessionScreen.kt` (contenido desplazable, controles de acción
  fijos fuera del área de scroll).
- `:core:domain` - `ObtainProgressOverview` (filtrar el catálogo por el nivel CEFR vigente antes de
  calcular `byLevel`; necesita leer `UserSettings` además de `CatalogRepository` y
  `PracticeRepository`).
- `:feature:progress` - `ProgressViewModel.kt` (sin cambios de contrato; sigue leyendo
  `ObtainUserSettings` para mostrar el nivel CEFR, ahora también usado por el caso de uso).

## Fuera de alcance

- Extraer el botón de escuchar a un control flotante independiente de la traducción: la prioridad
  es que los botones de acción nunca desaparezcan, y el scroll ya lo garantiza sin tocar el botón
  de escuchar. Se deja como alternativa para una exploración posterior si el scroll no resulta
  suficiente en la práctica.
- Reducir el espacio entre el título y la frase en Practicar: es un ajuste cosmético independiente
  de que los botones aparezcan siempre, que ya queda garantizado por el scroll.
- Mostrar en Progreso un indicador del total de frases del catálogo fuera del nivel CEFR vigente
  ("hay más"): el usuario lo plantea como posible mejora, no como requisito; se deja para una
  propuesta posterior si se confirma que hace falta.

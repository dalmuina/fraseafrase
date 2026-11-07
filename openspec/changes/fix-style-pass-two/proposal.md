# Proposal

## Why

El cambio `new-style` fijó la paleta, tipografía y esquinas definitivas comparando los mocks de
alta fidelidad, pero una revisión posterior contra esos mismos mocks encontró seis defectos
visuales y uno de comportamiento que no quedaron cubiertos: la pantalla de Practicar no muestra el
indicador de posición en su sitio (cabecera + barra de progreso) del mock, varios controles quedan
recortados o descentrados por un layout demasiado centrado, la tarjeta destacada de Progreso no usa
el estilo del mock, el texto de las insignias de nivel es inconsistente, y el stepper de N en
Ajustes permite bajar de su propio mínimo (10) sin que el control lo impida. Se corrigen ahora,
como segunda pasada de estilo, antes de que se acumulen más desviaciones del mock.

## What Changes

- Practicar: la cabecera de la pantalla pasa a mostrar el título "Practicar", el indicador "Frase X
  de Y" como una píldora con el color primario, y una barra de progreso lineal proporcional a la
  posición en la tanda, todo fijo en la parte superior (antes del contenido desplazable).
- Practicar: el contenido deja de centrarse como bloque; la tarjeta de la frase, el botón "Mostrar
  traducción", la tarjeta de traducción y los botones "Fallo"/"Acierto" pasan a ocupar el ancho
  disponible, para que ningún texto de botón quede recortado.
- Progreso: la tarjeta destacada pasa a usar el color primario de fondo (con su color de texto
  asociado) y a mostrar "Frases disponibles" y el número total en líneas separadas, seguido de
  "Nivel actual: <nivel>", como en el mock.
- Insignias de nivel (`DsLevelBadge`) y nivel CEFR en Practicar y Ajustes: la etiqueta de texto pasa
  a un estilo tipográfico fijo y más pequeño, consistente entre todos los niveles y pantallas, en
  vez de heredar el estilo de texto por defecto.
- Ajustes: el botón "−" del tamaño de tanda N se deshabilita cuando N ya es 10 (el mínimo) y el
  botón "+" se deshabilita cuando N ya es 30 (el máximo), para que no se pueda bajar del mínimo ni
  subir del máximo desde los propios botones.
- Ajustes: los botones "−"/"+" del tamaño de tanda pasan a tener la misma altura que el campo de
  texto que rodean y a quedar centrados verticalmente respecto a él.

## Capabilities

### New Capabilities

(ninguna)

### Modified Capabilities

- `settings-screen`: el control de tamaño de tanda N gana una restricción de interacción - los
  botones +/- no deben poder llevar N fuera de [10, 30], además de la validación de texto libre ya
  existente.

## Impact

- `:feature:session` (`PracticeSessionScreen.kt`): cabecera fija con título, píldora de posición y
  barra de progreso; layout de ancho completo en vez de centrado.
- `:feature:progress` (`ProgressScreen.kt`): estilo de la tarjeta destacada.
- `:feature:settings` (`SettingsScreen.kt`, `SettingsScreenTest.kt`): estado habilitado/deshabilitado
  de los botones +/-, y su tamaño/alineación respecto al campo de texto.
- `:core:designsystem` (`DsLevelBadge.kt`, posiblemente `Type.kt`): estilo tipográfico de la
  etiqueta de insignia.
- No afecta a `:core:domain`, `:core:data` ni `:core:platform`: es un cambio de presentación y de
  interacción de un control de la UI de ajustes.
- ADR afectados: ninguno nuevo. Respeta el alcance del ADR-0012 (no cambia reglas de práctica,
  racha, certificación ni repetición espaciada) y las decisiones ya fijadas en el ADR-0010
  (dependencias de módulo) y en el design de `new-style` (paleta, tipografía, esquinas), que esta
  propuesta ajusta en su aplicación concreta, no en sus valores base.

## Fuera de alcance

- Cambiar la paleta de color, la tipografía base o las esquinas ya fijadas por `new-style`: esta
  propuesta ajusta cómo se aplican en estos puntos concretos, no sus valores.
- Cualquier pantalla o componente no mencionado en "What Changes" (por ejemplo, el comportamiento
  de guardado de Ajustes, la lógica de racha o de repetición espaciada).
- Añadir una paleta de "colores semánticos" nueva o un sistema de tokens de tipografía distinto del
  ya existente en `:core:designsystem`.

# Design

## Context

Tres pantallas de Compose ya implementadas necesitan ajustes de disposición y una fuente de datos:

- `SettingsScreen.kt` (`:feature:settings`): `BatchSizeCard` pone el `OutlinedTextField` y los dos
  `IconButton` en un `Row` con `verticalAlignment = Alignment.CenterVertically`. El `OutlinedTextField`
  siempre recibe un `supportingText` no nulo (aunque esté vacío cuando el valor es válido), así que
  Material 3 reserva espacio para esa línea incluso sin error, y el `Row` centra los botones de 56dp
  respecto a esa altura total, no respecto a la caja visible del campo. De ahí el desplazamiento hacia
  abajo que reporta el usuario.
- `PracticeSessionScreen.kt` (`:feature:session`): todo el contenido (frase, traducción, botón de
  revelar o botones de autoevaluación) vive en un único `Box` con `contentAlignment = Alignment.Center`
  y una `Column` sin `verticalScroll`. Con texto largo, la `Column` mide más que la pantalla disponible
  y el `Box`/`Scaffold` la recortan: los botones, al ir después de las tarjetas en la `Column`, son lo
  primero en quedar fuera del área visible.
- `ObtainProgressOverview` (`:core:domain`) ya depende de `CatalogRepository` y `PracticeRepository`,
  pero no de los ajustes del usuario: `byLevel()` recorre `loaded.phrases` (todo el catálogo) sin
  filtrar por `CatalogPhrase.level` (el nivel CEFR), así que cuenta frases de niveles que el usuario no
  tiene seleccionados.

Ver proposal.md - Why para la motivación completa de los tres puntos.

## Goals / Non-Goals

**Goals:**
- Que el stepper +/- de Ajustes quede visualmente alineado con la caja del campo de texto, con y sin
  texto de error visible.
- Que en Practicar el control de revelar, o los de acierto/fallo, estén siempre visibles y pulsables,
  cualquiera que sea el tamaño de la frase o la traducción.
- Que el recuento por nivel de consolidación y el total de frases disponibles en Progreso reflejen
  solo el catálogo del nivel CEFR vigente en ajustes.

**Non-Goals:**
- Extraer el botón de escuchar a un control flotante, o reorganizar el espacio entre el título y la
  frase en Practicar (ver "Fuera de alcance" en proposal.md). Si el scroll no resulta suficiente en el
  uso real, se explorará en un cambio posterior.
- Mostrar en Progreso un indicador de "hay más frases en otros niveles" (fuera de alcance, ver
  proposal.md).
- Cambiar cómo se componen las tandas (`ComposeNextBatch`, `RegenerateActiveBatchForLevel`): ya filtran
  por nivel CEFR correctamente; solo el recuento de progreso no lo hacía.

## Decisions

### D1: Alinear el stepper con `Alignment.Top` en vez de `CenterVertically`

En `BatchSizeCard`, cambiar el `Row` que contiene `DecreaseBatchSizeButton`, `OutlinedTextField` e
`IncreaseBatchSizeButton` de `verticalAlignment = Alignment.CenterVertically` a
`Alignment.Top`. Los botones ya tienen `Modifier.size(TEXT_FIELD_HEIGHT)` (56dp), que coincide con la
caja visible por defecto de `OutlinedTextField` en Material 3; alinear por arriba hace que ambos
arranquen a la misma altura, sin que el hueco reservado para `supportingText` (vacío o con error)
desplace los botones hacia abajo.

**Alternativas consideradas:**
- *Medir la altura real del campo con `onGloballyPositioned` y fijarla en los botones*: añade estado y
  una pasada de medición extra para un problema que `Alignment.Top` resuelve de forma declarativa y sin
  recomposición adicional. Descartada por complejidad desproporcionada.
- *Quitar el parámetro `supportingText` cuando el valor es válido*: eliminaría el hueco reservado, pero
  entonces el campo cambiaría de altura total al pasar de válido a no válido, desplazando el resto de
  la tarjeta verticalmente cada vez que el usuario escribe un valor fuera de rango. Peor experiencia que
  reservar el hueco siempre.

### D2: Contenido desplazable con los controles de acción fuera del área de scroll

En `PracticeSessionScreen`, separar `PracticingContent` en dos zonas dentro de una `Column` que ocupa
el alto disponible: una zona superior con `Modifier.weight(1f).verticalScroll(rememberScrollState())`
que contiene `PhraseCard` (y `TranslationCard` si está revelada), y una zona inferior fija, fuera del
`verticalScroll`, con el botón de revelar o los botones de acierto/fallo. Así, cuando el contenido no
cabe, el usuario puede desplazarlo, pero los controles de acción nunca se mueven ni se recortan: siempre
ocupan la misma posición al final de la pantalla.

El mensaje de tanda completada (`CompletedMessage`) no cambia: su texto es corto y fijo, no necesita
scroll.

**Alternativas consideradas (de la propuesta inicial del usuario):**
- *Extraer el botón de escuchar a un `FloatingActionButton` junto a la traducción*: ahorra algo de
  espacio vertical, pero no resuelve el caso límite de una frase en español muy larga (antes de
  revelar, cuando el botón de escuchar ni siquiera está en pantalla) ni el de una traducción larga sin
  botón de escuchar. El scroll cubre ambos casos sin depender de cuánto espacio ahorre un control
  concreto. Se deja anotada como mejora independiente, no como alternativa excluyente.
- *Subir el bloque de la frase reduciendo el espacio entre el título y la frase*: es una ganancia fija
  de espacio (unos pocos `dp`), no una garantía: con suficiente texto, el desbordamiento reaparece. No
  resuelve el requisito de la propuesta ("los botones tienen que aparecer siempre"); el scroll sí.

### D3: `ObtainProgressOverview` filtra el catálogo por el nivel CEFR vigente antes de contar

Inyectar `ObtainUserSettings` (`:core:domain`, ya usado por `ProgressViewModel` y por
`RegenerateActiveBatchForLevel`) en `ObtainProgressOverview`. Antes de construir `byLevel`, filtrar
`loaded.phrases` a las que tengan `CatalogPhrase.level == settings.cefrLevel`, y pasar solo esa lista
filtrada a `byLevel(...)`. `ProgressOverviewResult.Available.byLevel` no cambia de forma (sigue siendo
`Map<Level, Int>` de 0 a `Level.MAX_LEVEL`), solo su contenido queda acotado al nivel CEFR vigente.
`ProgressViewModel` no necesita cambios: `totalAvailable` sigue siendo `result.byLevel.values.sum()`,
que ahora ya refleja solo el nivel CEFR vigente.

`ObtainProgressOverview` pasa a depender de un puerto adicional del dominio (`UserSettingsRepository`
vía `ObtainUserSettings`), igual que ya hace `RegenerateActiveBatchForLevel`: no introduce ninguna
dependencia nueva de infraestructura ni cruza la regla de módulos del ADR-0010.

**Alternativas consideradas:**
- *Filtrar en `ProgressViewModel` en vez de en el caso de uso*: dejaría la regla de negocio ("el
  progreso se cuenta por nivel CEFR vigente") en la capa de presentación, que debería ser una
  proyección sin lógica (ADR-0006). `ObtainProgressOverview` ya es el lugar que combina catálogo y
  progreso; es la pieza correcta para aplicar el filtro.
- *Añadir el nivel CEFR como parámetro de `invoke()` en vez de leerlo con `ObtainUserSettings`*:
  obligaría a quien llama (`ProgressViewModel`) a leer los ajustes y pasarlos, duplicando la lectura que
  el propio ViewModel ya hace para mostrar `cefrLevel` en la UI. Inyectar `ObtainUserSettings`
  directamente evita esa duplicación y mantiene la firma de `invoke()` sin parámetros, como hoy.

## Risks / Trade-offs

- [Los tests existentes de `ObtainProgressOverview` no fijan un nivel CEFR en las frases del catálogo
  de prueba (`aCatalogPhrase`), así que al añadir el filtro algunos podrían dejar de reflejar lo que
  prueban] → Revisar cada test de `ObtainProgressOverviewTest` al implementarlo: los que no comprueban
  el filtrado deben construir su catálogo con el mismo nivel CEFR que los ajustes del `FakeUserSettings`
  usado (o el nivel CEFR por defecto), y los nuevos escenarios de "frases de otros niveles CEFR no
  cuentan" son los que deben variar el nivel CEFR entre frases.
- [`verticalScroll` dentro de una `Column` con `weight(1f)` puede interactuar con el `LinearProgressIndicator`
  del `topBar` si el contenido es muy corto y no hay nada que desplazar] → No es un riesgo real: con
  contenido corto no hay scroll posible y el diseño ya reserva la zona inferior fija para los
  controles de acción, independientemente de si hay scroll o no.
- [Cambiar `verticalAlignment` del `Row` de `BatchSizeCard` podría desalinear otros elementos si en el
  futuro se añaden más controles a esa fila] → No aplica a este cambio (solo hay dos botones y un
  campo); si se añaden más controles, su alineación se revisará en ese momento.

## Estrategia de test

- `SettingsScreenTest` (`androidTest`, Compose UI Test): nuevo test que lee los límites (`bounds`) de
  los nodos de los controles de disminuir/aumentar y del campo de texto, con y sin error visible, y
  comprueba que sus bordes superiores coinciden (dentro de una tolerancia de medición, si hiciera
  falta por redondeo de `dp` a píxeles).
- `PracticeSessionScreenTest` (`androidTest`, Compose UI Test): nuevos tests con una `PracticeItem` de
  frase y traducción largas (texto de prueba suficientemente largo para forzar el desbordamiento en el
  tamaño de pantalla del test) que comprueban que el control de revelar, o los de acierto/fallo, están
  visibles (`assertIsDisplayed()`) sin necesitar `performScrollTo()` previo, y que siguen visibles tras
  desplazar el contenido de la frase/traducción.
- `ObtainProgressOverviewTest` (`:core:domain`, JUnit + Kotest, con `FakeCatalogRepository`,
  `FakePracticeRepository` y `FakeUserSettingsRepository`/`FakeSettingsRepository` de `:core:testing`):
  tests nuevos para los escenarios de "frases de otros niveles CEFR no cuentan" en `byLevel` y en el
  total disponible, y revisión de los tests existentes para fijar el nivel CEFR de sus fixtures.
- `ProgressViewModelTest`: no necesita tests nuevos de comportamiento (su contrato no cambia), pero sus
  fixtures (`ProgressFixtures.kt`) deben seguir construyendo un `ObtainProgressOverview` válido con el
  nuevo parámetro de ajustes.

# Tasks

## 1. Dominio: nivel CEFR en `PracticeItem`

- [ ] 1.1 En `core/domain/.../practice/ObtainPracticeSessionTest.kt`, añadir un test que compruebe
  que el `PracticeItem` resultante lleva el `cefrLevel` de la `CatalogPhrase` correspondiente
  (spec `practice-session`, escenario "Nivel CEFR visible sin revelar"); ejecutarlo y comprobar
  que falla porque `PracticeItem` no tiene ese campo.
- [ ] 1.2 Añadir `cefrLevel: CefrLevel` a `PracticeItem`
  (`core/domain/.../practice/PracticeSessionResult.kt`) y poblarlo en
  `ObtainPracticeSession.items()` desde `phrase.level`; verificar que el test de 1.1 pasa y que
  `./gradlew :core:domain:test` sigue en verde.

## 2. Presentación: contador de posición en el `ViewModel`

- [ ] 2.1 En `feature/session/.../PracticeSessionViewModelTest.kt`, añadir un test que, al cargar
  una tanda de N frases por primera vez, compruebe `Practicing.index == 1` y
  `Practicing.total == N` (spec `practice-session`, escenario "Primera frase de una tanda de
  cinco"); ejecutarlo y comprobar que falla porque `Practicing` no tiene esos campos.
- [ ] 2.2 Añadir `index: Int` y `total: Int` a `PracticeSessionUiState.Practicing`
  (`feature/session/.../PracticeSessionUiState.kt`) y poblarlos en `toScreenState()` con
  `index = 1`, `total = items.size`; verificar que el test de 2.1 pasa.
- [ ] 2.3 Añadir un test en `PracticeSessionViewModelTest.kt` que, tras autoevaluar una frase en
  una tanda de 3, compruebe que el índice avanza de 1 a 2 (spec, escenario "El indicador avanza
  tras autoevaluar"); ejecutarlo y comprobar que falla porque la recarga tras `answer()` siempre
  reinicia a 1.
- [ ] 2.4 En `PracticeSessionViewModel.answer()`, sustituir la llamada a `reload()` por una nueva
  función que recalcule `index` a partir del estado anterior (`index + 1`, o `1` si `index ==
  total`) y mantenga `total` de la lectura fresca (design D1); verificar que el test de 2.3 pasa.
- [ ] 2.5 Añadir un test en `PracticeSessionViewModelTest.kt` que, en una tanda de 2 frases,
  compruebe que tras autoevaluar la segunda frase el índice vuelve a 1 (spec, escenario "El
  indicador vuelve a 1 al dar la vuelta a la tanda"); ejecutarlo contra 2.4 y comprobar que ya
  pasa sin cambios adicionales, o ajustar el cálculo de 2.4 si no es así.
- [ ] 2.6 Añadir un test en `PracticeSessionViewModelTest.kt` que, tras un cierre confirmado hacia
  una tanda siguiente de tamaño distinto, compruebe `index == 1` y `total` igual al nuevo tamaño
  (spec, escenario "El indicador refleja el tamaño de una tanda siguiente distinto"); verificar
  que pasa ya con la ruta de `reload()` existente (D1) sin cambios adicionales.

## 3. UI: indicador de posición, nivel CEFR e icono de escuchar

- [ ] 3.1 En `feature/session/.../PracticeSessionScreenTest.kt`, añadir un test instrumentado
  (nombre `givenPracticingState_whenScreenShown_thenPositionIndicatorIsDisplayed`) que compruebe
  que la pantalla muestra el texto "1 de 5" con una tanda de 5 frases (spec `practice-session`);
  ejecutarlo y comprobar que falla porque la pantalla no pinta ningún indicador.
- [ ] 3.2 Añadir el `Text` del indicador "X de Y" en `PracticingContent`
  (`PracticeSessionScreen.kt`), usando `state.index`/`state.total` y un nuevo recurso de string
  con formato (`feature/session/src/main/res/values/strings.xml`); verificar que el test de 3.1
  pasa.
- [ ] 3.3 Añadir un test instrumentado
  (`givenPracticingState_whenScreenShown_thenCefrLevelIsDisplayed`) que compruebe que la pantalla
  muestra el nivel CEFR de `state.current` (p. ej. "B1") junto a la frase (spec, escenarios
  "Nivel CEFR visible sin revelar" y "al revelar"); ejecutarlo y comprobar que falla.
- [ ] 3.4 Añadir el `Text` del nivel CEFR en `PhraseCard` (`PracticeSessionScreen.kt`), visible
  tanto revelada como sin revelar, leyendo `item.cefrLevel`; verificar que el test de 3.3 pasa.
- [ ] 3.5 Añadir un test instrumentado
  (`givenPhraseRevealed_whenScreenShown_thenListenControlShowsAudioIcon`) que compruebe que el
  control de escuchar expone un icono (spec `phrase-audio`, escenario "El control de escuchar
  muestra un icono de audio"); ejecutarlo y comprobar que falla porque el `TextButton` solo tiene
  texto.
- [ ] 3.6 Comprobar con un build real si `Icons.Default.PlayArrow` resuelve sin tocar
  `gradle/libs.versions.toml` (design D3); si no resuelve, añadir
  `androidx.compose.material:material-icons-core` (no `-extended`) al catálogo de versiones y
  documentar la justificación en `design.md`. Envolver el contenido del `TextButton` de escuchar
  (`PhraseCard`, `PracticeSessionScreen.kt:124-129`) en un `Row` con
  `Icon(Icons.Default.PlayArrow, contentDescription = null)` seguido del `Text` existente;
  verificar que el test de 3.5 pasa y que los escenarios ya existentes de `phrase-audio` (visible
  solo al revelar, deshabilitado con `busy`/aviso de cierre) siguen pasando sin cambios.

## 4. Verificación final

- [ ] 4.1 Ejecutar `./gradlew check` (tests, ktlint, detekt) y confirmar que termina en verde.
- [ ] 4.2 Ejecutar `./gradlew koverVerify` y confirmar que se mantienen los mínimos de cobertura
  (dominio 90 %, presentación 80 %, global 70 %).

## Workflow follow-up

- Revisión de `android-architect` sobre `proposal.md`, `design.md`, las specs delta y `tasks.md`
  antes de `/opsx:apply`.
- Revisión de `code-reviewer` tras la implementación, antes de `/opsx:verify`.
- `/opsx:archive` en la rama del cambio, antes de abrir la pull request a `develop`.

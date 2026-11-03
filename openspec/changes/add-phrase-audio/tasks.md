# Tasks

## 1. Dominio: puerto, caso de uso y locale en `PracticeItem`

- [x] 1.1 Añadir el puerto `PhrasePlayer` (`fun play(text: String, locale: LanguageTag)`) en
  `com.dalmuina.fraseafrase.core.domain.audio` (`:core:domain`), siguiendo el patrón de `Clock`
  (design.md - Decisiones, puerto sin resultado); verificar con `./gradlew :core:domain:compileKotlin`.
- [x] 1.2 Añadir `FakePhrasePlayer` a `:core:testing` que registre las llamadas (`text`, `locale`)
  recibidas; verificar con `./gradlew :core:testing:compileKotlin`.
- [x] 1.3 Test: `PlayPhraseAudio`, `given a text and a locale when invoked then the player receives
  them` (`:core:domain`, usando `FakePhrasePlayer`); hacerlo fallar porque la clase no existe, luego
  implementar `PlayPhraseAudio` (`operator fun invoke`) hasta que pase.
- [x] 1.4 Test: `ObtainPracticeSession`, `given a catalog phrase with a target locale when the session
  is read then the practice item carries that locale` (spec `phrase-audio`, "Reproducción del audio en
  el locale de la frase"); hacerlo fallar añadiendo primero la aserción sobre `targetLocale`, luego
  añadir el campo `targetLocale: LanguageTag` a `PracticeItem` y rellenarlo en `ObtainPracticeSession`
  con `phrase.targetLocale` hasta que el test y los existentes de `ObtainPracticeSession` pasen. Al ser
  un campo no opcional, esto también rompe la compilación de
  `PracticeSessionScreenPreviews.kt` y del test instrumentado `PracticeSessionScreenTest.kt`
  (`:feature:session`), que construyen `PracticeItem(...)` directamente: añadirles un `targetLocale` en
  este mismo paso para que el módulo siga compilando.

## 2. Plataforma: `TextToSpeechPlayer`

- [x] 2.1 Tests de `TextToSpeechPlayerTest` (`:core:platform`, JVM, con fakes de `isReady`,
  `setLanguage` y `speak` inyectados por el constructor interno; design.md - Decisiones, seam de
  test): `given isReady returns false when play is called then speak is never invoked` (spec
  `phrase-audio`, "Motor de TTS no disponible"), `given setLanguage returns LANG_MISSING_DATA or
  LANG_NOT_SUPPORTED when play is called then speak is never invoked` (spec `phrase-audio`, "Locale de
  la frase sin voz instalada") y `given play is called twice when the second call runs then speak is
  invoked both times with QUEUE_FLUSH` (spec `phrase-audio`, "Escuchar dos veces reinicia la
  reproducción"); hacerlos fallar porque la clase no existe.
- [x] 2.2 Implementar `TextToSpeechPlayer` (constructor interno con el seam de 2.1; constructor
  `@Inject` real sobre `android.speech.tts.TextToSpeech` con inicialización asíncrona y bandera
  `ready`) hasta que los tests de 2.1 pasen; verificar con `./gradlew :core:platform:test` y
  `./gradlew :core:platform:compileDebugKotlin`.
- [ ] 2.3 Verificación manual en un emulador/dispositivo (design.md - Risks / Trade-offs, integración
  real no cubierta por 2.1): reproducir una frase con el motor TTS disponible y comprobar que no se
  lanza ni se registra ningún error con el idioma `pt-PT` desinstalado. Anotar el resultado en este
  punto de `tasks.md` al completarlo.

## 3. Presentación: `:feature:session`

- [ ] 3.1 Añadir `PracticeSessionAction.PlayAudio` (`data object`, sin parámetros; design.md -
  Decisiones).
- [ ] 3.2 Test de `PracticeSessionViewModel` (`MainDispatcherExtension`, `FakePhrasePlayer` a través de
  `PlayPhraseAudio`): `given a revealed phrase when PlayAudio is dispatched then PlayPhraseAudio
  receives its target text and locale` (spec `phrase-audio`, "Reproducción del audio en el locale de
  la frase"); hacerlo fallar antes de añadir el manejo de `PlayAudio` en `onAction` con
  `PlayPhraseAudio` como nueva dependencia inyectada del ViewModel.
- [ ] 3.3 Tests del mismo ViewModel: `given a response being registered (busy) when PlayAudio is
  dispatched then PlayPhraseAudio is not invoked` y `given the close warning open when PlayAudio is
  dispatched then PlayPhraseAudio is not invoked` (spec `phrase-audio`, "Control de escuchar
  deshabilitado fuera de la práctica activa"); hacerlos fallar antes de añadir las guardas de `busy` y
  `closeWarning` al manejo de `PlayAudio`.
- [ ] 3.4 Compose UI test de `PracticeSessionScreen` (sin ViewModel): el control de escuchar no se
  muestra sin revelar, se muestra revelada la frase e invoca `onAction(PlayAudio)` al pulsarlo, y se
  deshabilita cuando `enabled` es falso (spec `phrase-audio`, "Botón de escuchar tras revelar",
  "Control de escuchar deshabilitado fuera de la práctica activa"); hacerlo fallar antes de añadir el
  `TextButton` de escuchar a `PhraseCard` (sin icono, visible solo con `revealed`, deshabilitado igual
  que `AssessmentButtons`; design.md - Decisiones, UI) y el string `practice_listen`.
- [ ] 3.5 Verificar con `./gradlew :feature:session:test`.

## 4. Integración en `:app`

- [ ] 4.1 Añadir el binding de Hilt `bindPhrasePlayer(player: TextToSpeechPlayer): PhrasePlayer`
  (`PersistenceModule` o un `@Module` nuevo para `:core:platform`, a decidir según legibilidad;
  design.md - Decisiones, Cableado); verificar con `./gradlew :app:assembleDebug`.
- [ ] 4.2 Prueba manual en la app (emulador o dispositivo): revelar una frase, pulsar "Escuchar" y
  confirmar que se oye el audio en portugués; confirmar que el botón no aparece sin revelar.

## 5. Verificación final

- [ ] 5.1 Ejecutar `./gradlew check` (tests, ktlint, detekt) en verde.
- [ ] 5.2 Ejecutar `./gradlew koverVerify` y confirmar los mínimos de cobertura (dominio 90 %,
  presentación 80 %, global 70 %) en los módulos tocados.

## Workflow follow-up

- Ejecutar `/opsx:verify` para comprobar la implementación contra `proposal.md`, las specs y
  `tasks.md`.
- Ejecutar `/opsx:archive` en la rama del cambio, antes de la pull request.

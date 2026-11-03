# Design

## Context

`:core:platform` solo tiene `DeviceClock` (implementa el puerto `Clock` de `:core:domain`). No hay
ningún puerto de audio ni dependencia de `android.speech.tts` en el proyecto. `PracticeItem`
(`:core:domain`, `core/domain/.../practice/PracticeSessionResult.kt`) no lleva el locale de destino de
la frase, solo sus textos (`source`, `target`); `CatalogPhrase` sí tiene `targetLocale` y
`sourceLocale` (`LanguageTag`, ADR-0003), así que falta propagarlo. `ObtainPracticeSession`
(`:core:domain`) es el único lugar donde se construye `PracticeItem` a partir de `CatalogPhrase`.
`PracticeSessionScreen` (`:feature:session`) ya deshabilita sus acciones con `state.busy` y oculta
las de autoevaluación mientras `closeWarning != null`; el control de escuchar sigue el mismo patrón
(decisión del usuario: botón manual tras revelar, sin reproducción automática, con fallo silencioso
si el motor TTS no está disponible).

## Goals / Non-Goals

**Goals:**
- Puerto `PhrasePlayer` en `:core:domain` y su única implementación `TextToSpeechPlayer` en
  `:core:platform`, siguiendo el patrón de `Clock` / `DeviceClock`.
- Caso de uso `PlayPhraseAudio` para que `PracticeSessionViewModel` no dependa del puerto directamente
  (ADR-0006).
- Control de escuchar en `PracticeSessionScreen`, visible solo revelada la frase, deshabilitado con
  `busy` o con el aviso de cierre abierto.
- Fallo silencioso: ni el dominio ni la UI modelan el fallo del motor TTS como un estado distinto.

**Non-Goals:**
- Reproducción automática al revelar (decisión del usuario: control manual).
- Indicar al usuario que el TTS falló o que el idioma no está instalado.
- Selección de voz, velocidad o tono; caché de audio; precarga.
- Reconocimiento de voz o corrección automática (ADR-0012: primer cambio tras el MVP).
- Detener el audio al salir de la pantalla o al cambiar de frase: `TextToSpeech.speak` con
  `QUEUE_FLUSH` (ver más abajo) ya descarta cualquier reproducción anterior antes de empezar una
  nueva, así que no hace falta una llamada explícita de parada en el ciclo de vida de la pantalla.

## Decisions

### Puerto `PhrasePlayer` en `:core:domain`, sin resultado

```kotlin
package com.dalmuina.fraseafrase.core.domain.audio

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag

/**
 * Puerto de síntesis de voz (ADR-0010, ADR-0012). Reproduce un texto en el locale dado. No informa de
 * fallos: un motor no disponible o un locale sin voz instalada no reproducen nada (spec `phrase-audio`,
 * "Fallo silencioso del motor de TTS"). La implementación de sistema vive en `:core:platform`.
 */
interface PhrasePlayer {
    fun play(text: String, locale: LanguageTag)
}
```

`fun`, no `suspend`: `TextToSpeech.speak` ya es asíncrono internamente (encola y reproduce en su propio
hilo) y no hay ningún resultado que esperar, a diferencia de un acceso a Room o DataStore. Mantenerlo
síncrono desde el punto de vista del llamador evita una corrutina sin nada real que suspender.

**Alternativa considerada:** `suspend fun play(...): PlaybackResult` con un tipo sellado de
resultado — se descarta porque no hay nada que el llamador pueda hacer con un fallo (decisión del
usuario: fallo silencioso) y porque `TextToSpeech` no ofrece de forma fiable un callback de "terminó
correctamente" sin añadir un `UtteranceProgressListener` que esta función no necesita.

### `TextToSpeechPlayer` en `:core:platform`, con el mismo seam de test que `DeviceClock`

`android.speech.tts.TextToSpeech` no se puede instanciar en un test JVM, pero toda la lógica de
decisión de `play()` (si el motor está listo, qué hacer con el resultado de `setLanguage`, con qué
modo de cola llamar a `speak`) no depende de Android y sí hay que probarla (spec `phrase-audio`,
"Motor de TTS no disponible", "Locale de la frase sin voz instalada", "Escuchar dos veces reinicia la
reproducción"). Igual que `DeviceClock` separa `zone: () -> ZoneId` y `now: () -> Instant` de
`ZoneId::systemDefault` e `Instant::now`, `TextToSpeechPlayer` recibe esa lógica como tres funciones en
su constructor interno, inyectadas con las operaciones reales del `TextToSpeech` del dispositivo solo
en el constructor `@Inject`:

```kotlin
class TextToSpeechPlayer internal constructor(
    private val isReady: () -> Boolean,
    private val setLanguage: (Locale) -> Int,
    private val speak: (String) -> Unit,
) : PhrasePlayer {

    @Inject
    constructor(@ApplicationContext context: Context) : this(
        isReady = { ready },
        setLanguage = tts::setLanguage,
        speak = { text -> tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text) },
    )

    private var ready = false
    private val tts = TextToSpeech(context) { status -> ready = status == TextToSpeech.SUCCESS }

    override fun play(text: String, locale: LanguageTag) {
        if (!isReady()) return
        val result = setLanguage(Locale.forLanguageTag(locale.value))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) return
        speak(text)
    }
}
```

(El `tts` y `ready` reales se declaran tras el constructor secundario solo por orden de inicialización
de Kotlin; en el código definitivo se reordenan para que compile, sin cambiar el seam.)

- `isReady`, `setLanguage` y `speak` son los únicos puntos donde el código real toca
  `android.speech.tts.TextToSpeech`; `TextToSpeechPlayerTest` (`:core:platform`, JVM) los sustituye por
  fakes y cubre los tres escenarios de la spec sin ningún emulador:
  `given isReady returns false when play is called then speak is never invoked`, `given setLanguage
  returns LANG_MISSING_DATA when play is called then speak is never invoked` y `given play is called
  twice when the second call runs then speak receives QUEUE_FLUSH both times` (ver Testing).
- `setLanguage` devuelve `LANG_MISSING_DATA` o `LANG_NOT_SUPPORTED` cuando el locale no tiene voz
  instalada; ambos casos vuelven sin reproducir, sin lanzar ni registrar nada.
- `QUEUE_FLUSH` descarta cualquier reproducción en curso antes de empezar la nueva; con una sola frase
  reproducible a la vez, no hace falta `QUEUE_ADD`.
- `@ApplicationContext`: el motor vive mientras vive el proceso, igual que `PracticeDatabase`
  (`PersistenceModule`); no se llama a `tts.shutdown()` porque no hay ningún punto del ciclo de vida de
  la aplicación donde hacerlo de forma fiable (`Application.onTerminate()` no se invoca en dispositivos
  reales) y no liberar el motor hasta que el proceso termina es el mismo trato que ya recibe
  `PracticeDatabase`. Riesgo aceptado, ver Risks / Trade-offs.

**Alternativa considerada:** inicializar el motor de forma perezosa en el primer `play()` — se
descarta porque añadiría estado mutable adicional (¿ya se está inicializando?) sin cambiar el
comportamiento observable: la primera reproducción tras abrir la app ya puede perderse si el motor
tarda en inicializar, con o sin inicialización perezosa.

### `PracticeItem` gana `targetLocale`

`PracticeItem` (`core/domain/.../practice/PracticeSessionResult.kt`) añade `val targetLocale:
LanguageTag`. `ObtainPracticeSession` lo rellena con `phrase.targetLocale` al construir cada
`PracticeItem`, igual que ya hace con `source` y `target`. Es un cambio aditivo y local a ese único
punto de construcción (ver proposal.md - Impact); ningún otro caso de uso ni la UI de la tanda
(ADR-0012) lo necesitan.

### Caso de uso `PlayPhraseAudio`

```kotlin
class PlayPhraseAudio @Inject constructor(
    private val player: PhrasePlayer,
) {
    operator fun invoke(text: String, locale: LanguageTag) = player.play(text, locale)
}
```

Envoltorio directo, sin lógica propia: existe solo para que `PracticeSessionViewModel` dependa de un
caso de uso y no del puerto (ADR-0006), igual que el resto de casos de uso de la pantalla de práctica.

### `PracticeSessionAction.PlayAudio` sin parámetros

```kotlin
data object PlayAudio : PracticeSessionAction
```

El ViewModel ya tiene la frase actual en su estado (`Practicing.current`), con `target` y
`targetLocale`; la acción no necesita llevar el texto, igual que `Reveal` o `RequestClose` no llevan
el id de la frase actual. El manejo en el ViewModel:

```kotlin
PracticeSessionAction.PlayAudio -> {
    val state = _uiState.value as? PracticeSessionUiState.Practicing ?: return
    val current = state.current
    if (current != null && state.revealed && !state.busy && state.closeWarning == null) {
        playPhraseAudio(current.target, current.targetLocale)
    }
}
```

No cambia `_uiState`: reproducir audio no es una transición de estado observable por la UI (a
diferencia de `Reveal` o `Answer`), así que no hay nada que un test de `Turbine` sobre el `StateFlow`
pueda comprobar directamente; el test de esta acción verifica la llamada a `PlayPhraseAudio` con un
mock/fake, no una emisión del `StateFlow`.

### UI: botón de escuchar en `PhraseCard`, sin promoción a `:core:designsystem`

`PhraseCard` (`PracticeSessionScreen.kt`) añade un `TextButton` con el texto "Escuchar" junto a la
traducción, visible solo cuando `revealed` es `true`, deshabilitado cuando `!enabled` (mismo booleano
que ya deshabilita `AssessmentButtons`). Sin icono, igual que los botones de "Ajustes" y "Cerrar
tanda" (decisión ya tomada en `add-settings-screen`: el proyecto no tiene
`material-icons-core`). Un solo uso en una feature: no se promueve a `:core:designsystem` (ADR-0010,
regla de promoción).

### Cableado en `:app`

Nuevo `@Module` `PlatformModule` en `:app` (o una sección nueva en `PersistenceModule`, a decidir en
la tarea de implementación según cuál quede más legible) con:

```kotlin
@Binds
abstract fun bindPhrasePlayer(player: TextToSpeechPlayer): PhrasePlayer
```

### Testing

- `PhrasePlayer`: fake en `:core:testing` (`FakePhrasePlayer`) que registra las llamadas (`text`,
  `locale`) para los tests de `PlayPhraseAudio` y del ViewModel.
- `PlayPhraseAudio`: test unitario con el fake, sin corrutinas (función síncrona).
- `TextToSpeechPlayer`: test unitario en JVM (`TextToSpeechPlayerTest`, `:core:platform`) con fakes de
  `isReady`, `setLanguage` y `speak` (ver más arriba); cubre los tres escenarios de `phrase-audio` que
  viven en esta clase. Solo queda fuera del test automático la integración real con el motor de
  Android (audio audible, comportamiento exacto del SO con un paquete de voz desinstalado), verificada
  manualmente (ver Risks / Trade-offs).
- `PracticeSessionViewModel`: test con `MainDispatcherExtension`, `given a revealed phrase when
  PlayAudio is dispatched then PlayPhraseAudio is invoked with its target text and locale` y los casos
  de `busy` / `closeWarning` abiertos (spec `phrase-audio`).
- `PracticeSessionScreen`: Compose UI test, sin ViewModel: el control de escuchar aparece solo
  revelado y se deshabilita con los mismos flags que `AssessmentButtons`.

## Risks / Trade-offs

- [`TextToSpeechPlayer` no tiene test de integración real contra `android.speech.tts.TextToSpeech`] →
  Su lógica de decisión sí está cubierta por `TextToSpeechPlayerTest` (seam de funciones, igual que
  `DeviceClock`); solo queda sin automatizar la integración con el motor real de Android, de riesgo
  bajo, verificada manualmente en un emulador/dispositivo durante la tarea de implementación.
- [El motor TTS nunca se libera (`tts.shutdown()`)] → Mismo trato que `PracticeDatabase`: vive con el
  proceso. Si se detecta una fuga de recursos real (batería, memoria) en uso diario, se revisa en un
  cambio posterior; no hay evidencia de que ocurra con un único `TextToSpeech` por proceso.
- [Dispositivos sin motor TTS instalado o sin el locale `pt-PT`] → Comportamiento ya cubierto por la
  spec (fallo silencioso); no se necesita diseño adicional, pero conviene verificarlo manualmente en
  un emulador sin paquete de voz portugués instalado.

## Migration Plan

Sin datos que migrar. `targetLocale` en `PracticeItem` se calcula en cada lectura a partir del
catálogo (no se persiste); no afecta a Room ni a DataStore.

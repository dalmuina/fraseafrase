# Proposal

## Why

ADR-0012 incluye el audio (TTS) dentro del alcance del MVP: el usuario necesita poder escuchar la
frase en portugués para aprender su pronunciación, no solo leerla. `:core:platform` solo tiene
`DeviceClock`; no hay puerto de audio en el dominio ni forma de reproducir nada desde la pantalla de
práctica.

## What Changes

- Nuevo puerto `PhrasePlayer` en `:core:domain` y su implementación `TextToSpeechPlayer` en
  `:core:platform` sobre `android.speech.tts.TextToSpeech`, con el locale BCP-47 de la frase
  (ADR-0003).
- Nuevo caso de uso `PlayPhraseAudio` que envuelve el puerto (ADR-0006: los ViewModels solo dependen
  de casos de uso).
- `PracticeItem` (`:core:domain`) gana el campo `targetLocale`, propagado desde `CatalogPhrase` en
  `ObtainPracticeSession`, para que la pantalla sepa en qué idioma reproducir el audio.
- Botón de escuchar en `PracticeSessionScreen`, visible solo tras revelar la traducción, que reproduce
  el audio de la frase en portugués. Si el motor TTS no está disponible o el idioma no está instalado,
  el botón no hace nada perceptible (fallo silencioso): no se añade ningún estado de error nuevo a la
  pantalla.
- Cableado del nuevo puerto en `:app` (`PersistenceModule` o un módulo de Hilt nuevo para
  `:core:platform`).

## Capabilities

### New Capabilities

- `phrase-audio`: reproducción del audio de la frase en portugués desde la pantalla de práctica,
  incluida su disponibilidad según el estado de la pantalla (revelada, ocupada, con el aviso de cierre
  abierto) y el comportamiento ante un fallo del motor TTS.

### Modified Capabilities

(ninguna; `practice-session` no cambia sus requisitos existentes — el botón de escuchar es un
requisito nuevo y autocontenido, igual que el punto de entrada a ajustes se definió en
`settings-screen` y no como una modificación de `practice-session`)

## Impact

- `:core:domain`: puerto `PhrasePlayer`, caso de uso `PlayPhraseAudio`, campo `targetLocale` nuevo en
  `PracticeItem`.
- `:core:platform`: `TextToSpeechPlayer` (adaptador de `android.speech.tts.TextToSpeech`).
- `:core:testing`: fake de `PhrasePlayer` para los tests del caso de uso y del ViewModel.
- `:feature:session`: nueva acción `PlayAudio` en `PracticeSessionAction`, botón de escuchar en
  `PracticeSessionScreen`.
- `:app`: nuevo binding de Hilt para `PhrasePlayer`.

## Fuera de alcance

- Reconocimiento de voz y corrección automática (primer cambio tras el MVP, ADR-0012).
- Reproducción automática al revelar.
- Indicación visible de fallo del TTS.
- Selección de voz o velocidad, caché de audio.

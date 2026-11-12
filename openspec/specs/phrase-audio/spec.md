# Phrase Audio Specification

## Purpose

Permite al usuario escuchar la pronunciación de la frase en portugués desde la pantalla de práctica,
en el locale BCP-47 de la frase, como parte del método de aprendizaje del ADR-0012.

## Requirements

### Requirement: Botón de escuchar tras revelar

La pantalla de práctica MUST mostrar un control para escuchar el audio de la frase actual en
portugués únicamente cuando su traducción está revelada. Sin revelar, MUST NOT mostrarse ese
control. El control MUST mostrarse como un icono de audio (altavoz), sin texto visible, en todos
los estados en los que el control es visible, incluido cuando está deshabilitado, y MUST exponer
una descripción accesible equivalente al texto que ya no se muestra.

#### Scenario: Revelar muestra el control de escuchar

- **GIVEN** la pantalla muestra una frase sin revelar
- **WHEN** el usuario la revela
- **THEN** la pantalla muestra el control de escuchar junto a la traducción

#### Scenario: Sin revelar no hay control de escuchar

- **GIVEN** la pantalla muestra una frase sin revelar
- **WHEN** el usuario observa la pantalla
- **THEN** no se muestra ningún control de escuchar

#### Scenario: El control de escuchar muestra un icono de audio

- **GIVEN** la pantalla muestra una frase revelada
- **WHEN** el usuario observa el control de escuchar
- **THEN** el control muestra un icono de audio sin ningún texto visible junto a él, y expone una
  descripción accesible equivalente

### Requirement: Reproducción del audio en el locale de la frase

Al activar el control de escuchar, el sistema MUST reproducir el texto en portugués de la frase
revelada en su locale BCP-47 (ADR-0003). Activar el control varias veces MUST reproducir el audio
cada vez, reiniciando cualquier reproducción en curso de la misma frase.

#### Scenario: Escuchar reproduce la frase en su locale

- **GIVEN** una frase revelada con texto en portugués "Bom dia" y locale de destino `pt-PT`
- **WHEN** el usuario activa el control de escuchar
- **THEN** el sistema reproduce "Bom dia" en el locale `pt-PT`

#### Scenario: Escuchar dos veces reinicia la reproducción

- **GIVEN** el audio de la frase revelada reproduciéndose
- **WHEN** el usuario activa el control de escuchar otra vez
- **THEN** el sistema vuelve a reproducir el audio desde el principio

### Requirement: Control de escuchar deshabilitado fuera de la práctica activa

El control de escuchar MUST NOT estar disponible mientras se registra una respuesta en curso
(`busy`) o mientras el aviso de cierre de tanda está abierto, igual que el resto de acciones de
autoevaluación.

#### Scenario: Ocupado deshabilita el control de escuchar

- **GIVEN** una frase revelada con una respuesta registrándose
- **WHEN** el usuario intenta activar el control de escuchar
- **THEN** el control está deshabilitado y no se reproduce ningún audio

#### Scenario: Aviso de cierre abierto deshabilita el control de escuchar

- **GIVEN** el aviso de cierre de tanda abierto con una frase revelada
- **WHEN** el usuario intenta activar el control de escuchar
- **THEN** el control está deshabilitado y no se reproduce ningún audio

### Requirement: Fallo silencioso del motor de TTS

Si el motor de síntesis de voz no está disponible o el locale de la frase no tiene voz instalada en
el dispositivo, activar el control de escuchar MUST NOT mostrar ningún error ni cambiar el estado de
la pantalla; el audio simplemente no se reproduce.

#### Scenario: Motor de TTS no disponible

- **GIVEN** un motor de síntesis de voz que no está disponible en el dispositivo
- **WHEN** el usuario activa el control de escuchar
- **THEN** la pantalla no muestra ningún error y permanece en el mismo estado

#### Scenario: Locale de la frase sin voz instalada

- **GIVEN** una frase revelada cuyo locale de destino no tiene voz instalada en el dispositivo
- **WHEN** el usuario activa el control de escuchar
- **THEN** la pantalla no muestra ningún error y permanece en el mismo estado

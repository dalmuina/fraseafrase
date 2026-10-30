# Proposal

## Why

El método de práctica (ADR-0012) depende de reglas deterministas: racha de aciertos dentro de la tanda,
certificación con racha ≥ 3, resolución de cada frase al cerrar la tanda y niveles 0–5 con intervalos
de repaso. Hoy no existe ninguna de ellas en el dominio, así que ni la práctica por tandas ni la
composición de la tanda nueva pueden construirse sobre una base probada. Este cambio fija esa base en
`:core:domain` como lógica pura, testeable y sin Android.

## What Changes

- Añadir la entidad `PhraseProgress` (frase dentro de la tanda activa) con nivel, racha, marca de respondida y próximo repaso.
- Añadir la lógica de racha dentro de la tanda: un acierto suma 1 a la racha, un fallo la pone a 0.
- Añadir la regla de certificación: una frase está certificada cuando su racha es ≥ 3 al cerrar la tanda.
- Añadir la resolución de cada frase al cerrar la tanda: certificada, respondida no certificada y nunca respondida.
- Añadir los niveles 0–5 y sus intervalos de repaso en días naturales (1, 3, 7, 14 y 30 días), calculados
  a partir de una fecha recibida.
- Añadir el puerto `Clock` en `com.dalmuina.fraseafrase.core.domain.time` (interfaz). Su implementación de
  sistema se añadirá en `:core:platform` en un cambio posterior.
- No hay cambios de persistencia, UI ni navegación.

**Fuera de alcance** (cambios posteriores):

- Composición de la tanda nueva (prioridad de arrastradas, pendientes y nuevas, límite N, lista circular)
  y sus casos de catálogo agotado y primera tanda. Irán en la especificación `practice-batch`.
- Persistencia de niveles, rachas y tandas (Room / DataStore) y sus repositorios.
- Interfaz de práctica, aviso de cierre y ajustes de N.
- Audio (TTS) y corrección automática por voz; dirección inversa (ADR-0012).

## Capabilities

### New Capabilities

- `spaced-repetition`: reglas de racha, certificación, resolución al cerrar una tanda y niveles con
  intervalos de repaso (ADR-0012). Es una capacidad de comportamiento del sistema, independiente de la
  forma en que se guarde el progreso o se componga la tanda.

### Modified Capabilities

Ninguna. No existe todavía `openspec/specs/`, así que no hay requisitos vigentes que cambiar.

## Impact

- **Código:** paquetes `com.dalmuina.fraseafrase.core.domain.time` (puerto `Clock`) y
  `com.dalmuina.fraseafrase.core.domain.spacedrepetition` (entidades, función de repaso y casos de uso).
  Sin cambios en otros módulos.
- **Dependencias:** ninguna nueva. Solo Kotlin puro y los frameworks de test ya presentes en el version catalog.
- **ADR:** respeta ADR-0012 (reglas y terminología) y ADR-0010 (dependencias hacia dentro, dominio sin
  Android). No requiere ADR nuevo.
- **Supuestos registrados:** el nivel de una frase está siempre entre 0 y 5; las fechas son días naturales
  locales y el dominio no trabaja con horas.

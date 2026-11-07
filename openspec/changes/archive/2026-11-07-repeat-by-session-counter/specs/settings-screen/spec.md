# Spec Delta

## REMOVED Requirements

### Requirement: Regeneración sin cierre al cambiar el nivel CEFR

**Reason**: con la resolución de cada frase ocurriendo al instante de la autoevaluación (`spaced-repetition`),
ya no existe una "resolución de cierre" de la que distinguir el cambio de CEFR; cambiar CEFR y cambiar N
pasan a comportarse igual.
**Migration**: ver "Regeneración sin tocar el progreso al cambiar los ajustes" de este delta.

### Requirement: Cierre de la tanda activa al cambiar N

**Reason**: cambiar N ya no certifica ni resetea racha: no hay una resolución de cierre distinta de la que
ocurre, frase a frase, al autoevaluarla. Cambiar N pasa a recomponer sin tocar el progreso, igual que cambiar
CEFR.
**Migration**: ver "Regeneración sin tocar el progreso al cambiar los ajustes" de este delta.

## ADDED Requirements

### Requirement: Regeneración sin tocar el progreso al cambiar los ajustes

Si el nivel CEFR o el tamaño de tanda N guardados difieren de los que estaban persistidos antes de guardar, el
sistema MUST recomponer la tanda activa de inmediato, sin mostrar ningún aviso previo, sin incrementar el
contador de sesiones generadas y sin tocar la racha, el nivel de consolidación ni el `dueAtSession` de ninguna
frase: es una recomposición de qué frases están activas, no una resolución de cierre.

#### Scenario: Cambiar el nivel CEFR no toca el progreso

- **GIVEN** una tanda activa guardada, con una frase de racha 2, y el nivel CEFR A1 persistido
- **WHEN** el usuario elige el nivel B1 y confirma el guardado
- **THEN** la tanda activa se recompone con frases nuevas de nivel B1 y la frase de racha 2 conserva su racha,
  su nivel de consolidación y su `dueAtSession` sin cambios

#### Scenario: Cambiar N no toca el progreso

- **GIVEN** una tanda activa guardada y N igual a 20 persistido, con una frase de racha 2
- **WHEN** el usuario escribe 15 como tamaño de tanda y confirma el guardado
- **THEN** la tanda activa se recompone con tamaño 15 y la frase de racha 2 conserva su racha, su nivel de
  consolidación y su `dueAtSession` sin cambios

#### Scenario: Cambiar N y el nivel CEFR a la vez tampoco toca el progreso

- **GIVEN** una tanda activa guardada, el nivel CEFR A1 y N igual a 20, ambos persistidos
- **WHEN** el usuario elige el nivel B1, escribe 15 como tamaño de tanda y confirma el guardado
- **THEN** la tanda activa se recompone con nivel B1 y N igual a 15, sin certificar ni tocar el progreso de
  ninguna frase

#### Scenario: Ningún cambio de ajustes incrementa el contador de sesiones

- **GIVEN** el contador de sesiones generadas en 9
- **WHEN** el usuario cambia el nivel CEFR, N, o ambos, y confirma el guardado
- **THEN** el contador de sesiones generadas sigue en 9

#### Scenario: Sin tanda activa no hay nada que recomponer

- **GIVEN** no hay tanda activa guardada
- **WHEN** el usuario cambia el nivel CEFR o N y confirma el guardado
- **THEN** el guardado se completa sin error y sin mostrar ningún aviso

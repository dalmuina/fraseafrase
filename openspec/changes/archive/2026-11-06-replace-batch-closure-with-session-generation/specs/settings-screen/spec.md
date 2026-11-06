# Spec Delta

## REMOVED Requirements

### Requirement: Cierre de la tanda activa al cambiar el nivel CEFR o N

**Reason**: cambiar el nivel CEFR y cambiar N dejan de tener el mismo efecto. Cambiar el nivel CEFR ya no
cierra (ni certifica) la tanda activa: solo la recompone con el nuevo nivel. Cambiar N sigue cerrando la tanda
activa como hasta ahora.
**Migration**: ver los requisitos "Regeneración sin cierre al cambiar el nivel CEFR" y "Cierre de la tanda
activa al cambiar N" de este delta.

## ADDED Requirements

### Requirement: Regeneración sin cierre al cambiar el nivel CEFR

Si el nivel CEFR guardado difiere del que estaba persistido antes de guardar, y N no cambia, el sistema MUST
recomponer la tanda activa de inmediato con el nuevo nivel, sin certificarla ni cerrarla (`practice-batch`): no
MUST subir ni bajar ningún nivel de consolidación ni cambiar ningún próximo repaso por este guardado.

#### Scenario: Cambiar solo el nivel CEFR no certifica ni cierra

- **GIVEN** una tanda activa guardada, con una frase de racha 3, y el nivel CEFR A1 persistido
- **WHEN** el usuario elige el nivel B1, mantiene la N persistida y confirma el guardado
- **THEN** la tanda activa se recompone con frases nuevas de nivel B1 y la frase de racha 3 conserva su nivel
  de consolidación sin cambios

#### Scenario: Sin tanda activa no hay nada que recomponer

- **GIVEN** no hay tanda activa guardada
- **WHEN** el usuario cambia el nivel CEFR, mantiene la N persistida y confirma el guardado
- **THEN** el guardado se completa sin error y sin mostrar ningún aviso

### Requirement: Cierre de la tanda activa al cambiar N

Si el tamaño de tanda N guardado difiere del que estaba persistido antes de guardar, el sistema MUST cerrar la
tanda activa, si hay alguna, sin mostrar el aviso previo de no certificadas: es un cierre deliberado desde
ajustes, no un cierre iniciado desde la práctica.

#### Scenario: Cambiar N cierra la tanda activa sin aviso

- **GIVEN** una tanda activa guardada y N igual a 20 persistido
- **WHEN** el usuario escribe 15 como tamaño de tanda, mantiene el nivel CEFR persistido y confirma el guardado
- **THEN** la tanda activa se cierra sin mostrar el aviso de frases no certificadas

#### Scenario: Cambiar N y el nivel CEFR a la vez cierra la tanda activa

- **GIVEN** una tanda activa guardada, el nivel CEFR A1 y N igual a 20, ambos persistidos
- **WHEN** el usuario elige el nivel B1, escribe 15 como tamaño de tanda y confirma el guardado
- **THEN** la tanda activa se cierra (certifica y resetea racha) y la siguiente se recompone con nivel B1 y N
  igual a 15

#### Scenario: Sin tanda activa no hay nada que cerrar

- **GIVEN** no hay tanda activa guardada
- **WHEN** el usuario cambia N y confirma el guardado
- **THEN** el guardado se completa sin error y sin mostrar ningún aviso de cierre

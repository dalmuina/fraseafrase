# Spec Delta

## MODIFIED Requirements

### Requirement: Recomposición de la tanda activa al detectar ajustes aplicados distintos

Al obtener la tanda activa de un idioma destino que ya tiene una, el sistema MUST compararla contra
sus ajustes aplicados registrados (`user-settings`) y recomponerla de inmediato si difieren de los
ajustes guardados vigentes, sin tocar progreso ni el contador de sesiones. Esta comparación MUST
repetirse en cada obtención, sobreviva o no la app de por medio. Si no hay ningún ajuste aplicado
registrado todavía, el sistema MUST registrar los ajustes guardados vigentes como sus ajustes
aplicados en esa misma obtención, sin recomponer la tanda existente.

#### Scenario: Ajustes aplicados distintos recomponen sin tocar el progreso

- **GIVEN** una tanda activa de un idioma destino compuesta con el nivel CEFR A1 y N igual a 20,
  registrados como sus ajustes aplicados, y una frase de esa tanda con racha 2
- **WHEN** los ajustes guardados de ese idioma destino pasan a tener el nivel CEFR B1 y se obtiene su
  tanda activa
- **THEN** la tanda activa se recompone con frases del nivel CEFR B1, sin incrementar el contador de
  sesiones generadas y conservando la racha, el nivel de consolidación y el `dueAtSession` de la frase
  de racha 2 sin cambios

#### Scenario: Ajustes aplicados iguales no recomponen

- **GIVEN** una tanda activa de un idioma destino compuesta con el nivel CEFR B1 y N igual a 20,
  registrados como sus ajustes aplicados, que siguen siendo los ajustes guardados vigentes de ese
  idioma destino
- **WHEN** se obtiene la tanda activa de ese idioma destino
- **THEN** se devuelve la misma tanda activa, sin recomponerla

#### Scenario: Sin ajustes aplicados registrados todavía no se recompone

- **GIVEN** una tanda activa de un idioma destino sin ningún ajuste aplicado registrado para ese
  idioma destino, con el nivel CEFR B1 y N igual a 15 guardados
- **WHEN** se obtiene la tanda activa de ese idioma destino
- **THEN** se devuelve la tanda activa existente tal cual, sin recomponerla, y los ajustes aplicados
  registrados de ese idioma destino quedan en el nivel CEFR B1 y N igual a 15

#### Scenario: Tras registrarse, un cambio posterior de ajustes ya recompone

- **GIVEN** una tanda activa de un idioma destino sin ajustes aplicados registrados, obtenida una vez
  con el nivel CEFR A1 y N igual a 20 guardados (lo que registra ese nivel y esa N como sus ajustes
  aplicados)
- **WHEN** los ajustes guardados de ese idioma destino cambian al nivel CEFR B1 y se obtiene su tanda
  activa de nuevo
- **THEN** la tanda activa se recompone con frases del nivel CEFR B1

#### Scenario: El cambio sobrevive al reinicio de la app

- **GIVEN** una tanda activa de un idioma destino compuesta con el nivel CEFR A1, registrado como su
  ajuste aplicado, y un cambio posterior de los ajustes guardados de ese idioma destino al nivel CEFR
  B1 hecho sin volver a obtener su tanda activa
- **WHEN** la app se reinicia y se obtiene la tanda activa de ese idioma destino
- **THEN** la tanda activa se recompone con frases del nivel CEFR B1

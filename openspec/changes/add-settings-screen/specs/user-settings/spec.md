# Spec Delta

## ADDED Requirements

### Requirement: Guardado de un nuevo nivel CEFR y N

El sistema MUST ofrecer una forma de guardar un nivel CEFR y una N nuevos que sustituya a los
valores anteriores. Un guardado correcto MUST NOT lanzar error.

#### Scenario: Guardar sustituye los ajustes anteriores

- **GIVEN** el nivel CEFR A1 y N igual a 20 guardados
- **WHEN** se guardan el nivel CEFR B2 y N igual a 15
- **THEN** una lectura posterior de los ajustes devuelve el nivel CEFR B2 y N igual a 15

### Requirement: Guardado fallido no deja ajustes a medias

Si el almacén de ajustes no puede escribirse, el sistema MUST propagar el fallo sin dejar el nivel
CEFR y la N guardados en una combinación que no se haya pedido guardar.

#### Scenario: Fallo de escritura no mezcla ajustes antiguos y nuevos

- **GIVEN** el nivel CEFR A1 y N igual a 20 guardados, y un almacén de ajustes que produce un error
  de E/S al escribirse
- **WHEN** se intenta guardar el nivel CEFR B2 y N igual a 15
- **THEN** una lectura posterior de los ajustes devuelve el nivel CEFR A1 y N igual a 20, o el nivel
  CEFR B2 y N igual a 15, pero no una combinación de ambos

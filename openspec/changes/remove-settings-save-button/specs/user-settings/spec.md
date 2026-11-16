# Spec Delta

## ADDED Requirements

### Requirement: Ajustes aplicados a la tanda activa

El sistema MUST ofrecer una forma de registrar y leer, por idioma destino, el nivel CEFR y la N con
los que se compuso la tanda activa vigente de ese idioma destino ("ajustes aplicados"),
independientemente de los ajustes editables de ese idioma destino.

#### Scenario: Sin ajustes aplicados registrados

- **GIVEN** que no se ha registrado ningún ajuste aplicado para el idioma destino `pt-PT`
- **WHEN** se leen los ajustes aplicados de `pt-PT`
- **THEN** el resultado indica que no hay ninguno registrado

#### Scenario: Registrar y leer los ajustes aplicados

- **GIVEN** el nivel CEFR B1 y N igual a 25 recién compuestos para el idioma destino `pt-PT`
- **WHEN** se registran como ajustes aplicados de `pt-PT` y se leen a continuación
- **THEN** la lectura devuelve el nivel CEFR B1 y N igual a 25

#### Scenario: Los ajustes aplicados sobreviven al reinicio

- **GIVEN** el nivel CEFR B1 y N igual a 25 registrados como ajustes aplicados de `pt-PT`
- **WHEN** la app se reinicia y se leen los ajustes aplicados de `pt-PT`
- **THEN** la lectura devuelve el nivel CEFR B1 y N igual a 25

#### Scenario: Los ajustes aplicados de un idioma destino no se mezclan con los de otro

- **GIVEN** el nivel CEFR B1 y N igual a 25 registrados como ajustes aplicados de `pt-PT`
- **WHEN** se registran el nivel CEFR A2 y N igual a 10 como ajustes aplicados de `pt-BR` y se leen
  los de ambos idiomas destino
- **THEN** `pt-PT` sigue devolviendo el nivel CEFR B1 y N igual a 25, y `pt-BR` devuelve el nivel
  CEFR A2 y N igual a 10

#### Scenario: Los ajustes aplicados son independientes de los ajustes editables

- **GIVEN** el nivel CEFR A1 y N igual a 20 registrados como ajustes aplicados de `pt-PT`
- **WHEN** se guarda el nivel CEFR B2 y N igual a 15 como ajustes editables de `pt-PT`, sin registrar
  ningún ajuste aplicado nuevo
- **THEN** los ajustes aplicados de `pt-PT` siguen siendo el nivel CEFR A1 y N igual a 20

# user-settings Specification

## Purpose

Define la persistencia de los ajustes del usuario que afectan a la práctica: el nivel CEFR de las frases nuevas y el tamaño de tanda N, con sus valores por defecto y su recuperación ante valores corruptos, según ADR-0012 y ADR-0013. No define la pantalla de ajustes.

## Requirements

### Requirement: Valores por defecto

Si no hay ningún ajuste guardado, el nivel CEFR MUST ser A1 y el tamaño de tanda N MUST ser 20.

#### Scenario: Primera ejecución

- **GIVEN** que no hay ajustes guardados
- **WHEN** se leen los ajustes
- **THEN** el nivel CEFR es A1 y N es 20

### Requirement: Persistencia de los ajustes

Los ajustes guardados MUST recuperarse tras reiniciar la app con el mismo valor.

#### Scenario: Ajustes sobreviven al reinicio

- **GIVEN** el nivel CEFR B1 y N igual a 25 guardados
- **WHEN** la app se reinicia y se leen los ajustes
- **THEN** el nivel CEFR es B1 y N es 25

### Requirement: Fallback de N ante valores fuera de rango

Si el valor guardado de N no es un entero entre 10 y 30 inclusive, o no puede leerse, el sistema MUST usar 20 y MUST NOT lanzar error.

#### Scenario: N fuera de rango usa el valor por defecto

- **GIVEN** un valor guardado de N igual a 31
- **WHEN** se leen los ajustes
- **THEN** N es 20

#### Scenario: N corrupto usa el valor por defecto

- **GIVEN** un valor guardado de N que no es un entero
- **WHEN** se leen los ajustes
- **THEN** N es 20

#### Scenario: N guardado como entero se lee correctamente

- **GIVEN** un valor de N guardado como entero 25
- **WHEN** se leen los ajustes
- **THEN** N es 25

#### Scenario: N guardado con otro tipo usa el valor por defecto

- **GIVEN** un valor guardado de N de tipo booleano `true`
- **WHEN** se leen los ajustes
- **THEN** N es 20 y no se lanza ningún error de tipo

#### Scenario: N guardado como texto con forma de entero usa el valor por defecto

- **GIVEN** un valor guardado de N igual al texto `"25"`
- **WHEN** se leen los ajustes
- **THEN** N es 20, porque el valor guardado no es un entero

#### Scenario: N guardado como texto no entero usa el valor por defecto

- **GIVEN** un valor guardado de N igual al texto `veinte`
- **WHEN** se leen los ajustes
- **THEN** N es 20 y no se lanza ningún error de tipo

### Requirement: Fallback del nivel CEFR ante valores no válidos

Si el valor guardado del nivel CEFR no es A1, A2, B1, B2 o C1, el sistema MUST usar A1 y MUST NOT lanzar error.

#### Scenario: Nivel CEFR no válido usa A1

- **GIVEN** un valor guardado de nivel CEFR igual a `D1`
- **WHEN** se leen los ajustes
- **THEN** el nivel CEFR es A1

### Requirement: Lectura de un almacén ilegible

Si el almacén de ajustes no puede leerse, el sistema MUST devolver los valores por defecto (A1 y 20) y MUST NOT lanzar error.

#### Scenario: Almacén ilegible devuelve los valores por defecto

- **GIVEN** un almacén de ajustes que produce un error de E/S al leerse
- **WHEN** se leen los ajustes
- **THEN** el nivel CEFR es A1 y N es 20

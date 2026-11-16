# user-settings Specification

## Purpose

Define la persistencia de los ajustes del usuario que afectan a la práctica: el nivel CEFR de las frases nuevas y el tamaño de tanda N, con sus valores por defecto y su recuperación ante valores corruptos, según ADR-0012 y ADR-0013. No define la pantalla de ajustes.

## Requirements

### Requirement: Valores por defecto

Si no hay ningún ajuste guardado para un idioma destino, el nivel CEFR de ese idioma destino MUST
ser A1 y su tamaño de tanda N MUST ser 20.

#### Scenario: Primera ejecución

- **GIVEN** que no hay ajustes guardados para el idioma destino `pt-PT`
- **WHEN** se leen los ajustes de `pt-PT`
- **THEN** el nivel CEFR es A1 y N es 20

#### Scenario: Un idioma destino nuevo no hereda los ajustes de otro

- **GIVEN** el idioma destino `pt-PT` con nivel CEFR B1 y N igual a 25 guardados, y el idioma
  destino `pt-BR` sin ajustes guardados todavía
- **WHEN** se leen los ajustes de `pt-BR`
- **THEN** el nivel CEFR de `pt-BR` es A1 y su N es 20

### Requirement: Persistencia de los ajustes

Los ajustes guardados de un idioma destino MUST recuperarse tras reiniciar la app con el mismo
valor, de forma independiente de los ajustes de cualquier otro idioma destino.

#### Scenario: Ajustes sobreviven al reinicio

- **GIVEN** el nivel CEFR B1 y N igual a 25 guardados para el idioma destino `pt-PT`
- **WHEN** la app se reinicia y se leen los ajustes de `pt-PT`
- **THEN** el nivel CEFR es B1 y N es 25

#### Scenario: Los ajustes de dos idiomas destino no se mezclan

- **GIVEN** el nivel CEFR B1 y N igual a 25 guardados para `pt-PT`, y el nivel CEFR A2 y N igual a
  10 guardados para `pt-BR`
- **WHEN** se leen los ajustes de cada idioma destino
- **THEN** `pt-PT` devuelve el nivel CEFR B1 y N igual a 25, y `pt-BR` devuelve el nivel CEFR A2 y N
  igual a 10

### Requirement: Fallback de N ante valores fuera de rango

Si el valor guardado de N para un idioma destino no es un entero entre 10 y 30 inclusive, o no puede
leerse, el sistema MUST usar 20 para ese idioma destino y MUST NOT lanzar error.

#### Scenario: N fuera de rango usa el valor por defecto

- **GIVEN** un valor guardado de N igual a 31 para el idioma destino `pt-PT`
- **WHEN** se leen los ajustes de `pt-PT`
- **THEN** N es 20

#### Scenario: N corrupto usa el valor por defecto

- **GIVEN** un valor guardado de N que no es un entero para el idioma destino `pt-PT`
- **WHEN** se leen los ajustes de `pt-PT`
- **THEN** N es 20

#### Scenario: N guardado como entero se lee correctamente

- **GIVEN** un valor de N guardado como entero 25 para el idioma destino `pt-PT`
- **WHEN** se leen los ajustes de `pt-PT`
- **THEN** N es 25

#### Scenario: N guardado con otro tipo usa el valor por defecto

- **GIVEN** un valor guardado de N de tipo booleano `true` para el idioma destino `pt-PT`
- **WHEN** se leen los ajustes de `pt-PT`
- **THEN** N es 20 y no se lanza ningún error de tipo

#### Scenario: N guardado como texto con forma de entero usa el valor por defecto

- **GIVEN** un valor guardado de N igual al texto `"25"` para el idioma destino `pt-PT`
- **WHEN** se leen los ajustes de `pt-PT`
- **THEN** N es 20, porque el valor guardado no es un entero

#### Scenario: N guardado como texto no entero usa el valor por defecto

- **GIVEN** un valor guardado de N igual al texto `veinte` para el idioma destino `pt-PT`
- **WHEN** se leen los ajustes de `pt-PT`
- **THEN** N es 20 y no se lanza ningún error de tipo

### Requirement: Fallback del nivel CEFR ante valores no válidos

Si el valor guardado del nivel CEFR de un idioma destino no es A1, A2, B1, B2 o C1, el sistema MUST
usar A1 para ese idioma destino y MUST NOT lanzar error.

#### Scenario: Nivel CEFR no válido usa A1

- **GIVEN** un valor guardado de nivel CEFR igual a `D1` para el idioma destino `pt-PT`
- **WHEN** se leen los ajustes de `pt-PT`
- **THEN** el nivel CEFR es A1

### Requirement: Lectura de un almacén ilegible

Si el almacén de ajustes no puede leerse, el sistema MUST devolver los valores por defecto (A1 y 20)
para el idioma destino consultado y MUST NOT lanzar error.

#### Scenario: Almacén ilegible devuelve los valores por defecto

- **GIVEN** un almacén de ajustes que produce un error de E/S al leerse
- **WHEN** se leen los ajustes del idioma destino `pt-PT`
- **THEN** el nivel CEFR es A1 y N es 20

### Requirement: Guardado de un nuevo nivel CEFR y N

El sistema MUST ofrecer una forma de guardar, para un idioma destino, un nivel CEFR y una N nuevos
que sustituyan a los valores anteriores de ese idioma destino, sin modificar los ajustes de ningún
otro idioma destino. Un guardado correcto MUST NOT lanzar error.

#### Scenario: Guardar sustituye los ajustes anteriores

- **GIVEN** el nivel CEFR A1 y N igual a 20 guardados para el idioma destino `pt-PT`
- **WHEN** se guardan el nivel CEFR B2 y N igual a 15 para `pt-PT`
- **THEN** una lectura posterior de los ajustes de `pt-PT` devuelve el nivel CEFR B2 y N igual a 15

#### Scenario: Guardar los ajustes de un idioma destino no modifica los de otro

- **GIVEN** el nivel CEFR A1 y N igual a 20 guardados para `pt-BR`
- **WHEN** se guardan el nivel CEFR B2 y N igual a 15 para `pt-PT`
- **THEN** una lectura posterior de los ajustes de `pt-BR` sigue devolviendo el nivel CEFR A1 y N
  igual a 20

### Requirement: Guardado fallido no deja ajustes a medias

Si el almacén de ajustes no puede escribirse, el sistema MUST propagar el fallo sin dejar el nivel
CEFR y la N guardados de ese idioma destino en una combinación que no se haya pedido guardar.

#### Scenario: Fallo de escritura no mezcla ajustes antiguos y nuevos

- **GIVEN** el nivel CEFR A1 y N igual a 20 guardados para el idioma destino `pt-PT`, y un almacén de
  ajustes que produce un error de E/S al escribirse
- **WHEN** se intenta guardar el nivel CEFR B2 y N igual a 15 para `pt-PT`
- **THEN** una lectura posterior de los ajustes de `pt-PT` devuelve el nivel CEFR A1 y N igual a 20,
  o el nivel CEFR B2 y N igual a 15, pero no una combinación de ambos

### Requirement: Idioma destino activo

El sistema MUST persistir cuál es el idioma destino activo entre los idiomas destino disponibles.
Si no hay ninguno guardado, o el guardado ya no está disponible, el sistema MUST usar como activo el
primer idioma destino disponible y MUST NOT lanzar error.

#### Scenario: Primera ejecución usa el primer idioma destino disponible

- **GIVEN** que no hay ningún idioma destino activo guardado, con `pt-PT` y `pt-BR` disponibles en
  ese orden
- **WHEN** se lee el idioma destino activo
- **THEN** el idioma destino activo es `pt-PT`

#### Scenario: El idioma destino activo guardado se recupera

- **GIVEN** `pt-BR` guardado como idioma destino activo
- **WHEN** la app se reinicia y se lee el idioma destino activo
- **THEN** el idioma destino activo es `pt-BR`

#### Scenario: Un idioma destino activo guardado que ya no está disponible usa el primero disponible

- **GIVEN** `pt-BR` guardado como idioma destino activo, y un catálogo donde `pt-BR` ya no está
  disponible pero `pt-PT` sí
- **WHEN** se lee el idioma destino activo
- **THEN** el idioma destino activo es `pt-PT`

### Requirement: Cambio del idioma destino activo

El sistema MUST ofrecer una forma de cambiar el idioma destino activo a cualquiera de los idiomas
destino disponibles. Cambiarlo MUST NOT modificar los ajustes, el progreso ni la tanda activa de
ningún idioma destino.

#### Scenario: Cambiar el idioma destino activo

- **GIVEN** `pt-PT` como idioma destino activo
- **WHEN** se cambia el idioma destino activo a `pt-BR`
- **THEN** una lectura posterior del idioma destino activo devuelve `pt-BR`

#### Scenario: Cambiar de idioma destino no toca el progreso de ninguno

- **GIVEN** `pt-PT` activo con progreso guardado, y `pt-BR` con su propio progreso guardado
- **WHEN** se cambia el idioma destino activo de `pt-PT` a `pt-BR`
- **THEN** el progreso guardado de `pt-PT` y de `pt-BR` quedan exactamente igual que antes del cambio

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

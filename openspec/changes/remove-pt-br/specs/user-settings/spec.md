## MODIFIED Requirements

### Requirement: Valores por defecto

Si no hay ningún ajuste guardado para un idioma destino, el nivel CEFR de ese idioma destino MUST
ser A1 y su tamaño de tanda N MUST ser 20.

#### Scenario: Primera ejecución

- **GIVEN** que no hay ajustes guardados para el idioma destino `pt-PT`
- **WHEN** se leen los ajustes de `pt-PT`
- **THEN** el nivel CEFR es A1 y N es 20

#### Scenario: Un idioma destino nuevo no hereda los ajustes de otro

- **GIVEN** el idioma destino `pt-PT` con nivel CEFR B1 y N igual a 25 guardados, y el idioma
  destino `en-GB` sin ajustes guardados todavía
- **WHEN** se leen los ajustes de `en-GB`
- **THEN** el nivel CEFR de `en-GB` es A1 y su N es 20

### Requirement: Persistencia de los ajustes

Los ajustes guardados de un idioma destino MUST recuperarse tras reiniciar la app con el mismo
valor, de forma independiente de los ajustes de cualquier otro idioma destino.

#### Scenario: Ajustes sobreviven al reinicio

- **GIVEN** el nivel CEFR B1 y N igual a 25 guardados para el idioma destino `pt-PT`
- **WHEN** la app se reinicia y se leen los ajustes de `pt-PT`
- **THEN** el nivel CEFR es B1 y N es 25

#### Scenario: Los ajustes de dos idiomas destino no se mezclan

- **GIVEN** el nivel CEFR B1 y N igual a 25 guardados para `pt-PT`, y el nivel CEFR A2 y N igual a
  10 guardados para `en-GB`
- **WHEN** se leen los ajustes de cada idioma destino
- **THEN** `pt-PT` devuelve el nivel CEFR B1 y N igual a 25, y `en-GB` devuelve el nivel CEFR A2 y N
  igual a 10

### Requirement: Guardado de un nuevo nivel CEFR y N

El sistema MUST ofrecer una forma de guardar, para un idioma destino, un nivel CEFR y una N nuevos
que sustituyan a los valores anteriores de ese idioma destino, sin modificar los ajustes de ningún
otro idioma destino. Un guardado correcto MUST NOT lanzar error.

#### Scenario: Guardar sustituye los ajustes anteriores

- **GIVEN** el nivel CEFR A1 y N igual a 20 guardados para el idioma destino `pt-PT`
- **WHEN** se guardan el nivel CEFR B2 y N igual a 15 para `pt-PT`
- **THEN** una lectura posterior de los ajustes de `pt-PT` devuelve el nivel CEFR B2 y N igual a 15

#### Scenario: Guardar los ajustes de un idioma destino no modifica los de otro

- **GIVEN** el nivel CEFR A1 y N igual a 20 guardados para `en-GB`
- **WHEN** se guardan el nivel CEFR B2 y N igual a 15 para `pt-PT`
- **THEN** una lectura posterior de los ajustes de `en-GB` sigue devolviendo el nivel CEFR A1 y N
  igual a 20

### Requirement: Idioma destino activo

El sistema MUST persistir cuál es el idioma destino activo entre los idiomas destino disponibles. El idioma
destino activo MUST ser distinto del idioma de origen. Si no hay ninguno guardado, el guardado ya no está
disponible o coincide con el idioma de origen, el sistema MUST usar como activo el primer idioma disponible
distinto del idioma de origen y MUST NOT lanzar error.

#### Scenario: Primera ejecución usa el primer idioma destino disponible

- **GIVEN** que no hay ningún idioma destino activo guardado, con `pt-PT`, `en-GB` y `es-ES` disponibles
  en ese orden y `es-ES` como idioma de origen
- **WHEN** se lee el idioma destino activo
- **THEN** el idioma destino activo es `pt-PT`

#### Scenario: Primera ejecución salta el idioma de origen

- **GIVEN** que no hay ningún idioma destino activo guardado, con `es-ES` y `pt-PT` disponibles en ese orden y
  `es-ES` como idioma de origen
- **WHEN** se lee el idioma destino activo
- **THEN** el idioma destino activo es `pt-PT`

#### Scenario: El idioma destino activo guardado se recupera

- **GIVEN** `en-GB` guardado como idioma destino activo y `es-ES` como idioma de origen
- **WHEN** la app se reinicia y se lee el idioma destino activo
- **THEN** el idioma destino activo es `en-GB`

#### Scenario: Un idioma destino activo guardado que ya no está disponible usa el primero disponible

- **GIVEN** `pt-BR` guardado como idioma destino activo (idioma retirado del catálogo), `es-ES` como idioma de
  origen, y un catálogo donde `pt-BR` no está disponible pero `pt-PT` y `es-ES` sí, en ese orden
- **WHEN** se lee el idioma destino activo
- **THEN** el idioma destino activo es `pt-PT`

#### Scenario: Un idioma destino guardado igual al origen usa el primero distinto del origen

- **GIVEN** `es-ES` guardado como idioma destino activo y `es-ES` como idioma de origen, con `es-ES`, `de-DE` y
  `pt-PT` disponibles en ese orden
- **WHEN** se lee el idioma destino activo
- **THEN** el idioma destino activo es `de-DE`

### Requirement: Cambio del idioma destino activo

El sistema MUST ofrecer una forma de cambiar el idioma destino activo a cualquiera de los idiomas
destino disponibles distinto del idioma de origen. Cambiarlo MUST NOT modificar los ajustes, el progreso ni la
tanda activa de ningún idioma destino.

#### Scenario: Cambiar el idioma destino activo

- **GIVEN** `pt-PT` como idioma destino activo
- **WHEN** se cambia el idioma destino activo a `en-GB`
- **THEN** una lectura posterior del idioma destino activo devuelve `en-GB`

#### Scenario: Cambiar de idioma destino no toca el progreso de ninguno

- **GIVEN** `pt-PT` activo con progreso guardado, y `en-GB` con su propio progreso guardado
- **WHEN** se cambia el idioma destino activo de `pt-PT` a `en-GB`
- **THEN** el progreso guardado de `pt-PT` y de `en-GB` quedan exactamente igual que antes del cambio

#### Scenario: No se puede elegir como destino el idioma de origen

- **GIVEN** `es-ES` como idioma de origen
- **WHEN** se intenta cambiar el idioma destino activo a `es-ES`
- **THEN** el cambio se rechaza sin error (resultado de rechazo, no una excepción) y el idioma destino activo
  guardado no cambia

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
- **WHEN** se registran el nivel CEFR A2 y N igual a 10 como ajustes aplicados de `en-GB` y se leen
  los de ambos idiomas destino
- **THEN** `pt-PT` sigue devolviendo el nivel CEFR B1 y N igual a 25, y `en-GB` devuelve el nivel
  CEFR A2 y N igual a 10

#### Scenario: Los ajustes aplicados son independientes de los ajustes editables

- **GIVEN** el nivel CEFR A1 y N igual a 20 registrados como ajustes aplicados de `pt-PT`
- **WHEN** se guarda el nivel CEFR B2 y N igual a 15 como ajustes editables de `pt-PT`, sin registrar
  ningún ajuste aplicado nuevo
- **THEN** los ajustes aplicados de `pt-PT` siguen siendo el nivel CEFR A1 y N igual a 20

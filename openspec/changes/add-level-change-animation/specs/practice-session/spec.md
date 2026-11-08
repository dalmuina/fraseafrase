# Spec Delta

## ADDED Requirements

### Requirement: Indicador animado de cambio de insignia

Al autoevaluar una frase, si su nivel de consolidación resultante es distinto del nivel que tenía
inmediatamente antes de esa autoevaluación, la pantalla MUST mostrar brevemente un indicador animado con
la insignia (`level-badges`) y la etiqueta del nivel resultante, y MUST ocultarlo automáticamente sin que
el usuario tenga que descartarlo. Si el nivel resultante es igual al nivel anterior, la pantalla MUST NOT
mostrar ese indicador.

#### Scenario: Un acierto que certifica muestra el indicador

- **GIVEN** una frase de nivel 2 (insignia "Plata") con racha 2
- **WHEN** el usuario la revela y se autoevalúa con acierto
- **THEN** la pantalla muestra el indicador de cambio de insignia con la insignia "Oro"

#### Scenario: Un fallo que baja el nivel muestra el indicador

- **GIVEN** una frase de nivel 3 (insignia "Oro")
- **WHEN** el usuario la revela y se autoevalúa con fallo
- **THEN** la pantalla muestra el indicador de cambio de insignia con la insignia "Plata" (ADR-0015)

#### Scenario: Un acierto que no certifica no muestra el indicador

- **GIVEN** una frase de nivel 1 (insignia "Bronce") con racha 0
- **WHEN** el usuario la revela y se autoevalúa con acierto
- **THEN** la pantalla no muestra ningún indicador de cambio de insignia

#### Scenario: Certificar de nuevo en nivel máximo no muestra el indicador

- **GIVEN** una frase de nivel 5 (insignia "Diamante") con racha 2
- **WHEN** el usuario la revela y se autoevalúa con acierto
- **THEN** la pantalla no muestra ningún indicador de cambio de insignia, porque el nivel se mantiene en 5
  (ADR-0015)

#### Scenario: Un fallo en nivel 0 no muestra el indicador

- **GIVEN** una frase nueva de nivel 0 (insignia "sin rango")
- **WHEN** el usuario la revela y se autoevalúa con fallo
- **THEN** la pantalla no muestra ningún indicador de cambio de insignia, porque el nivel 0 no tiene nivel
  inferior al que bajar (ADR-0015)

#### Scenario: El indicador desaparece sin intervención del usuario

- **GIVEN** el indicador de cambio de insignia visible tras una subida de nivel
- **WHEN** transcurre su duración sin que el usuario lo toque
- **THEN** el indicador deja de mostrarse por sí solo

### Requirement: Estilo distinto según la dirección del cambio de insignia

El indicador de cambio de insignia MUST distinguirse visualmente según la dirección del cambio: una subida
de nivel MUST mostrarse con énfasis (halo alrededor de la insignia) y MUST permanecer visible más tiempo
que una bajada; una bajada de nivel MUST mostrarse sin ese halo y MUST permanecer visible menos tiempo que
una subida.

#### Scenario: La subida se muestra con halo

- **GIVEN** un acierto que certifica una frase
- **WHEN** la pantalla muestra el indicador de cambio de insignia
- **THEN** el indicador muestra un halo alrededor de la insignia "Oro"

#### Scenario: La bajada se muestra sin halo

- **GIVEN** un fallo que baja el nivel de una frase
- **WHEN** la pantalla muestra el indicador de cambio de insignia
- **THEN** el indicador no muestra ningún halo alrededor de la insignia resultante

#### Scenario: La bajada permanece visible menos tiempo que la subida

- **GIVEN** una subida de nivel y, por separado, una bajada de nivel
- **WHEN** se muestra el indicador de cambio de insignia en cada caso
- **THEN** la duración del indicador de la bajada es menor que la duración del indicador de la subida

### Requirement: El indicador no bloquea la interacción con la frase siguiente

El indicador de cambio de insignia MUST NOT impedir ver ni pulsar los controles de la frase que la pantalla
muestra a continuación (`practice-session`, "Controles de acción siempre visibles y pulsables"): mientras
el indicador está visible, esos controles MUST seguir existiendo en pantalla, aunque la respuesta se trate
como en curso (`busy`) hasta que el indicador termine.

#### Scenario: El control de revelar sigue presente mientras se anima el cambio de insignia

- **GIVEN** una autoevaluación que cambia el nivel de consolidación de la frase respondida
- **WHEN** la pantalla muestra el indicador de cambio de insignia sobre la frase siguiente
- **THEN** el control de revelar la traducción de la frase siguiente está presente en pantalla

## MODIFIED Requirements

### Requirement: Aciertos para el próximo nivel visibles

La pantalla MUST mostrar, para la frase revelada o sin revelar, cuántos aciertos consecutivos le faltan para
alcanzar la insignia del siguiente nivel de consolidación: `max(0, 3 - racha)` aciertos para la insignia de
`min(nivel + 1, 5)`. El contador MUST mostrarse de forma literal, sin texto alternativo cuando ya llegó a 3
aciertos en la tanda activa ni cuando el nivel de consolidación ya es el máximo (5).

#### Scenario: Contador visible tras un acierto

- **GIVEN** una frase de nivel 0 con racha 1 en la tanda activa
- **WHEN** el usuario la revela
- **THEN** la pantalla muestra que le faltan 2 aciertos para la insignia "Bronce"

#### Scenario: Fallo reinicia el contador

- **GIVEN** una tanda con la frase A de nivel 2 y racha 1
- **WHEN** el usuario revela A y se autoevalúa con fallo
- **THEN** al mostrarse A de nuevo y revelarla, la pantalla muestra que le faltan 3 aciertos para la insignia
  "Oro", porque el fallo dejó su racha en 0 sin bajar su nivel (ADR-0024)

#### Scenario: Fallo con racha mayor que 0 suma un acierto al contador

- **GIVEN** una tanda con la frase A de nivel 2 y racha 2
- **WHEN** el usuario revela A y se autoevalúa con fallo
- **THEN** al mostrarse A de nuevo y revelarla, la pantalla muestra que le faltan 2 aciertos para la insignia
  "Oro", porque el fallo solo restó un acierto a su racha (ADR-0024)

#### Scenario: Fallo con racha 0 deja la frase a un acierto del nivel que tenía

- **GIVEN** una tanda con la frase A de nivel 2 y racha 0
- **WHEN** el usuario revela A y se autoevalúa con fallo
- **THEN** al mostrarse A de nuevo y revelarla, la pantalla muestra que le falta 1 acierto para la insignia
  "Plata", porque el fallo bajó su nivel a 1 con racha 2 (ADR-0024)

#### Scenario: Racha de tres sigue mostrando el contador en 0

- **GIVEN** una frase de nivel 1 con racha 3 en la tanda activa
- **WHEN** la pantalla la muestra
- **THEN** la pantalla muestra que le faltan 0 aciertos para la insignia "Plata"

#### Scenario: Nivel máximo sigue mostrando el contador

- **GIVEN** una frase de nivel 5 con racha 3 en la tanda activa
- **WHEN** la pantalla la muestra
- **THEN** la pantalla muestra que le faltan 0 aciertos para la insignia "Diamante"

#### Scenario: El contador cambia con la frase mostrada

- **GIVEN** una tanda con la frase A de nivel 0 y racha 0, y la frase B de nivel 2 y racha 1, mostrando A
- **WHEN** el usuario autoevalúa A y la pantalla pasa a mostrar B
- **THEN** la pantalla muestra que a B le faltan 2 aciertos para la insignia "Oro"

### Requirement: Insignia de nivel de consolidación visible

La pantalla de práctica MUST mostrar la insignia de nivel de consolidación (`level-badges`) de la
frase actual, revelada o sin revelar, junto a su nivel CEFR. La insignia mostrada MUST corresponder
al nivel de consolidación actual de esa frase, incluida la insignia "sin rango" en nivel 0.

#### Scenario: Insignia visible sin revelar

- **GIVEN** una tanda activa cuya primera frase tiene nivel de consolidación 2
- **WHEN** el usuario abre la pantalla de práctica
- **THEN** la pantalla muestra la insignia "Plata" junto a la frase sin revelar

#### Scenario: Insignia visible al revelar

- **GIVEN** una frase de nivel de consolidación 4 sin revelar
- **WHEN** el usuario la revela
- **THEN** la pantalla sigue mostrando la insignia "Platino"

#### Scenario: Insignia sin rango en nivel 0

- **GIVEN** una frase nueva con nivel de consolidación 0
- **WHEN** la pantalla la muestra
- **THEN** la pantalla muestra la insignia "sin rango" para esa frase

#### Scenario: La insignia cambia con la frase mostrada

- **GIVEN** una tanda con la frase A de nivel de consolidación 1 y la frase B de nivel de
  consolidación 5, mostrando A
- **WHEN** el usuario autoevalúa A y la pantalla pasa a mostrar B
- **THEN** la pantalla muestra la insignia "Diamante"

#### Scenario: Un fallo que baja el nivel actualiza la insignia

- **GIVEN** una frase con nivel de consolidación 3 (insignia "Oro") y racha 0
- **WHEN** el usuario la revela y se autoevalúa con fallo
- **THEN** al volver a mostrarse esa frase, la pantalla muestra la insignia "Plata"

#### Scenario: Un fallo con racha mayor que 0 mantiene la insignia

- **GIVEN** una frase con nivel de consolidación 3 (insignia "Oro") y racha 1
- **WHEN** el usuario la revela y se autoevalúa con fallo
- **THEN** al volver a mostrarse esa frase, la pantalla sigue mostrando la insignia "Oro"

### Requirement: Indicador animado de cambio de insignia

Al autoevaluar una frase, si su nivel de consolidación resultante es distinto del nivel que tenía
inmediatamente antes de esa autoevaluación, la insignia de nivel (`level-badges`) que la pantalla
muestra junto al nivel CEFR de esa misma frase MUST transicionar visiblemente de la insignia
anterior a la insignia resultante, sin que la pantalla avance todavía a la frase siguiente de la
tanda. La pantalla MUST avanzar automáticamente a la frase siguiente solo al terminar esa
transición, sin que el usuario tenga que intervenir. Si el nivel resultante es igual al nivel
anterior, la pantalla MUST avanzar a la frase siguiente sin esperar ninguna transición.

#### Scenario: Un acierto que certifica muestra el indicador

- **GIVEN** una frase de nivel 2 (insignia "Plata") con racha 2
- **WHEN** el usuario la revela y se autoevalúa con acierto
- **THEN** la insignia junto al nivel CEFR de esa frase transiciona de "Plata" a "Oro", sin que
  la pantalla avance todavía a la frase siguiente

#### Scenario: Un fallo que baja el nivel muestra el indicador

- **GIVEN** una frase de nivel 3 (insignia "Oro") con racha 0
- **WHEN** el usuario la revela y se autoevalúa con fallo
- **THEN** la insignia junto al nivel CEFR de esa frase transiciona de "Oro" a "Plata" (ADR-0024),
  sin que la pantalla avance todavía a la frase siguiente

#### Scenario: Un fallo con racha mayor que 0 no muestra el indicador

- **GIVEN** una frase de nivel 3 (insignia "Oro") con racha 2
- **WHEN** el usuario la revela y se autoevalúa con fallo
- **THEN** la pantalla no muestra ninguna transición de insignia y avanza de inmediato a la frase
  siguiente, porque el fallo solo restó un acierto a su racha y el nivel se mantiene en 3 (ADR-0024)

#### Scenario: Un acierto que no certifica no muestra el indicador

- **GIVEN** una frase de nivel 1 (insignia "Bronce") con racha 0
- **WHEN** el usuario la revela y se autoevalúa con acierto
- **THEN** la pantalla no muestra ninguna transición de insignia y avanza de inmediato a la frase
  siguiente

#### Scenario: Certificar de nuevo en nivel máximo no muestra el indicador

- **GIVEN** una frase de nivel 5 (insignia "Diamante") con racha 2
- **WHEN** el usuario la revela y se autoevalúa con acierto
- **THEN** la pantalla no muestra ninguna transición de insignia y avanza de inmediato a la frase
  siguiente, porque el nivel se mantiene en 5 (ADR-0015)

#### Scenario: Un fallo en nivel 0 no muestra el indicador

- **GIVEN** una frase nueva de nivel 0 (insignia "sin rango") con racha 0
- **WHEN** el usuario la revela y se autoevalúa con fallo
- **THEN** la pantalla no muestra ninguna transición de insignia y avanza de inmediato a la frase
  siguiente, porque el nivel 0 no tiene nivel inferior al que bajar (ADR-0024)

#### Scenario: El indicador desaparece sin intervención del usuario

- **GIVEN** la insignia de la frase respondida transicionando tras una subida de nivel
- **WHEN** transcurre la duración de esa transición sin que el usuario la toque
- **THEN** la pantalla avanza por sí sola a la frase siguiente de la tanda

#### Scenario: La transición no oculta el resto de la tarjeta

- **GIVEN** una autoevaluación que cambia el nivel de consolidación de la frase respondida
- **WHEN** la pantalla muestra la transición de insignia sobre esa frase
- **THEN** la traducción revelada y los controles de acierto/fallo de esa frase siguen presentes
  en pantalla, deshabilitados por estar la respuesta en curso (`busy`)

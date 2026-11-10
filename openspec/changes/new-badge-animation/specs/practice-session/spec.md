# Spec Delta

## MODIFIED Requirements

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

- **GIVEN** una frase de nivel 3 (insignia "Oro")
- **WHEN** el usuario la revela y se autoevalúa con fallo
- **THEN** la insignia junto al nivel CEFR de esa frase transiciona de "Oro" a "Plata" (ADR-0015),
  sin que la pantalla avance todavía a la frase siguiente

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

- **GIVEN** una frase nueva de nivel 0 (insignia "sin rango")
- **WHEN** el usuario la revela y se autoevalúa con fallo
- **THEN** la pantalla no muestra ninguna transición de insignia y avanza de inmediato a la frase
  siguiente, porque el nivel 0 no tiene nivel inferior al que bajar (ADR-0015)

#### Scenario: El indicador desaparece sin intervención del usuario

- **GIVEN** la insignia de la frase respondida transicionando tras una subida de nivel
- **WHEN** transcurre la duración de esa transición sin que el usuario la toque
- **THEN** la pantalla avanza por sí sola a la frase siguiente de la tanda

#### Scenario: La transición no oculta el resto de la tarjeta

- **GIVEN** una autoevaluación que cambia el nivel de consolidación de la frase respondida
- **WHEN** la pantalla muestra la transición de insignia sobre esa frase
- **THEN** la traducción revelada y los controles de acierto/fallo de esa frase siguen presentes
  en pantalla, deshabilitados por estar la respuesta en curso (`busy`)

## REMOVED Requirements

### Requirement: El indicador no bloquea la interacción con la frase siguiente

**Reason**: Dejó de existir "la frase siguiente" bajo el indicador: ahora la transición ocurre
sobre la propia frase respondida, antes de avanzar, no sobre una frase ya navegada.

**Migration**: Su intención (que los controles sigan presentes y visibles mientras se anima)
queda absorbida como el escenario "La transición no oculta el resto de la tarjeta" del requisito
"Indicador animado de cambio de insignia", en este mismo delta.

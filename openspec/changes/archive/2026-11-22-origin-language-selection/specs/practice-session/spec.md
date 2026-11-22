# Spec Delta: practice-session

## ADDED Requirements

### Requirement: El cambio del idioma del dispositivo se refleja al volver a Practicar

Al volver a la pantalla de práctica después de que cambie el idioma de origen (porque cambió el idioma del
dispositivo), la pantalla MUST mostrar la misma tanda activa, en el mismo orden y con las mismas rachas, con el
texto de origen en el nuevo idioma de origen. La frase mostrada MUST volver a quedar sin revelar.

#### Scenario: Volver a Practicar tras cambiar el idioma del dispositivo muestra el nuevo texto de origen

- **GIVEN** la pantalla de práctica cargada con origen `es-ES` y una tanda activa cuya primera frase A muestra
  "Buenos días", y el texto `en-GB` de A es "Good morning"
- **WHEN** el idioma del dispositivo pasa a `en-GB` y el usuario vuelve a Practicar
- **THEN** la pantalla muestra "Good morning" sin revelar, la tanda conserva sus frases, orden y rachas, y no
  muestra "Buenos días"

#### Scenario: Volver a Practicar sin cambios conserva el estado

- **GIVEN** la pantalla de práctica con la frase actual revelada
- **WHEN** el usuario va a Ajustes, no cambia nada, y vuelve a Practicar
- **THEN** la frase actual sigue revelada

## MODIFIED Requirements

### Requirement: Frase en español con traducción oculta

La pantalla MUST mostrar la frase de la tanda activa en el idioma de origen y MUST ocultar la frase en el idioma
destino hasta que el usuario la revele. La frase que se muestra MUST ser la primera de la lista de la tanda.

#### Scenario: Al abrir la práctica se muestra la primera frase en español

- **GIVEN** una tanda activa con las frases A, B y C en ese orden, donde A tiene el texto en español "Buenos días" y en portugués "Bom dia", con `es-ES` como idioma de origen y `pt-PT` como idioma destino activo
- **WHEN** el usuario abre la pantalla de práctica
- **THEN** la pantalla muestra "Buenos días" y no muestra "Bom dia"

#### Scenario: Revelar muestra la traducción en portugués

- **GIVEN** la pantalla muestra la frase A sin revelar
- **WHEN** el usuario revela la frase
- **THEN** la pantalla muestra "Bom dia" junto a "Buenos días" y ofrece acierto y fallo

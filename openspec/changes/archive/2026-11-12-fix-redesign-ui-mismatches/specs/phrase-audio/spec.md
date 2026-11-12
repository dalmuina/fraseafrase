# Spec Delta

## MODIFIED Requirements

### Requirement: Botón de escuchar tras revelar

La pantalla de práctica MUST mostrar un control para escuchar el audio de la frase actual en
portugués únicamente cuando su traducción está revelada. Sin revelar, MUST NOT mostrarse ese
control. El control MUST mostrarse como un icono de audio (altavoz), sin texto visible, en todos
los estados en los que el control es visible, incluido cuando está deshabilitado, y MUST exponer
una descripción accesible equivalente al texto que ya no se muestra.

#### Scenario: Revelar muestra el control de escuchar

- **GIVEN** la pantalla muestra una frase sin revelar
- **WHEN** el usuario la revela
- **THEN** la pantalla muestra el control de escuchar junto a la traducción

#### Scenario: Sin revelar no hay control de escuchar

- **GIVEN** la pantalla muestra una frase sin revelar
- **WHEN** el usuario observa la pantalla
- **THEN** no se muestra ningún control de escuchar

#### Scenario: El control de escuchar muestra un icono de audio

- **GIVEN** la pantalla muestra una frase revelada
- **WHEN** el usuario observa el control de escuchar
- **THEN** el control muestra un icono de audio sin ningún texto visible junto a él, y expone una
  descripción accesible equivalente

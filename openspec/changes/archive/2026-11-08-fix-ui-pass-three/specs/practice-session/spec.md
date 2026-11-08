# Spec Delta

## ADDED Requirements

### Requirement: Controles de acción siempre visibles y pulsables

La pantalla de práctica MUST mantener siempre visible y pulsable, sin necesidad de ningún
desplazamiento adicional, el control de revelar la traducción cuando la frase no está revelada, o
los controles de acierto y fallo cuando sí lo está. Si el contenido de la frase y su traducción no
cabe en el espacio disponible, la pantalla MUST permitir desplazamiento (scroll) sobre ese
contenido sin desplazar ni ocultar esos controles.

#### Scenario: El control de revelar es visible con una frase muy larga

- **GIVEN** la pantalla de práctica muestra una frase sin revelar cuyo texto ocupa más alto que el
  espacio disponible en pantalla
- **WHEN** la pantalla se muestra
- **THEN** el control de revelar la traducción está visible y es pulsable sin necesitar
  desplazamiento

#### Scenario: Los controles de acierto y fallo son visibles con una traducción muy larga

- **GIVEN** la pantalla de práctica muestra una frase revelada cuya traducción ocupa más alto que el
  espacio disponible en pantalla
- **WHEN** la pantalla se muestra
- **THEN** los controles de acierto y fallo están visibles y son pulsables sin necesitar
  desplazamiento

#### Scenario: La frase y la traducción muy largas se pueden desplazar

- **GIVEN** la pantalla de práctica muestra una frase revelada cuya frase y traducción juntas ocupan
  más alto que el espacio disponible en pantalla
- **WHEN** el usuario desplaza el contenido de la frase y la traducción
- **THEN** el contenido se desplaza y los controles de acierto y fallo permanecen visibles y
  pulsables en el mismo lugar

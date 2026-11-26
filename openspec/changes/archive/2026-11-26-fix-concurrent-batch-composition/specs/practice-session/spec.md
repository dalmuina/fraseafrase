# Spec Delta

## ADDED Requirements

### Requirement: El cambio de idioma destino se refleja al volver a Practicar

Al volver a la pantalla de práctica después de cambiar el idioma destino activo, la pantalla MUST mostrar la tanda
activa del nuevo idioma destino, con su texto de destino, y la frase mostrada MUST quedar sin revelar. Esto MUST
cumplirse aunque la tanda del nuevo idioma destino tenga los mismos ids de frase, en el mismo orden y con los mismos
textos de origen que la tanda que se estaba mostrando.

#### Scenario: Mismos ids y mismo origen en otro idioma destino muestran el texto del idioma nuevo

- **GIVEN** la pantalla de práctica cargada con el idioma destino `pt-PT`, la frase actual A revelada con el texto
  de destino "Bom dia" y el texto de origen "Buenos días", y una tanda activa de `de-DE` con las mismas frases, en
  el mismo orden, cuya frase A tiene el texto de destino "Guten Morgen" y el mismo texto de origen
- **WHEN** el idioma destino activo pasa a `de-DE` y el usuario vuelve a Practicar
- **THEN** la pantalla muestra la frase A sin revelar, con el texto de destino "Guten Morgen", el idioma destino
  activo es `de-DE` y no muestra "Bom dia"

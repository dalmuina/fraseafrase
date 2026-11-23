## MODIFIED Requirements

### Requirement: Icono del idioma activo en la cabecera

Las pantallas de Practicar, Progreso y Ajustes MUST mostrar, en su cabecera, un icono circular que
identifica el idioma destino activo. El icono MUST reflejar el idioma destino activo en el momento
de mostrarse.

#### Scenario: El icono muestra el idioma activo en Practicar

- **GIVEN** el idioma destino activo `pt-PT`
- **WHEN** el usuario abre la pantalla de Practicar
- **THEN** la cabecera muestra el icono circular correspondiente a `pt-PT`

#### Scenario: El icono muestra el idioma activo en Progreso y Ajustes

- **GIVEN** el idioma destino activo `en-GB`
- **WHEN** el usuario abre la pantalla de Progreso o la de Ajustes
- **THEN** la cabecera de esa pantalla muestra el icono circular correspondiente a `en-GB`

### Requirement: El icono se actualiza tras cambiar de idioma

Tras cambiar el idioma destino activo desde Ajustes, el icono de la cabecera de cualquiera de las
tres pantallas MUST mostrar el nuevo idioma destino activo la siguiente vez que se muestre.

#### Scenario: El icono refleja el idioma recién elegido

- **GIVEN** el idioma destino activo `pt-PT` y el usuario lo cambia a `en-GB` desde Ajustes
- **WHEN** el usuario abre la pantalla de Practicar
- **THEN** la cabecera muestra el icono circular correspondiente a `en-GB`

# Spec Delta

## REMOVED Requirements

### Requirement: Persistencia del catálogo de frases en Room

**Reason**: El resembrado dependía de un cambio de `schemaVersion`, que solo admite el valor 1: nunca podía ocurrir.
**Migration**: La sustituye "Persistencia y resembrado del catálogo en Room". La caché del catálogo se recrea al
actualizar; el progreso no cambia.

## ADDED Requirements

### Requirement: Persistencia y resembrado del catálogo en Room

El contenido de catálogo validado MUST persistirse en tablas de consulta locales, sembradas desde los documentos de
frases empaquetados. Cada idioma MUST sembrarse de forma independiente y volver a sembrarse cuando cambie el contenido
de su documento, aunque no cambie `schemaVersion`. El sembrado MUST NOT modificar el progreso de ningún idioma destino
ni las frases sembradas de otro idioma.

#### Scenario: Primer arranque siembra el catálogo

- **GIVEN** una instalación nueva sin catálogo sembrado y los documentos de frases de `pt-PT`, `it-IT` y `es-ES`
- **WHEN** la app arranca
- **THEN** las frases de los tres idiomas quedan disponibles para consulta, cada una con el texto de origen de
  cualquiera de los otros dos

#### Scenario: Una frase añadida llega a una instalación ya sembrada

- **GIVEN** los idiomas `pt-PT` y `es-ES` ya sembrados con una frase cada uno
- **WHEN** la app arranca con documentos de ambos idiomas que añaden una segunda frase al final y mantienen
  `schemaVersion` igual a `1`
- **THEN** la consulta de `pt-PT` con origen `es-ES` devuelve las dos frases, en el orden del documento

#### Scenario: Un texto corregido sustituye al anterior sin afectar a otros idiomas

- **GIVEN** los idiomas `pt-PT`, `it-IT` y `es-ES` ya sembrados
- **WHEN** la app arranca con un documento de `pt-PT` que cambia el texto de una frase, y los otros documentos iguales
- **THEN** `pt-PT` devuelve el texto corregido e `it-IT` conserva sus frases sin volver a sembrarse

#### Scenario: Sin cambio de contenido no se vuelve a sembrar

- **GIVEN** un idioma ya sembrado cuyo documento de frases es idéntico al del último arranque
- **WHEN** la app arranca
- **THEN** ese documento no se vuelve a sembrar

#### Scenario: Un documento nuevo inválido conserva lo sembrado

- **GIVEN** un idioma ya sembrado
- **WHEN** la app arranca con un documento de ese idioma cuyo contenido cambió y no supera la validación
- **THEN** las frases sembradas de ese idioma siguen disponibles sin cambios

#### Scenario: Resembrar un idioma no cambia el progreso guardado

- **GIVEN** `pt-PT` ya sembrado con la frase `a1-saludos-001` y progreso guardado para ella con nivel 2 y 1 acierto
- **WHEN** la app arranca con un documento de `pt-PT` que añade una segunda frase al final
- **THEN** el progreso guardado de `a1-saludos-001` sigue con nivel 2 y 1 acierto

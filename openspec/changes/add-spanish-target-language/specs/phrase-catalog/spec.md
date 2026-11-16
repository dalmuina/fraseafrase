# Spec Delta

## MODIFIED Requirements

### Requirement: Resolución del idioma de origen por defecto

El idioma de origen de las frases MUST NOT fijarse en código a un valor concreto. Para un idioma
destino dado, el sistema MUST usar como origen el locale del dispositivo si existe contenido de
catálogo con ese `sourceLocale` para ese idioma destino. En caso contrario, el sistema MUST usar como
respaldo un idioma de origen disponible para ese idioma destino, y ese respaldo MUST NOT ser igual al
propio idioma destino que se está resolviendo.

#### Scenario: El locale del dispositivo coincide con el origen disponible

- **GIVEN** un catálogo de `pt-PT` cuyo `sourceLocale` es `es-ES` y un dispositivo con locale `es-ES`
- **WHEN** se resuelve el idioma de origen para `pt-PT`
- **THEN** el idioma de origen resuelto es `es-ES`

#### Scenario: El locale del dispositivo no tiene contenido disponible

- **GIVEN** un catálogo de `pt-PT` cuyo único `sourceLocale` disponible es `es-ES` y un dispositivo
  con locale `en-US`
- **WHEN** se resuelve el idioma de origen para `pt-PT`
- **THEN** el idioma de origen resuelto es `es-ES`

#### Scenario: El respaldo no coincide con el propio idioma destino

- **GIVEN** un catálogo de `es-ES` cuyo único `sourceLocale` disponible es `pt-PT` y un dispositivo con
  locale `es-ES`
- **WHEN** se resuelve el idioma de origen para `es-ES`
- **THEN** el idioma de origen resuelto es `pt-PT`, no `es-ES`

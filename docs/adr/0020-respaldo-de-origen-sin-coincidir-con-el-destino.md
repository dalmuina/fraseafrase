# ADR-0020: El respaldo del idioma de origen nunca coincide con el idioma destino

- **Estado:** Sustituido por ADR-0023
- **Fecha:** 2026-11-16
- **Modifica a:** ADR-0019 (solo la regla de respaldo de `ResolveSourceLocale`)

## Contexto

ADR-0019 introdujo `ResolveSourceLocale`, que resuelve el idioma de origen de un idioma destino: usa
el locale del dispositivo si hay contenido con ese `sourceLocale`, y en caso contrario "se usa `es-ES`
por defecto". En el código ese respaldo es un literal fijo (`LanguageTag("es-ES")`), razonable en ese
momento porque `es-ES` solo podía ser origen, nunca destino.

El cambio `add-spanish-target-language` añade `es-ES` como idioma destino nuevo (con origen `pt-PT`).
Con el respaldo actual, si el idioma destino elegido es `es-ES` y el dispositivo no tiene un origen
distinto disponible, `ResolveSourceLocale` devolvería `es-ES` como origen del propio `es-ES` -
"español desde español", un resultado sin sentido para el usuario. El literal de ADR-0019 deja de ser
válido en general: ahora que el conjunto de idiomas destino puede incluir cualquier locale, un
respaldo fijo puede coincidir con el destino que se está resolviendo.

## Decisión

El respaldo de `ResolveSourceLocale` deja de ser un literal fijo y pasa a calcularse a partir de los
orígenes disponibles para el idioma destino que se está resolviendo, excluyendo siempre ese propio
idioma destino del resultado:

1. Si el locale del dispositivo está entre los orígenes disponibles para ese destino (y no es el
   propio destino), se usa.
2. Si no, y `es-ES` está entre los orígenes disponibles para ese destino (y no es el propio destino),
   se usa `es-ES` - preserva el comportamiento ya observable para `pt-PT`/`pt-BR`.
3. Si no, se usa el primer origen disponible para ese destino distinto del propio destino.
4. Si no hay ningún origen disponible distinto del propio destino (el catálogo de ese idioma destino
   falló al cargar o está vacío), se usa el locale del dispositivo tal cual, sin lanzar error: en ese
   caso no hay ninguna noción de "origen disponible" que respetar, y el resultado no tiene efecto
   observable porque la consulta de frases ya va a devolver el mismo fallo de catálogo con
   independencia del origen que se le pase.

Esta regla sustituye únicamente la frase de ADR-0019 "si el catálogo no tiene contenido para ese
locale como origen, se usa `es-ES` por defecto" por una regla que nunca puede resolver un origen igual
al destino, para cualquier idioma destino presente o futuro. El resto de ADR-0019 (catálogo en Room
sembrado por idioma destino, selección de idioma destino en Ajustes, independencia por idioma destino
de nivel CEFR/N/tanda activa/progreso, `DeviceLocaleProvider` como puerto en `:core:platform`) no
cambia.

## Consecuencias

- **Positivas:**
  - Ningún idioma destino, presente o futuro, puede resolver un origen igual a sí mismo.
  - El comportamiento observable de `pt-PT` y `pt-BR` no cambia: su único origen disponible sigue
    siendo `es-ES`, que sigue siendo el resultado preferido entre los candidatos.
  - El dominio sigue sin un literal de idioma fijo e incondicional (ADR-0003): `es-ES` solo se usa como
    preferencia entre los orígenes realmente disponibles para ese destino, nunca como valor
    incondicional.
- **Negativas:**
  - `ResolveSourceLocale` gana un parámetro (`targetLocale`), con un único punto de llamada que
    actualizar (`ObtainCatalogPhrasesForActiveLanguage`).
- **Descartado:**
  - Excluir `es-ES` de `availableTargetLocales()` o de la lista de Ajustes cuando no tuviera un
    origen distinto de sí mismo disponible: ataca el síntoma (qué idiomas destino puede ofrecer la
    app) en vez de la causa (un respaldo mal calculado), y habría añadido lógica de exclusión a
    `ObtainAvailableLanguages` y a la pantalla de Ajustes sin necesidad.

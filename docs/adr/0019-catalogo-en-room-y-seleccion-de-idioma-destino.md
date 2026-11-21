# ADR-0019: Catálogo en Room, selección de idioma destino y origen derivado del dispositivo

- **Estado:** Sustituido por ADR-0022 (solo la parte de formato de documento)
- **Fecha:** 2026-11-15
- **Sustituye a:** ADR-0013 (solo la parte de almacenamiento del catálogo y la excepción de variante)
- **Modifica a:** ADR-0003 (la variante deja de estar diferida; el origen deja de ser un literal fijo)

## Contexto

El MVP enseña un único idioma destino (`pt-PT`) desde un único origen (`es-ES`). ADR-0003 fijó esto
a propósito y dejó explícito que añadir `pt-BR` u otro idioma sería "añadir contenido, no modificar
código", pero que "la variante no es seleccionable en la UI del MVP". ADR-0013, al decidir cómo
persistir el catálogo, fue más allá y difirió también el ajuste de variante, dejando el locale de la
raíz del JSON como única fuente de locales "en ese cambio".

Ahora se quiere: (a) añadir `pt-BR` como segundo idioma destino con contenido propio, (b) dejar que
el usuario elija el idioma destino desde Ajustes, (c) derivar el idioma de origen del locale del
dispositivo en vez de fijarlo a `es-ES` en código, con `es-ES` como valor por defecto cuando no hay
contenido para el locale del dispositivo (hoy es el único origen con contenido), y (d) llevar nivel
CEFR, tamaño de tanda, tanda activa, contador de sesión y progreso por frase a ser independientes por
idioma destino.

Esto excede lo que ADR-0013 dejaba abierto ("el locale de la raíz del catálogo es la única fuente de
locales en este cambio") y contradice directamente su decisión de almacenamiento: un único JSON con
un único par de locales ya no puede representar dos idiomas destino con progreso independiente sin
forzar al dominio y a la UI a tratar el locale como una constante.

## Decisión

**Catálogo:** pasa de "JSON en assets, cargado en memoria" a **tablas de Room**, pero conservando la
propiedad que motivaba ADR-0013 (el contenido se revisa en diffs de texto antes de incluirse, sin
migraciones de Room para ampliarlo):
- El contenido se sigue autorizando y revisando como JSON en assets, **uno por idioma destino**
  (`catalog/phrases-pt-PT.json`, `catalog/phrases-pt-BR.json`, ...), con el mismo esquema que hoy
  (`schemaVersion`, `sourceLocale`, `targetLocale`, `phrases[]`) y las mismas reglas de permanencia e
  ids de ADR-0013 (nunca se reutilizan ni se borran, las frases nuevas van al final), ahora
  verificadas por idioma destino.
- En tiempo de ejecución, ese JSON se siembra en tablas de Room (`CatalogPhraseEntity`, clave
  `(id, targetLocale)`) reutilizando el mismo parser y las mismas validaciones que ya existen
  (`CatalogJson`). La siembra se repite cuando cambia el `schemaVersion` de un fichero; nunca toca las
  tablas de progreso del usuario.
- Añadir contenido a un idioma existente, o añadir un idioma destino nuevo, sigue sin requerir una
  migración de Room: es una siembra nueva a partir de un JSON nuevo o modificado, igual que antes.

**Idioma destino:** deja de estar diferido. Pasa a ser un ajuste de usuario seleccionable
(`activeTargetLocale`), con los idiomas destino que tengan contenido sembrado (`pt-PT`, `pt-BR`)
como opciones. El dominio sigue sin literales de idioma (ADR-0003): la lista de idiomas disponibles
se deriva del catálogo, no de una constante en código.

**Idioma de origen:** deja de fijarse en código a `es`. Se deriva del locale del dispositivo a través
de un puerto nuevo del dominio (`DeviceLocaleProvider`), implementado en `:core:platform`. Si el
catálogo no tiene contenido para ese locale como origen, se usa `es-ES` por defecto. Hoy `es-ES` es el
único origen con contenido, así que en la práctica el resultado observable no cambia todavía; el
puerto y la regla de fallback quedan listos para cuando se añada contenido en otros orígenes.

**Independencia por idioma:** nivel CEFR, tamaño de tanda (N), tanda activa, contador de sesión y
progreso por frase (nivel de consolidación, racha) pasan a ser independientes por idioma destino.
`PracticeDatabase` sube de versión 3 a 4 para añadir `target_locale` a las claves de
`phrase_progress`, `active_batch` y `session_counter`. Igual que en ADR-0013, se usa
`fallbackToDestructiveMigration()` (proyecto pre-1.0.0, sin usuarios en producción): el progreso
existente se pierde, como una instalación nueva.

## Consecuencias

- **Positivas:**
  - Añadir un idioma destino nuevo, o ampliar uno existente, sigue siendo "añadir contenido, no
    modificar código" para la parte de catálogo, cumpliendo la promesa original de ADR-0003.
  - El progreso de cada idioma queda aislado: cambiar de idioma en Ajustes no mezcla rachas, niveles
    ni tandas activas entre idiomas.
  - El puerto de locale de origen deja el dominio preparado para más orígenes en el futuro sin
    volver a tocar el modelo de datos.
- **Negativas:**
  - El catálogo deja de ser "solo lectura desde assets": ahora vive también en Room, con una siembra
    que hay que mantener correcta y cuya ausencia de tests dejaría contenido desincronizado sin
    avisar.
  - La migración destructiva de `PracticeDatabase` v3→v4 descarta el progreso existente, igual que ya
    aceptó ADR-0013 para v1→v2 y v2→v3.
  - Más superficie en Ajustes y en el modelo de persistencia (ajustes y progreso por idioma en vez de
    globales).
- **Descartado:**
  - Mantener el catálogo únicamente en JSON con un fichero por idioma pero sin pasar por Room: habría
    cumplido "añadir idioma sin migraciones" igual de bien, pero el usuario pidió explícitamente una
    base de datos para el catálogo.
  - Derivar el origen del dispositivo sin valor por defecto: dejaría la app sin contenido legible si
    el dispositivo tiene un locale sin traducción.
  - Un único nivel CEFR/N compartido entre idiomas: mezclaría el progreso de idiomas con historiales
    de aprendizaje distintos.

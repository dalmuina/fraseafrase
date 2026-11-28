# ADR-0032: Idioma de origen por la lista de idiomas del dispositivo y el idioma base

- **Estado:** Propuesto
- **Fecha:** 2026-11-28
- **Sustituye a:** ADR-0023 (solo la regla "Idioma de origen")

## Contexto

ADR-0023 fija el idioma de origen como "el locale del dispositivo si está entre los idiomas del catálogo; si no,
el idioma de origen por defecto (hoy `es-ES`)". La comparación es de etiqueta entera y solo con el primer idioma
del dispositivo. Consecuencias observadas:

- Un móvil en `en-US`, `es-MX` o `pt-BR` ve las frases en `es-ES`, aunque el catálogo tenga `en-GB`, `es-ES` y
  `pt-PT`. La decisión D2 de `docs/plan-mvp.md` ya pedía coincidir por idioma, pero nunca llegó a implementarse.
- Android elige los textos de la interfaz con toda la lista de idiomas del dispositivo. Si el origen solo mira el
  primero, la interfaz traducida (`localize-interface`) y el texto de origen pueden quedar en idiomas distintos
  con la misma configuración.

## Decisión

El idioma de origen se resuelve con la **lista ordenada de idiomas del dispositivo**. Para cada idioma de la
lista, en orden:

1. Si hay un idioma disponible con la misma etiqueta, se usa.
2. Si no, si hay idiomas disponibles con el mismo idioma base (la subetiqueta de idioma: `en` en `en-US`), se usa
   el primero por orden alfabético de etiqueta.

Gana el primer idioma de la lista que case. Si ninguno casa, o la lista está vacía, se usa el idioma de origen
por defecto, que provee `:app` (ADR-0003).

Ejemplos con el catálogo actual: `[en-US]` → `en-GB`; `[es-MX]` → `es-ES`; `[pt-BR]` → `pt-PT`;
`[eu-ES, en-US]` → `en-GB`; `[en-US, es-ES]` → `en-GB`; `[ja-JP]` → `es-ES`.

El puerto `DeviceLocaleProvider` devuelve la lista entera y la regla vive en el dominio.

**Sin cambios respecto a ADR-0023:** el origen es único para toda la app, no se elige ni se guarda y no depende del
destino; el texto de origen sale del documento de frases del idioma de origen; ids paralelos; "origen ≠ destino"
(el destino se ajusta al origen).

## Consecuencias

- **Positivas:**
  - Los usuarios de otras regiones de un idioma del catálogo ven las frases en su idioma.
  - El origen sigue el mismo criterio que Android usa para los textos de la interfaz.
  - Resultado determinista aunque el catálogo llegue a tener dos variantes del mismo idioma.
- **Negativas:**
  - Al actualizar, el origen puede cambiar para usuarios con dispositivo en otra región (p. ej. de `es-ES` a
    `en-GB` en un `en-US`). No hay datos que migrar: el progreso es por idioma destino.
  - El desempate alfabético es arbitrario (`pt-AO` daría `pt-BR` antes que `pt-PT` si ambos existieran).
  - No distingue scripts (`zh-Hans` / `zh-Hant`); ninguno está en el catálogo.
- **Descartado:**
  - Solo el primer idioma del dispositivo: deja la discrepancia con los textos de Android.
  - Dos pasadas (coincidencias exactas en toda la lista antes que por idioma base): con `[en-US, es-ES]` daría
    `es-ES`, contra el orden de preferencia del usuario.
  - Desempatar por el orden de los idiomas disponibles: ese orden no está garantizado por la consulta del catálogo.
  - Elegir el origen en Ajustes: sigue descartado, como en ADR-0023.

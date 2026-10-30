# ADR-0003: Portugués europeo con modelo de datos multivariante

- **Estado:** Aceptado
- **Fecha:** 2026-10-29

## Contexto
El portugués europeo (pt-PT) y el brasileño (pt-BR) difieren en vocabulario, gramática (uso de
*tu*/*você*, gerundio, colocación de pronombres) y pronunciación. Mezclarlos confunde al estudiante.
El idioma de origen del usuario es el español.

## Decisión
- El MVP enseña **portugués europeo (`pt-PT`)** desde **español (`es`)**.
- Todo el modelo de datos identifica los idiomas con **etiquetas BCP-47** (`pt-PT`, `pt-BR`, `es-ES`…), nunca con valores fijos en el código:
  - cada frase tiene `targetLocale` y `sourceLocale`;
  - el TTS y el reconocimiento de voz reciben el locale de la frase.
- El dominio no contiene ninguna referencia literal a `pt-PT`: la variante activa es un ajuste del usuario con valor por defecto `pt-PT`.

## Consecuencias
- **Positivas:** añadir `pt-BR` u otro idioma en el futuro es añadir contenido, no modificar código.
- **Negativas:** el catálogo inicial solo cubre `pt-PT`; la variante no es seleccionable en la UI del MVP.
- **Descartado:** contenido en portugués "neutro" o mezclado.

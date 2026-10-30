# ADR-0001: Registrar decisiones de arquitectura con ADR

- **Estado:** Aceptado
- **Fecha:** 2026-10-29

## Contexto
El proyecto se desarrolla con un asistente de IA (Claude) que no conserva memoria entre sesiones.
Sin un registro explícito, las decisiones de arquitectura se reabren o se contradicen en cada sesión,
y el código deriva hacia soluciones inconsistentes.

## Decisión
Toda decisión de arquitectura relevante se registra como un ADR en `docs/adr/`, numerado de forma
correlativa (`NNNN-titulo-en-kebab-case.md`) y con la plantilla de `docs/adr/README.md`.

- Un ADR aceptado no se edita: si la decisión cambia, se crea un ADR nuevo que lo sustituye.
- `CLAUDE.md` y `openspec/config.yaml` referencian los ADR vigentes, de modo que la IA los lee antes de proponer cambios.
- Una propuesta de OpenSpec que contradiga un ADR aceptado debe incluir un ADR nuevo que lo sustituya.

## Consecuencias
- **Positivas:** las decisiones quedan versionadas en Git junto al código; la IA tiene una fuente de verdad estable.
- **Negativas:** pequeño coste de documentación por cada decisión.
- **Descartado:** documentar decisiones solo en conversaciones o en wikis externas.

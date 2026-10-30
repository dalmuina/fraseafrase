# ADR-0002: Desarrollo guiado por especificaciones con OpenSpec

- **Estado:** Aceptado
- **Fecha:** 2026-10-29

## Contexto
El código lo escribe mayoritariamente un asistente de IA. Sin una especificación previa, la IA
interpreta los requisitos a su manera, amplía el alcance y produce comportamiento difícil de verificar.

## Decisión
El proyecto sigue **Spec-Driven Development (SDD)** con **OpenSpec** (Fission AI).

- Las especificaciones vigentes viven en `openspec/specs/<dominio>/spec.md` y describen el comportamiento del sistema.
- Todo cambio funcional empieza como un *change* en `openspec/changes/<nombre>/` con `proposal.md`, `design.md`, `tasks.md` y los deltas de especificación.
- Ciclo obligatorio: `/opsx:explore` (si el alcance no está claro) → `/opsx:propose` → revisión humana → `/opsx:apply` → `/opsx:verify` → `/opsx:archive`.
- Se usa el perfil **custom** de OpenSpec para disponer de `verify`, `sync` y el resto de flujos ampliados.
- Ningún código de producción se escribe sin una tarea en un `tasks.md` aprobado.
- Los requisitos se redactan con escenarios verificables (*GIVEN / WHEN / THEN*), que se traducen directamente en tests.

## Consecuencias
- **Positivas:** alineación previa con la IA, trazabilidad requisito → test → código, revisiones centradas en el comportamiento.
- **Negativas:** sobrecoste en cambios muy pequeños (se mitiga con propuestas cortas; las reglas son mínimos, no cuotas).
- **Descartado:** GitHub Spec Kit (puertas de fase más rígidas) y desarrollo sin especificación previa.

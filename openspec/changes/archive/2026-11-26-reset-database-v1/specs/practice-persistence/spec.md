# Spec Delta

## REMOVED Requirements

### Requirement: Descarte del estado guardado con un esquema anterior

**Reason**: Antes de publicar 1.0.0, `PracticeDatabase` vuelve a la versión 1 y deja de tener esquemas anteriores.
A partir de 1.0.0, el estado del usuario nunca se descarta al cambiar de esquema: cada subida de versión necesita
una migración que conserve el progreso (ADR-0030).

**Migration**: Las instalaciones de desarrollo con una base de una versión superior a 1 tienen que desinstalar la
app o borrar sus datos. No hay instalaciones de usuarios afectadas.

---
description: Diagnostica en qué punto del flujo SDD está el cambio y ejecuta el siguiente paso si no hay dudas
---

# /next

Decide el siguiente paso del flujo de CLAUDE.md (explore → propose → android-architect → apply por grupos → code-reviewer → verify → archive → push → PR del usuario) y, si no hay nada que decidir, ejecútalo. Un solo paso por invocación.

## 1. Diagnóstico (solo lectura)
- git branch --show-current, git status --short, git log --oneline develop..HEAD
- .claude/handoff.md: Fase, Veredicto android-architect, decisiones pendientes, siguiente paso
- openspec list y, si hay cambio activo, los artefactos de openspec/changes/<cambio>/ y las casillas de tasks.md
- docs/deuda-tecnica.md
Si el handoff y el repositorio no coinciden, para y explica la discrepancia.

## 2. Reglas (aplica la primera que se cumpla)
1. Hay decisiones pendientes o preguntas sin responder: lístalas con opciones y recomendación. No avances.
2. Hay cambios sin commitear: indica qué falta para cerrar el grupo. No avances.
3. Estás en develop o master: es un paso del usuario, crear 1.0.0/<cambio> desde develop actualizado. Propón el siguiente cambio a partir del handoff y de docs/deuda-tecnica.md. No crees ramas.
4. Rama de cambio sin carpeta en openspec/changes: /opsx:explore, o /opsx:propose si el explore ya está resuelto en el handoff.
5. Propuesta sin veredicto APROBADO: android-architect (verificación de cierre si ya hubo rondas) y openspec validate <cambio> --strict.
6. APROBADO y tareas pendientes: /opsx:apply solo del primer grupo con tareas sin marcar, con tdd-implementer.
7. Todas las tareas marcadas y code-reviewer sin ejecutar: code-reviewer.
8. Sugerencias del reviewer que afectan a spec o design: aplicarlas.
9. Revisión limpia y verify sin ejecutar: /opsx:verify.
10. Verify correcto: tareas de cierre (líneas de estado de los ADR si las hay), /opsx:archive, openspec validate --specs y push de la rama. Antes de archivar, muestra un resumen y pide confirmación.
11. Rama archivada y con push hecho: es un paso del usuario, abrir la PR a develop, comprobar la CI y hacer merge.

## 3. Límites
- Ejecuta solo el paso decidido y para. Nunca encadenes dos pasos.
- Nunca crees ni borres ramas, hagas merge, abras PR ni lances /opsx:apply sin APROBADO.
- En los pasos del usuario (3 y 11) solo indica qué hacer.

## 4. Salida
Termina con:
- Fase actual
- Paso ejecutado (o propuesto, si es del usuario)
- Siguiente paso después de este
- Línea de estado de contexto (✅ / ⚠️ / ⛔)
Antes de terminar, actualiza el campo Fase de .claude/handoff.md.

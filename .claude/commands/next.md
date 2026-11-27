---
description: Diagnostica en qué punto del flujo SDD está el cambio y ejecuta el siguiente paso si no hay dudas
---

# /next

Decide el siguiente paso del flujo de CLAUDE.md según el **Tipo** del cambio (vía rápida, contenido, ligero, normal)
y, si no hay nada que decidir, ejecútalo. Un solo paso por invocación.

## 1. Diagnóstico (solo lectura, mínimo)
- `git branch --show-current`, `git status --short`, `git log --oneline develop..HEAD`
- `.claude/handoff.md`: Tipo, Fase, Veredicto android-architect, Siguiente grupo, decisiones pendientes, siguiente paso
- `openspec list` y, si hay cambio activo, **solo** las casillas de `tasks.md` (Grep `- \[ \]` / `- \[x\]`).
  No leas `proposal.md`, `design.md` ni `specs/**` enteros para diagnosticar.
- `docs/plan-mvp.md` solo en la regla 3.
Si el handoff y el repositorio no coinciden, para y explica la discrepancia.

## 2. Reglas (aplica la primera que se cumpla)
1. Hay decisiones pendientes o preguntas sin responder: lístalas con opciones y recomendación. No avances.
2. Hay cambios sin commitear: indica qué falta para cerrar el grupo. No avances.
3. Estás en develop o master: es un paso del usuario, crear `1.0.0/<cambio>` desde develop actualizado. Propón el
   siguiente cambio a partir del handoff y de `docs/plan-mvp.md`, con su **Tipo** (tabla de CLAUDE.md). No crees ramas.
4. **Tipo vía rápida o contenido:** no hay OpenSpec. Si no está hecho, impleméntalo en esta sesión; después
   `./gradlew :<módulo>:check` de los módulos tocados y un único commit. Opcional: una ronda de code-reviewer si el
   diff no es trivial. Tras el commit, resetea `.claude/handoff.md` a la plantilla mínima de CLAUDE.md (sección
   "Contexto y sesiones"), con una línea de referencia a este cambio. Después para: push y PR lo hace el usuario.
5. Rama de cambio (ligero o normal) sin carpeta en `openspec/changes`: `/opsx:explore`, o `/opsx:propose` si el explore
   ya está resuelto en el handoff. **Modelo recomendado: Opus.**
6. Propuesta sin veredicto APROBADO: android-architect y `openspec validate <cambio> --strict`.
   - Ligero: una sola ronda. Normal: hasta dos (la segunda solo verifica los bloqueantes de la primera, que se le pasan en el prompt).
   - Si el veredicto es APROBADO con hallazgos "Corregir", aplícalos en una pasada y registra `APROBADO` + fecha; no hay nueva ronda.
   - CAMBIOS NECESARIOS: corrige los bloqueantes y lanza la segunda ronda. RECHAZADO: vuelve a propose con el usuario.
   - Al registrar APROBADO: commit `docs(openspec): ...` con los artefactos y rellena **Siguiente grupo** en el handoff
     (números de tarea, módulos, rutas de escenarios). → **`/clear`**.
7. APROBADO y quedan grupos de implementación sin marcar: **no uses la skill `/opsx:apply`** (lee todos los artefactos en la
   sesión principal). Delega directamente el primer grupo pendiente en `tdd-implementer` con esta plantilla, rellenada desde el
   handoff y las líneas del grupo en `tasks.md`:
   ```
   Cambio: <nombre>. Tareas: <N.M–N.K> de openspec/changes/<nombre>/tasks.md.
   Módulos: <:módulo>. Escenarios: <ruta spec.md> → "<nombre del escenario>", ...
   Ficheros previstos (design.md): <producción> / <test>.
   Sección de design.md a consultar, si alguna: "<título>".
   ```
   - Grupo mecánico (ramas `when`, recursos, renombrados, fakes triviales): pasa `model: haiku`.
   - Grupo de contenido (assets, traducciones): no va a `tdd-implementer`; hazlo en la sesión principal sin leer los assets enteros.
   - Sección final de verificación (`check` global, `koverVerify`): la ejecuta la sesión principal, no un subagente.
   Al volver: commit del grupo, actualiza **Siguiente grupo** y Fase en el handoff. → **`/clear`**.
8. Todas las tareas marcadas y code-reviewer sin ejecutar: code-reviewer (una ronda en ligero; hasta dos en normal).
   Los hallazgos "Corregir" se aplican en una pasada y se commitean sin nueva revisión. → **`/clear`**.
9. Sugerencias del reviewer que afectan a spec o design: aplicarlas con `/opsx:update`.
10. Revisión limpia y verify sin ejecutar: `/opsx:verify` (opcional en cambios ligeros). → **`/clear`**.
11. Verify correcto (o ligero con revisión limpia): tareas de cierre (líneas de estado de los ADR si las hay), `/opsx:archive`
    y `openspec validate --specs`. Antes de archivar, muestra un resumen y pide confirmación. El commit final de archive
    (`docs(openspec): archive <cambio> and sync specs`) dispara push + apertura de PR a develop (hook `post-archive-pr.sh`).
12. PR ya abierta por el hook: paso del usuario, comprobar la CI y hacer merge. Resetea `.claude/handoff.md`
    a la plantilla mínima de CLAUDE.md (sección "Contexto y sesiones").

## 2b. Presupuesto de revisión
Aplica el apartado "Presupuesto de revisión" de CLAUDE.md y pásalo en el prompt de cada subagente de revisión:
solo un bloqueante detiene el avance; "Corregir" se arregla en una pasada sin nueva revisión; "Descartar" no se hace;
máximo dos rondas por fase; el cambio ligero lleva una ronda de android-architect y una de code-reviewer.

## 2c. Modelo por fase
- explore, propose y decisiones de diseño: **Opus**.
- apply (orquestación), review, verify y archive: **Sonnet** (`/model sonnet`). Si la sesión está en Opus al entrar en
  la regla 7, recomienda el cambio antes de delegar.

## 3. Límites
- Ejecuta solo el paso decidido y para. Nunca encadenes dos pasos.
- Nunca crees ni borres ramas, hagas merge, abras PR ni delegues en `tdd-implementer` sin APROBADO.
- En el paso del usuario (3) solo indica qué hacer; en el 12, confirma que el hook ha abierto la PR y recuerda comprobar la CI antes de mergear.

## 4. Salida
Antes de terminar, actualiza Fase (y Siguiente grupo si aplica) en `.claude/handoff.md`. Termina con:
- Tipo y fase actual
- Paso ejecutado (o propuesto, si es del usuario)
- Siguiente paso después de este y modelo recomendado
- Línea de estado de contexto (✅ / ⚠️ / ⛔). Si es ✅ en un punto marcado con **`/clear`**, dilo explícitamente:
  "→ Ejecuta `/clear` y vuelve a `/next`".

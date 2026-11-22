# Recomendaciones de robustez

Mejoras detectadas durante `origin-language-selection` (2026-11-22) para abordar en un cambio propio. Ninguna
autoriza a ampliar el alcance de un cambio en curso (CLAUDE.md, regla 5). Complementan `docs/deuda-tecnica.md`, que
registra deuda de cambios concretos; aquí van las de carácter transversal. Cada una indica su **Tipo** probable.

## 1. Migraciones de Room sin test automático (normal)

- **Qué pasa.** `CatalogDatabase` v3 usa `fallbackToDestructiveMigration` (el catálogo se resiembra desde los assets).
  El escenario `phrase-catalog` "La actualización desde la versión con traducciones no pierde progreso" no tiene
  test: `createCatalogDatabase(context)` exige `Context` y `:core:data` no tiene `androidTest`. Hoy lo cubre solo
  la prueba manual de la tarea 8.3 del cambio.
- **Riesgo.** Una futura migración de `PracticeDatabase` (que sí guarda progreso del usuario) llegaría con la misma
  falta de red: pérdida de datos de estudio sin test que lo detecte.
- **Recomendación.** Crear el source set `androidTest` en `:core:data` con `MigrationTestHelper` (esquemas
  exportados ya existentes en `core/data/schemas/`) y cubrir al menos `PracticeDatabase` entre versiones
  consecutivas. Valorar que la fábrica de bases acepte una ruta/nombre para poder probarla sin `Context` real.
  Recordar el filtro por clase en tests instrumentados (CLAUDE.md, "Comandos").

## 2. Hooks con ruta relativa (ligero)

- **Qué pasa.** `.claude/settings.json` invoca `bash .claude/hooks/pre-commit-check.sh` y
  `bash .claude/hooks/post-archive-pr.sh` con ruta relativa. Si el directorio de trabajo no es la raíz del repo
  (p. ej. tras un `cd` a una subcarpeta), fallan con exit 127 y el hook de pre-commit no ejecuta `./gradlew check`.
- **Recomendación.** Usar `"$CLAUDE_PROJECT_DIR"/.claude/hooks/...` en los comandos. Opcional: que
  `pre-commit-check.sh` haga `cd "$(git rev-parse --show-toplevel)"` al empezar y que falle de forma ruidosa (no
  silenciosa) si no encuentra Gradle. Comprobar tras el cambio con un commit de prueba desde una subcarpeta.

## 3. Evidencia de tareas marcadas sin comprobación (proceso)

- **Qué pasa.** La tarea 3.3 estaba marcada `[x]` prometiendo un test de `CatalogDatabaseTest` que no existía; solo
  lo detectó la ronda 1 de `code-reviewer`.
- **Recomendación.** En `/next` (regla 7), al volver de `tdd-implementer`, comprobar que cada test nombrado en la
  tarea existe (`grep` del nombre de clase/método) antes de marcar el grupo. Es una comprobación barata y evita una
  ronda de revisión por un bloqueante de forma.

## 4. Finales de línea sin normalizar (ligero)

- **Qué pasa.** No hay `.gitattributes`; cada `git add`/`commit` en Windows avisa "LF will be replaced by CRLF" en
  los ficheros editados. Riesgo de diffs ruidosos y de fallos de ktlint/detekt según quién edite y dónde.
- **Recomendación.** Añadir `.gitattributes` con `* text=auto eol=lf` (y `*.bat` / `*.cmd` en CRLF si hace falta) y
  alinearlo con `.editorconfig` (`end_of_line = lf`). Renormalizar en un commit aparte (`git add --renormalize .`).

## 5. Comentarios que describen código ya eliminado (ligero)

- **Qué pasa.** Tras borrar `CatalogTranslationEntity`, `core/data/.../catalog/CatalogPhraseEntity.kt:8` sigue
  diciendo que el texto de origen vive en esa entidad.
- **Recomendación.** Corregir el KDoc (el origen sale de un self-JOIN con la misma tabla). Como medida general,
  tras eliminar un tipo, buscar su nombre en comentarios (`grep -rn "NombreTipo"`) y añadirlo a la lista de cierre
  de las tareas de borrado.

## 6. Cobertura de escenarios dependientes de la plataforma (normal)

- **Qué pasa.** Escenarios que dependen de `Context`, locale del dispositivo o ciclo de vida (p. ej. "Un cambio del
  idioma del dispositivo cambia el origen") se prueban con fakes de dominio, pero la integración real con
  `Configuration`/`LocaleList` solo se verifica a mano.
- **Recomendación.** Antes de la próxima feature que dependa de estado del sistema, añadir un test instrumentado
  mínimo por frontera de `:core:platform` (locale, TTS, reconocimiento de voz) detrás de un puerto ya existente, y
  documentar en `tasks.md` cuáles quedan manuales y por qué.

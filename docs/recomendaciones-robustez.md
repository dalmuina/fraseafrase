# Recomendaciones de robustez

Mejoras detectadas durante `origin-language-selection` (2026-11-22) para abordar en un cambio propio. Ninguna
autoriza a ampliar el alcance de un cambio en curso (CLAUDE.md, regla 5). Complementan `docs/deuda-tecnica.md`, que
registra la deuda que nace de un cambio o revisión concretos; aquí van las que afectan al proyecto en general. Cada una
indica su **Tipo** probable (tabla "Tipos de cambio" de CLAUDE.md).

## 1. Migraciones destructivas de Room antes de la 1.0.0 (normal)

- **Qué pasa.** `CatalogDatabase` (v3) y `PracticeDatabase` (v4) se construyen con `fallbackToDestructiveMigration()`.
  En el catálogo es aceptable: se resiembra desde los assets. En `PracticeDatabase` (`PracticeSchema.kt`) se justificó
  como proyecto pre-1.0.0 sin usuarios (design D7 de `repeat-by-session-counter`), pero esa base guarda el progreso de
  estudio. Además, el escenario `phrase-catalog` "La actualización desde la versión con traducciones no pierde
  progreso" no tiene test automático: lo cubre solo la prueba manual de la tarea 8.3 de `origin-language-selection`.
- **Riesgo.** Si la 1.0.0 se publica con el fallback, cualquier subida de versión del esquema de `PracticeDatabase`
  borra el progreso del usuario sin aviso y sin que ningún test lo detecte.
- **Recomendación.** Antes de publicar la 1.0.0, quitar `fallbackToDestructiveMigration()` de `PracticeDatabase` y
  exigir una `Migration` explícita por versión, con un test por migración. No hace falta `androidTest` ni `Context`:
  `PracticeMigrationTest` (`core/data/src/test`) ya prueba una migración en JVM con `BundledSQLiteDriver` y `@TempDir`,
  sin pasar por `createPracticeDatabase(context)`. Reutilizar ese patrón para el escenario de `CatalogDatabase` y
  apoyarse en los esquemas exportados de `core/data/schemas/`.

## 2. Hooks con ruta relativa (ligero)

- **Qué pasa.** `.claude/settings.json` invoca `bash .claude/hooks/pre-commit-check.sh` y
  `bash .claude/hooks/post-archive-pr.sh` con ruta relativa. Si el directorio de trabajo no es la raíz del repo
  (p. ej. tras un `cd` a una subcarpeta), `bash` no encuentra el script y termina con exit 127. Un hook `PreToolUse`
  que termina con un código distinto de 2 no bloquea la acción, así que el commit se hace **sin** `./gradlew check`.
  El script en sí ya hace `cd "$(git rev-parse --show-toplevel)"`; el fallo está solo en la invocación.
- **Recomendación.** Usar `"$CLAUDE_PROJECT_DIR"/.claude/hooks/...` en los comandos de `.claude/settings.json`.
  Revisar también el `|| exit 0` de `pre-commit-check.sh`: si `git rev-parse` falla, el commit pasa en silencio; es
  preferible bloquear (exit 2) con un mensaje. Comprobar tras el cambio con un commit de prueba desde una subcarpeta.

## 3. Comprobaciones de cierre de tareas (ligero)

- **Qué pasa.** La tarea 3.3 de `origin-language-selection` estaba marcada `[x]` prometiendo un test de
  `CatalogDatabaseTest` que no existía; solo lo detectó la ronda 1 de `code-reviewer`. En el mismo cambio, tras borrar
  `CatalogTranslationEntity`, el KDoc de `CatalogPhraseEntity` siguió describiendo la entidad eliminada.
- **Recomendación.** En `/next` (regla 7), al volver de `tdd-implementer`, comprobar que cada test nombrado en las
  tareas del grupo existe (`grep` del nombre de clase o método) antes de commitear el grupo. En las tareas que borran
  un tipo, buscar su nombre también en comentarios y KDoc (`grep -rn "NombreTipo"`). Son comprobaciones baratas que
  evitan una ronda de revisión por un bloqueante de forma. Si se considera solo un cambio de texto en
  `.claude/commands/next.md`, puede ir por vía rápida.

## 4. Finales de línea sin normalizar (ligero)

- **Qué pasa.** No hay `.gitattributes`; cada `git add`/`commit` en Windows avisa "LF will be replaced by CRLF" en
  los ficheros editados. `.editorconfig` ya fija `end_of_line = lf`, pero Git no lo aplica. Riesgo de diffs ruidosos
  y de fallos de ktlint/detekt según quién edite y dónde.
- **Recomendación.** Añadir `.gitattributes` con `* text=auto eol=lf` y `*.bat text eol=crlf` (`gradlew.bat` debe
  conservar CRLF). Renormalizar en un commit aparte (`git add --renormalize .`).

## 5. Cobertura de escenarios dependientes de la plataforma (normal)

- **Qué pasa.** Escenarios que dependen de `Context`, locale del dispositivo o ciclo de vida (p. ej. "Un cambio del
  idioma del dispositivo cambia el origen") se prueban con fakes de dominio, pero la integración real con
  `Configuration`/`LocaleList` solo se verifica a mano.
- **Recomendación.** Antes de la próxima feature que dependa de estado del sistema, añadir un test instrumentado
  mínimo por frontera de `:core:platform` (locale, TTS, reconocimiento de voz) detrás de un puerto ya existente, y
  documentar en `tasks.md` cuáles quedan manuales y por qué. `:core:platform` no tiene `src/androidTest`: el primer
  test exige crear el source set (el runner de instrumentación ya está resuelto, ver `docs/deuda-tecnica.md`).
  Recordar el filtro por clase en tests instrumentados (CLAUDE.md, "Comandos").

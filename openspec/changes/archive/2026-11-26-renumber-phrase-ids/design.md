# Design

## Context

Ver `proposal.md` (Why). Estado actual comprobado:

- Los 7 assets `core/data/src/main/assets/catalog/phrases-<locale>.json` tienen 1000 frases cada uno, con los mismos ids
  en el mismo orden. Todos los ids tienen la forma `<nivel>-<tema>-<nnn>`, y su nivel y su tema coinciden siempre con
  `level` y `topic`. `nnn` va por (nivel, tema), empieza en `001`, no tiene huecos y no pasa de `010`. Hay 114 temas,
  todos con `[a-z_]` y de 23 caracteres como mucho.
- `CatalogContract` (`:core:data`, `src/test`) contiene las reglas de permanencia y de paralelismo como funciones puras.
  `CatalogContractTest` las prueba con datos sintéticos y `RealCatalogContractTest` las aplica a los assets reales.
- `released-ids-pt-PT.txt` tiene 2 ids. Los demás `released-ids-*.txt` están vacíos.
- `PhraseId` es un `value class` de `String` sin validación. `TargetPhrase` (`:core:data`) expone `id`, `level: CefrLevel`
  (A1–C1) y `topic: Topic`.
- `catalog.db` se vuelve a sembrar cuando cambia la huella del documento (ADR-0028).

## Goals / Non-Goals

**Goals:**

- Renombrar los ids sin reasignar números y sin cambiar ningún otro campo de los assets.
- Que el contrato de CI rechace cualquier id que no cumpla el formato, el registro de bloques o la lista de temas, y
  cualquier frase nueva cuya etiqueta no coincida con su nivel y su tema.

**Non-Goals:**

- Validar el id en tiempo de ejecución. `CatalogJson` y el dominio no cambian.
- Migrar `practice.db` (A3).

## Decisions

### D1. Contrato como funciones puras en `CatalogContract`, no en producción

Se añaden dos funciones a `CatalogContract` (`core/data/src/test/kotlin/.../catalog/`), en el mismo estilo que
`retiredIds`:

- `idFormatViolations(ids: List<String>, blocks: Set<String>, topics: Set<String>): List<IdViolation>`, con
  `data class IdViolation(val id: String, val reason: IdViolationReason)` y
  `enum class IdViolationReason { MALFORMED, UNKNOWN_BLOCK, UNKNOWN_TOPIC }`. Si el formato no se cumple, la razón es
  `MALFORMED` y no se miran el bloque ni el tema. Devolver la razón permite que cada test compruebe que el id falla
  por el motivo del escenario.
- `birthLabelMismatches(phrases: List<TargetPhrase>, released: Set<String>): List<String>`. Devuelve los ids no
  publicados cuyo nivel (`level.name.lowercase()`) o tema (`topic.value`) no coinciden con los del id. Recibe frases
  ya validadas, con ids que cumplen el formato. Un id que no lo cumple no se compara aquí, porque ya lo detecta
  `idFormatViolations`.

Formato, en una constante privada:
`^([0-9]{4})-(a1|a2|b1|b2|c1)-([a-z]+(?:_[a-z]+)*)-(?!000)([0-9]{3})$`. El lookahead excluye `000`, así que `nnn` va de
`001` a `999`. La lista de niveles es fija en la expresión: coincide con `CefrLevel`, y añadir un nivel exigirá un ADR.

*Alternativa descartada:* validar en `CatalogJson` y rechazar la carga. Convertiría una regla de autoría en un fallo de
arranque para el usuario y metería en producción los registros, que D8 sustituirá.

### D2. Registros como recursos de test

En `core/data/src/test/resources/catalog/`:

- `blocks.txt`: una línea, `0000`.
- `topics.txt`: los 114 temas actuales, uno por línea, en orden alfabético. Se generan con un script a partir de los
  assets.

Se leen igual que los `released-ids` (`readLines().filter(String::isNotBlank)`). Los temas retirados se quedan en la
lista, porque un id publicado puede conservar un tema que ya no se usa. Añadir un bloque o un tema exige editar estos
ficheros, y la edición se ve en el diff de la PR.

### D3. Renombrado con un script de un solo uso, sin commitearlo

Un script de Python (en `$CLAUDE_JOB_DIR/tmp` o equivalente, fuera del repo) sustituye en cada asset el texto de las
líneas `"id": "<id>"` por `"id": "0000-<id>"`, con una expresión regular que exige el formato antiguo. No parsea ni
vuelve a serializar el JSON, para que el diff solo toque esas líneas. Antes de escribir comprueba que hay exactamente
1000 sustituciones por fichero. El mismo script genera `topics.txt`. Los dos ids de `released-ids-pt-PT.txt` se editan
a mano.

La sesión principal no lee los assets enteros (regla de contenido de `CLAUDE.md`). Comprueba el resultado con el
contrato, con `git diff --stat` (7 × 1000 líneas cambiadas) y con un Grep de ids sin bloque, que debe salir vacío.

*Alternativa descartada:* `jq`. Reformatea el documento entero y el diff dejaría de ser revisable.

### D4. Orden: contrato rojo antes que el renombrado

1. Funciones del contrato con sus tests sintéticos (TDD normal).
2. Registros y tests del contrato sobre los assets reales: fallan con los ids antiguos (rojo por `MALFORMED`).
3. Renombrado de los assets y de `released-ids-pt-PT.txt`: el contrato pasa a verde.
4. Ids literales de los demás tests de `:core:data` y `:core:domain`.

Los tests de `:core:data` que usan ids literales construyen sus propios documentos y no leen los assets, así que no se
rompen con el renombrado. Se actualizan por coherencia con las specs y con los comentarios de escenario.

### D5. ADR-0029 y `CLAUDE.md`

ADR-0029 "Formato de los ids de frase": formato, bloques en pasos de 1000 con registro explícito, etiqueta de
nacimiento, agotamiento de `nnn` (exige un ADR) y paquete de la MVP2 deducido del bloque, sin cambios en `practice.db`.
No sustituye a ninguno, porque ningún ADR fija el formato, solo la permanencia (ADR-0013, mantenida por ADR-0019 y
ADR-0028). Se crea como `Propuesto` y se acepta en
la tarea de documentación del cambio, junto con su fila en `docs/adr/README.md`.

## Risks / Trade-offs

- [Una instalación de desarrollo con progreso antiguo falla al componer la tanda ("Some progress belongs to phrases
  missing from the catalog")] → Se documenta en el ADR y en la PR: hay que borrar los datos de la app. Le sigue A3.
- [El script cambia algo más que los ids] → Exige el formato antiguo y 1000 sustituciones exactas. El paralelismo y la
  carga de los 7 assets en `RealCatalogContractTest` y el `diff --stat` lo detectarían.
- [Un tema nuevo se añade al asset y no a `topics.txt`] → Es justo lo que el contrato debe detectar. El mensaje del
  test nombra el id y la razón.

## Migration Plan

Sin migración de datos. `catalog.db` se resiembra por la huella. Para `practice.db`, hay que borrar los datos de la
app en los dispositivos de desarrollo. Para deshacer el cambio, basta con revertir el commit.

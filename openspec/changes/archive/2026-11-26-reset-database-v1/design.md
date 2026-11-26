# Design

## Context

Estado actual en `:core:data`:

| Base | Versión | Esquemas exportados | Fallback destructivo | Contenido |
|---|---|---|---|---|
| `catalog.db` (`CatalogDatabase`) | 4 | `1.json` a `4.json` | sí (`CatalogSchema.kt:17`) | caché sembrada desde assets (ADR-0019, ADR-0028) |
| `practice.db` (`PracticeDatabase`) | 5 | `1.json` a `5.json` | sí (`PracticeSchema.kt:61`) | progreso, tanda activa, contador y ajustes aplicados |

`PracticeMigrationTest` caracteriza la migración destructiva desde v3 y v4. Lo hace con una copia del builder de
producción que también llama a `fallbackToDestructiveMigration()`. El esquema se exporta a `core/data/schemas`
mediante el plugin de convención `PersistenceConventionPlugin` (`schemaDirectory("$projectDir/schemas")`).

Para la motivación, ver proposal.md, apartado "Why".

## Goals / Non-Goals

**Goals:**
- Las dos bases declaran `version = 1` y cada una tiene un solo esquema exportado, `1.json`, igual al esquema
  vigente hoy.
- Si a `practice.db` le falta una migración, Room falla al abrirla. No la recrea vacía.
- Queda escrita la política para las subidas de versión posteriores a 1.0.0 (ADR-0030).

**Non-Goals:**
- No cambia ninguna tabla, columna, índice ni trigger.
- No se añade `room3-testing` ni ningún test de migración: con una sola versión no hay nada que migrar.
- La copia de seguridad (`allowBackup`) se queda como está; es A4.

## Decisions

### D1. Las dos bases vuelven a la versión 1 y se regeneran todos sus esquemas

Se cambia `version` a 1 en `CatalogDatabase` y en `PracticeDatabase`. Se borran **todos** los ficheros de
`core/data/schemas/<base>/`, también el `1.json` antiguo, y se compila `:core:data` para que KSP exporte el `1.json`
nuevo.

- **Por qué borrar también el `1.json` antiguo:** contiene el esquema de la primera versión, no el actual. Borrarlo
  evita depender de si el exportador de Room sobrescribe o no un fichero que ya existe.
- **Cómo se comprueba:** el `identityHash` de Room se calcula a partir de las tablas y no depende del número de
  versión. Por eso el `identityHash` de cada `1.json` nuevo tiene que coincidir con el del último esquema anterior:
  `4.json` del catálogo y `5.json` de práctica. Si no coincide, se ha colado un cambio de esquema no buscado.
- **Alternativa descartada:** dejar las bases en v4 y v5 y quitar solo el fallback. Funciona, pero 1.0.0 saldría con
  cuatro esquemas de práctica que ningún usuario tendrá nunca. Esos esquemas sugerirían migraciones que hay que
  mantener cuando nunca han existido.

### D2. `practice.db` sin `fallbackToDestructiveMigration()`

`createPracticeDatabase` deja de llamar a `fallbackToDestructiveMigration()`. Ante una versión sin camino de migración,
Room lanza una `IllegalStateException` al abrir la base.

- **Por qué:** después de 1.0.0, que la app falle de forma visible es preferible a borrar el progreso del usuario sin
  avisar. Los tests de migración que exige el ADR-0030 evitan que llegue a producción una subida sin su migración.
- **Alternativa descartada:** `fallbackToDestructiveMigrationOnDowngrade()`. Una bajada de versión solo ocurre en
  desarrollo, y abriría otra vía de pérdida de datos.
- **Tests:** no hay comportamiento nuevo que llevar a rojo, porque solo se quita una configuración. Ningún test
  unitario pasa por `createPracticeDatabase`: los de `:core:data` usan bases en memoria o en `@TempDir`.
  `PersistenceWiringTest` (`:app`, instrumentado) sí la usa a través de Hilt, pero solo compara referencias y no abre
  la base. Los tests de Room que ya existen comprueban que el esquema v1 se crea y funciona.

### D3. `catalog.db` mantiene `fallbackToDestructiveMigration()`

Solo cambia su comentario. Deja de justificarse por ser "pre-1.0.0" y pasa a explicar que es una caché que se vuelve
a sembrar desde los assets cuando cambia la huella de su contenido (ADR-0028), y que se cita en el ADR-0030. Recrearla
no pierde nada del usuario.

**Alternativa descartada:** exigir también migraciones en el catálogo. No aporta nada: la siembra rellena las tablas
igualmente.

### D4. ADR-0030, "Política de migraciones de las bases Room"

Se escribe ya, en estado `Propuesto`. Se acepta en el grupo de documentación del apply, como se hizo con el ADR-0029.

- **No sustituye a ningún ADR.** Los ADR 0017, 0019, 0022, 0023 y 0027 aplicaron migraciones destructivas como
  consecuencia de estar en pre-1.0.0. Esa era su situación, no una regla para el futuro, y se quedan como historia.
- **La política retoma lo que ya fijaba el ADR-0013** ("versión inicial 1, sin `fallbackToDestructiveMigration`") y
  añade cómo se sube de versión.

### D5. KDoc y comentarios

- El KDoc de `PracticeDatabase` deja de enumerar las migraciones de v1 a v5. Pasa a citar ADR-0030, que retoma la
  versión inicial 1 que fijó el ADR-0013 (ya sustituido), e indica que la versión 1 es la de 1.0.0.
- El KDoc de `PracticeSchema.kt` (líneas 15-17) deja de decir "Desde `PracticeDatabase` v4". Pasa a decir: "La PK
  `target_locale` de `active_batch` garantiza como máximo una fila por idioma destino, sin trigger de singleton
  (design "PracticeDatabase v4" de `add-multi-language-catalog`)".
- El comentario de `RoomPracticeDataSourceReadTest.kt:129` deja de citar el fallback como precedente. Remite al
  design "Contador de la tanda completada" y al esquema v1.

## Estrategia de test

| Elemento | Cómo se verifica |
|---|---|
| Versiones a 1 y esquemas regenerados | Solo existe `1.json` por base, con `"version": 1`, y su `identityHash` coincide con el del último esquema anterior (D1) |
| `practice.db` sin fallback | Revisión del diff: ninguna llamada a `fallbackToDestructiveMigration` en `PracticeSchema.kt`; Grep en `core/data/src/main` |
| Funcionamiento de las dos bases en v1 | Los tests de Room que ya existen en `:core:data` (`:core:data:check`) |
| Escenario eliminado | Ya no existe `PracticeMigrationTest` ni ningún test que cite "Una base anterior se abre vacía" |

No hacen falta fakes nuevos.

## Risks / Trade-offs

- **[Riesgo]** Los dispositivos y emuladores de desarrollo tienen `practice.db` con `user_version = 5`. Con la base en
  v1 y sin fallback, la app falla al abrirla. → **Mitigación:** desinstalar la app o borrar sus datos tras el merge.
  Ya lo avisa `docs/plan-mvp.md` y lo repite el apartado "Migration Plan".
- **[Riesgo]** Que se cuele un cambio de esquema al regenerar. → **Mitigación:** comparar los `identityHash` (D1).
- **[Trade-off]** Hasta la primera migración real, nada automático impide volver a añadir
  `fallbackToDestructiveMigration()` a `practice.db`. → Lo vigilan el ADR-0030, una entrada de "No hacer" en
  `CLAUDE.md` y la revisión de código.

## Migration Plan

1. Tras el merge en `develop`, en cada dispositivo o emulador de desarrollo: desinstalar la app
   (`adb uninstall <applicationId>`; en debug lleva el sufijo `.debug`) o borrar sus datos desde Ajustes. Hay que
   hacerlo también antes de ejecutar los tests instrumentados de `:app` en ese dispositivo, porque
   `FraseAFraseAppTest` abriría una `practice.db` de una versión anterior.
2. No hay vuelta atrás que preservar datos. Volver a una rama anterior exige de nuevo borrar los datos.

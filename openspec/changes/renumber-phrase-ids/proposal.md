# Proposal

## Why

Los ids del catálogo (`a1-saludos-001`) no dicen a qué paquete de frases pertenecen, y ningún test fija su formato.
Antes de publicar la 1.0.0 y de ampliar el catálogo, hay que fijar un formato que admita los paquetes de la MVP2 sin
volver a renombrar. Después de la publicación, los ids ya no podrán cambiar (tarea A2 de `docs/plan-mvp.md`).

## What Changes

- **BREAKING (datos locales):** los 7 × 1000 ids de los assets pasan de `<nivel>-<tema>-<nnn>` a
  `<bloque>-<nivel>-<tema>-<nnn>`, anteponiendo el bloque base `0000-` (`0000-a1-saludos-001`). No se reasigna ningún
  número. `catalog.db` se resiembra sola por la huella (ADR-0028); el progreso de `practice.db` queda con los ids
  antiguos y hay que borrar los datos de la app o desinstalarla (le sigue A3, `reset-database-v1`).
- Formato del id: `^[0-9]{4}-(a1|a2|b1|b2|c1)-[a-z]+(_[a-z]+)*-[0-9]{3}$`, con `nnn` entre `001` y `999`.
- **Etiqueta de nacimiento:** el nivel y el tema del id indican dónde nació la frase. Para filtrar y mostrar se usan
  los campos `level` y `topic`. Una frase publicada puede reclasificarse sin cambiar su id.
- **Bloque = paquete**, en pasos de 1000 (`0000` base, `1000`, `2000`…), validado contra un registro explícito de
  bloques y no con una regla aritmética.
- Test de contrato nuevo en `:core:data` con dos registros como recursos de test (`blocks.txt` con `0000`,
  `topics.txt` con los 114 temas actuales):
  - en todos los ids: formato, bloque registrado y tema de la lista;
  - en las frases no publicadas: el nivel y el tema del id coinciden con `level` y `topic`.
- `released-ids-pt-PT.txt` (2 ids) y los ids literales de los tests y de 4 escenarios pasan al formato nuevo. No hay
  ninguna versión publicada, así que no se rompe la permanencia de ids.
- ADR-0029 nuevo: formato del id, registro de bloques, etiqueta de nacimiento y relación con los paquetes de la MVP2.
  No sustituye a ninguno: ningún ADR fija el formato, solo la permanencia (ADR-0013, mantenida por ADR-0019 y
  ADR-0028), que no cambia.
- `CLAUDE.md`: en la regla de oro 4, "Ids de frases: ADR-0029"; en "No hacer", la regla de ids cita el formato, el
  registro de bloques y la lista de temas.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `phrase-catalog`: se añaden "Formato de los ids" y "Nivel y tema del id en las frases no publicadas". Los
  escenarios que citan `a1-saludos-001` pasan a `0000-a1-saludos-001` en "Texto de origen desde el documento de
  frases del idioma de origen", "Ids únicos y no vacíos por documento de frases" y "Persistencia y resembrado del
  catálogo en Room".
- `practice-persistence`: el escenario con `a1-saludos-001` de "Persistencia del progreso por frase" pasa a
  `0000-a1-saludos-001`.

## Impact

- ADR afectados: ADR-0029 (nuevo). Permanencia de ids (ADR-0013, mantenida por ADR-0019 y ADR-0028) y resembrado por huella (ADR-0028): sin
  cambios. El cambio respeta el alcance del MVP (ADR-0018): no añade ni quita comportamiento de práctica.
- `:core:data`: los 7 `phrases-*.json` (solo el campo `id`), `released-ids-pt-PT.txt`, `CatalogContract`,
  `CatalogContractTest`, `RealCatalogContractTest` y los tests con ids literales (`CatalogDaoTest`,
  `CatalogDatabaseTest`, `CatalogJsonTest`, `RoomCatalogDataSourceTest`, `RoomPracticeDataSourceReadTest`).
- `:core:domain`: solo tests (`CatalogPhraseTest`, `PracticeRepositoryTest`). `PhraseId` no cambia.
- Código de producción: ninguno. Sin librerías nuevas.
- Documentación: `docs/adr/0029-*.md`, `docs/adr/README.md`, `CLAUDE.md`.

## Fuera de alcance

- Vaciar o migrar `practice.db` (A3, `reset-database-v1`).
- Rellenar los `released-ids-*.txt` con todos los ids (A11, `freeze-released-ids`).
- Validar el formato del id en tiempo de ejecución, al cargar el catálogo: es una regla de autoría y la vigila el
  contrato en CI.
- Registro de categorías y subcategorías en producción, y fusión de temas (D8).
- Paquetes de pago y bloques distintos de `0000`.

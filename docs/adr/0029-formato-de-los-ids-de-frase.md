# ADR-0029: Formato de los ids de frase

- **Estado:** Aceptado
- **Fecha:** 2026-11-26

## Contexto

ADR-0013 hace permanentes los ids de las frases, y ADR-0019 y ADR-0028 mantienen esa regla: no se reutilizan, no se
borran y las frases nuevas van al final. Ninguno fija su forma. Hoy todos siguen `<nivel>-<tema>-<nnn>` (`a1-saludos-001`) por costumbre, sin que ningún
test lo compruebe.

La MVP2 prevé paquetes de frases (de pago o temáticos) dentro del mismo documento de cada idioma. Con el formato actual,
el id no dice a qué paquete pertenece una frase, y dos paquetes con el mismo nivel y tema competirían por los mismos
números. Además, el contenido se reorganizará (fusión de temas, nivel corregido), y un id que dependa de la
clasificación actual tendría que cambiar, lo que la permanencia prohíbe.

Todavía no hay ninguna versión publicada, así que es la última ocasión de cambiar los ids sin romper la permanencia.

## Decisión

**Formato.** Un id de frase tiene la forma `<bloque>-<nivel>-<tema>-<nnn>`:

- `bloque`: 4 dígitos.
- `nivel`: `a1`, `a2`, `b1`, `b2` o `c1`.
- `tema`: letras minúsculas, con palabras separadas por `_`.
- `nnn`: 3 dígitos, de `001` a `999`, que se numeran por (bloque, nivel, tema).

Expresión: `^[0-9]{4}-(a1|a2|b1|b2|c1)-[a-z]+(_[a-z]+)*-[0-9]{3}$`, excluyendo `nnn` igual a `000`.

**Bloques.** Un bloque identifica un paquete y va en pasos de 1000: `0000` es el catálogo base, y `1000`, `2000`… serán
los paquetes. Los bloques válidos están en un registro explícito, versionado en el repositorio. No hay una regla
aritmética: si un paquete necesita un bloque de ampliación (`1001`), se añade al registro sin cambiar el formato. Un
paquete crece dentro de su bloque, porque `nnn` se numera por (bloque, nivel, tema).

**Etiqueta de nacimiento.** El nivel y el tema del id indican dónde nació la frase, no su clasificación vigente. Para
filtrar y mostrar se usan los campos `level` y `topic`. Una frase publicada puede reclasificarse sin cambiar su id.
Antes de publicarla, su id debe coincidir con su `level` y su `topic`.

**Temas.** Los temas válidos están en una lista cerrada, versionada en el repositorio. Incluye los temas retirados,
porque un id publicado puede conservarlos.

**Contrato.** Un test de contrato comprueba en CI, en todos los documentos de frases:

- en todos los ids: el formato, el bloque registrado y el tema de la lista;
- en los ids no publicados: el nivel y el tema del id coinciden con `level` y `topic`.

Agotar `nnn` en un (bloque, nivel, tema) exige un ADR nuevo.

**Paquetes.** El paquete de una frase se deduce de su bloque. El progreso guardado no cambia: sigue identificando
la frase por su id.

**Renombrado inicial.** Los ids actuales pasan al bloque `0000` anteponiendo `0000-`, sin reasignar números.

## Consecuencias

- Positivas:
  - Un paquete nuevo se añade sin tocar los ids existentes ni el esquema de las bases de datos.
  - Reclasificar una frase (fusionar temas o corregir su nivel) no cambia su id.
  - Cualquier id mal formado, con un bloque o un tema sin registrar, o con una etiqueta que no coincide antes de
    publicarse, falla en CI.
- Negativas:
  - Una frase reclasificada tiene un id que ya no coincide con su nivel o su tema. Hay que leer los campos, no el id.
  - El progreso guardado con los ids antiguos queda huérfano. Las instalaciones de desarrollo deben borrar los datos
    de la app.
  - Añadir un bloque o un tema exige editar el registro correspondiente.
- Descartadas:
  - Ids opacos (UUID o número secuencial): pierden la legibilidad del id en los diffs y en los tests.
  - Bloque deducido aritméticamente (`id / 1000`): no admite bloques de ampliación sin cambiar la regla.
  - Id que siga a la clasificación vigente: cada reclasificación rompería la permanencia.
  - Validar el formato al cargar el catálogo en la app: es una regla de autoría, y un fallo así dejaría al usuario sin
    catálogo.

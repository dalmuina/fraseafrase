# Tasks

No hay tareas de dominio/datos/presentación: este cambio solo añade contenido al catálogo JSON ya
existente (`core/data/src/main/assets/catalog/phrases.json`), sin tocar código. Cada grupo añade las
frases de un nivel al final de la lista `phrases`, en orden ascendente de nivel, y verifica que el
catálogo sigue siendo válido.

## 1. Nivel A1

- [x] 1.1 Añadir ≈30 frases de nivel A1 (incluyendo las 2 ya existentes, que no se tocan) repartidas
      en 6-8 temas variados, con `id`, `level`, `topic`, `target` (pt-PT) y `source` (es-ES) únicos y
      no vacíos; verificado con `./gradlew :core:data:test --rerun-tasks` en verde.

## 2. Nivel A2

- [x] 2.1 Añadir ≈30 frases de nivel A2 al final de la lista, repartidas en 6-8 temas variados;
      verificado con `./gradlew :core:data:test --rerun-tasks` en verde.

## 3. Nivel B1

- [x] 3.1 Añadir ≈30 frases de nivel B1 al final de la lista, repartidas en 6-8 temas variados;
      verificado con `./gradlew :core:data:test --rerun-tasks` en verde.

## 4. Nivel B2

- [x] 4.1 Añadir ≈30 frases de nivel B2 al final de la lista, repartidas en 6-8 temas variados;
      verificado con `./gradlew :core:data:test --rerun-tasks` en verde.

## 5. Nivel C1

- [x] 5.1 Añadir ≈30 frases de nivel C1 al final de la lista, repartidas en 6-8 temas variados;
      verificado con `./gradlew :core:data:test --rerun-tasks` en verde.

## 6. Verificación del contrato del catálogo

- [x] 6.1 Comprobado (vía `RealCatalogContractTest` en `./gradlew :core:data:test`) que los ids
      liberados (`a1-saludos-001`, `a1-comida-001`) siguen siendo un prefijo exacto de la lista de
      ids del catálogo ampliado (150 frases, 30 por nivel A1-C1, ids únicos).
- [x] 6.2 Ejecutado `./gradlew check`: tests, detekt, lint y cobertura Kover en verde con el
      catálogo ampliado (BUILD SUCCESSFUL).

## Workflow follow-up

- Revisión de `android-architect` (una ronda, cambio ligero sin lógica) antes de dar por aprobada
  la propuesta.
- Revisión de `code-reviewer` (una ronda) sobre el contenido y formato del catálogo ampliado.
- `/opsx:verify` y `/opsx:archive` en la rama `1.0.0/expand-phrase-catalog`, antes de abrir la PR.
- El usuario crea la rama, hace push y abre la PR a `develop`.

# ADR-0030: Política de migraciones de las bases Room

- **Estado:** Aceptado
- **Fecha:** 2026-11-26

## Contexto

ADR-0013 fijó que el estado del usuario empezaría en la versión 1 de Room y sin `fallbackToDestructiveMigration`.
Durante el desarrollo previo a 1.0.0 no fue así. Los ADR 0017, 0019, 0022, 0023 y 0027 subieron las versiones con
migraciones destructivas porque no había usuarios. Así, `PracticeDatabase` llegó a la versión 5 y `CatalogDatabase`
a la 4, con esquemas intermedios que ninguna instalación publicada tendrá nunca.

A partir de 1.0.0, `practice.db` guarda el progreso real de los usuarios. Si una subida de versión con migración
destructiva se publica, ese progreso se borra sin avisar. `catalog.db`, en cambio, es una caché que se vuelve a
sembrar desde los assets cuando cambia la huella de su contenido (ADR-0028).

Este ADR no sustituye a ninguno. Los ADR 0017, 0019, 0022, 0023 y 0027 siguen vigentes, o están sustituidos, por sus
decisiones principales. Sus migraciones destructivas fueron una consecuencia de la etapa previa a 1.0.0.

## Decisión

- Antes de publicar 1.0.0, `PracticeDatabase` y `CatalogDatabase` vuelven a la versión 1. Cada una conserva un único
  esquema exportado, `1.json`, que es el esquema vigente.
- **`practice.db` nunca migra de forma destructiva.** No usa `fallbackToDestructiveMigration()` ni ninguna de sus
  variantes. Si falta una migración, la apertura falla: es preferible a perder el progreso.
- **Cada subida de versión de `practice.db`:**
  - incrementa `version` en uno y exporta el nuevo esquema junto a los anteriores, que no se borran;
  - declara una `@AutoMigration` cuando Room puede deducirla. Si Room no puede, se escribe una `Migration` manual,
    o se usa una `AutoMigrationSpec` para renombrados y borrados;
  - lleva un test con `MigrationTestHelper` sobre los esquemas exportados. El test parte de una base con datos de la
    versión anterior y comprueba que el progreso se conserva.
- La primera subida de versión añade `room3-testing` y monta esa infraestructura de test. Hasta entonces no hace falta.
- **`catalog.db` puede recrearse.** Mantiene `fallbackToDestructiveMigration()`, porque recrearla y volver a sembrarla
  no pierde nada del usuario. Aun así, sube de versión siempre que cambie su esquema y exporta el esquema nuevo.

## Consecuencias

- **Positivas:**
  - 1.0.0 sale con un historial de esquemas limpio.
  - Una migración que falte se ve como un error, en lugar de convertirse en una pérdida de datos silenciosa.
  - Cada cambio de esquema de `practice.db` queda probado con datos reales de la versión anterior.
- **Negativas:**
  - Las instalaciones de desarrollo con una base de versión superior a 1 tienen que borrar sus datos.
  - Cambiar el esquema de `practice.db` cuesta más: hace falta una migración y su test.
- **Descartado:**
  - Mantener las versiones 4 y 5 y quitar solo el fallback.
  - `fallbackToDestructiveMigrationOnDowngrade()` en `practice.db`.
  - Exigir migraciones también en `catalog.db`.

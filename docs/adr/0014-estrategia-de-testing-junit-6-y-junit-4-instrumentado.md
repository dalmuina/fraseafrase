# ADR-0014: Estrategia de testing con JUnit 6 y JUnit 4 en instrumentados

- **Estado:** Aceptado
- **Fecha:** 2026-11-01
- **Sustituye a:** ADR-0007

## Contexto
El código lo genera mayoritariamente una IA. Los tests escritos *después* tienden a confirmar lo que
el código ya hace en lugar de lo que debería hacer. Los escenarios de OpenSpec deben convertirse en
comprobaciones ejecutables.

ADR-0007 fijó TDD obligatorio y la pirámide de tests, y nombró JUnit 5. El proyecto usa JUnit 6.1.3
(`junit = "6.1.3"` en `gradle/libs.versions.toml`), que mantiene la API de Jupiter y el mismo modelo de
programación. Además, los tests instrumentados de `:app` necesitan `AndroidJUnitRunner`, `createComposeRule`
(ya usado en `feature/session`) y `HiltAndroidRule`, que exigen JUnit 4. ADR-0007 no contemplaba esa
excepción ni declaraba el cambio de versión.

## Decisión
**TDD obligatorio** para todo código de producción: rojo → verde → refactor.

1. Cada escenario *GIVEN / WHEN / THEN* de la especificación se traduce primero en un test que **falla**.
2. Se escribe el código mínimo para que pase.
3. Se refactoriza manteniendo los tests en verde.
4. En `tasks.md`, cada tarea de implementación va precedida de su tarea de test.

**Excepción acotada de DI:** el cableado de Hilt ya existente se verifica con una mutación (prueba de
rojo: se quita la anotación y el test debe fallar por aserción). Esta excepción no aplica a código nuevo
de producción.

**Pirámide y herramientas:**

| Nivel | Módulos | Herramientas |
|---|---|---|
| Unitario (JVM) | `:core:domain`, ViewModels, mapeadores | JUnit 6.1.3 (Jupiter), Kotest assertions, MockK (solo en fronteras), Turbine, `kotlinx-coroutines-test` |
| Integración | `:core:data` | Room in-memory, tests instrumentados o Robolectric |
| UI | `:feature:*` | Compose UI Test sobre `XxxScreen` sin estado |
| Instrumentado | `:app` (en este cambio) | JUnit 4, `AndroidJUnitRunner`, `HiltAndroidRule` |

Los tests JVM siguen con JUnit 6 (Jupiter). Solo los tests instrumentados usan JUnit 4.

**Normas:**

- Preferir **fakes** (en `:core:testing`) sobre mocks; los mocks solo para verificar interacciones con fronteras externas.
- Nombres de test descriptivos con comillas invertidas: `` `given a failed phrase when building next session then it is included` ``.
  **Excepción en tests instrumentados (`androidTest`):** `givenX_whenY_thenZ` en camelCase con guiones bajos. Con minSdk 26
  el DEX no admite espacios en los nombres de método (fallo real al compilar `:app` con tests de Hilt). Es la forma que ya
  usan los tests de UI de `:feature:session`.
- Estructura *Arrange / Act / Assert* (o *Given / When / Then*), una idea por test.
- Sin `Thread.sleep` ni dependencias de la fecha real: se inyectan `Clock` y dispatchers de test.

**Cobertura mínima (Kover), verificada en CI:**

- `:core:domain`: 90 % de líneas.
- ViewModels de `:feature:*`: 80 % de líneas.
- `:core:data`: 80 % de líneas (ADR-0013).
- Global: 70 % de líneas.

Los tests instrumentados no cuentan para Kover.

**Calidad estática:** detekt 1.23.8 con `detekt-formatting` bloquea el merge si falla (ADR-0011). No se usa ktlint por separado.

## Consecuencias
- **Positivas:** los tests documentan el comportamiento esperado; refactorizar es seguro; `/opsx:verify` tiene evidencias objetivas; la versión de JUnit y la excepción de instrumentados quedan registradas.
- **Negativas:** desarrollo inicial más lento. Hay dos versiones de JUnit en el proyecto (6 en JVM, 4 en instrumentados), que hay que recordar al escribir tests.
- **Descartado:** tests escritos después del código; perseguir el 100 % de cobertura; usar JUnit 5 en lugar de JUnit 6, porque el proyecto ya usa 6.1.3.

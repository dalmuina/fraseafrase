# ADR-0007: Estrategia de testing y TDD obligatorio

- **Estado:** Sustituido por ADR-0014
- **Fecha:** 2026-10-29

## Contexto
El código lo genera mayoritariamente una IA. Los tests escritos *después* tienden a confirmar lo que
el código ya hace en lugar de lo que debería hacer. Los escenarios de OpenSpec deben convertirse en
comprobaciones ejecutables.

## Decisión
**TDD obligatorio** para todo código de producción: rojo → verde → refactor.

1. Cada escenario *GIVEN / WHEN / THEN* de la especificación se traduce primero en un test que **falla**.
2. Se escribe el código mínimo para que pase.
3. Se refactoriza manteniendo los tests en verde.
4. En `tasks.md`, cada tarea de implementación va precedida de su tarea de test.

**Pirámide y herramientas:**

| Nivel | Módulos | Herramientas |
|---|---|---|
| Unitario (JVM) | `:core:domain`, ViewModels, mapeadores | JUnit 5, Kotest assertions, MockK (solo en fronteras), Turbine, `kotlinx-coroutines-test` |
| Integración | `:core:data` | Room in-memory, tests instrumentados o Robolectric |
| UI | `:feature:*` | Compose UI Test sobre `XxxScreen` sin estado |

**Normas:**

- Preferir **fakes** (en `:core:testing`) sobre mocks; los mocks solo para verificar interacciones con fronteras externas.
- Nombres de test descriptivos con comillas invertidas: `` `given a failed phrase when building next session then it is included` ``.
- Estructura *Arrange / Act / Assert* (o *Given / When / Then*), una idea por test.
- Sin `Thread.sleep` ni dependencias de la fecha real: se inyectan `Clock` y dispatchers de test.

**Cobertura mínima (Kover), verificada en CI:**

- `:core:domain`: 90 % de líneas.
- ViewModels de `:feature:*`: 80 % de líneas.
- Global: 70 % de líneas.

**Calidad estática:** ktlint (formato) y detekt (complejidad, olores de código) bloquean el merge si fallan.

## Consecuencias
- **Positivas:** los tests documentan el comportamiento esperado; refactorizar es seguro; `/opsx:verify` tiene evidencias objetivas.
- **Negativas:** desarrollo inicial más lento.
- **Descartado:** tests escritos después del código; perseguir el 100 % de cobertura.

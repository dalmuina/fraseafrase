# Design

## Context

El cableado de Hilt vive en `:app` (`di/PersistenceModule.kt`). Liga `CatalogRepository` a `AssetsCatalogDataSource`
(`@Singleton`, con memoización) y expone `PracticeDatabase` con un `@Provides @Singleton`. `RoomPracticeDataSource` y
`DataStoreSettingsDataSource` se ligan sin ámbito. `DataStore` no es un binding: el delegado
`preferencesDataStore` lo crea una vez por proceso para cada nombre de fichero.

El convention plugin `fraseafrase.android.*` fija `AndroidJUnitRunner` en el `defaultConfig` de todos los módulos Android
y añade `androidx.test:runner` a `androidTestImplementation`; ese arreglo ya está hecho (`fix-instrumented-test-runner`).
El plugin `fraseafrase.hilt` aplica KSP y añade `hilt-compiler` a `ksp`, pero no a `kspAndroidTest`. `feature/session` ya
tiene tests instrumentados en JUnit 4 (`createComposeRule`). La CI no ejecuta tests instrumentados. El análisis estático
del proyecto es detekt 1.23.8 con `detekt-formatting` (ADR-0011), no ktlint por separado. Ver proposal.md, sección Why.

## Goals / Non-Goals

**Goals:**
- Dos requisitos verificables (ver `specs/`) que fallen si se quita `@Singleton` del catálogo o de la base de datos.
- Ubicación y configuración limitadas a `:app`, para no tocar el resto de módulos.

**Non-Goals:**
- Probar DataStore por identidad o por comportamiento (ver D4).
- Cambiar el convention plugin ni el runner de otros módulos (deuda nueva, ver D7).
- Ejecutar los tests en CI (siguiente cambio de infraestructura, ver D7).
- Incluir `src/androidTest` en detekt en este cambio (ver D7).

## Decisions

### D1. Ubicación: `:app/src/androidTest`

`:app` es el único módulo que conoce `PersistenceModule` y todas las implementaciones (ADR-0010). Un test en
`:core:data` no vería el grafo real. **Alternativa descartada:** `:core:data/src/androidTest` con un módulo Hilt de
prueba propio; duplicaría el cableado que se quiere verificar.

Ruta del runner: `app/src/androidTest/kotlin/com/dalmuina/fraseafrase/HiltTestRunner.kt`, con la FQCN
`com.dalmuina.fraseafrase.HiltTestRunner` en `testInstrumentationRunner`. El proyecto usa `kotlin/`, no `java/`.

### D2. Runner propio de Hilt solo en `:app`

Hilt exige un `AndroidJUnitRunner` que devuelva `HiltTestApplication` (documentación de Hilt, instrumentation testing).
Se crea `HiltTestRunner` y `:app` sobrescribe `testInstrumentationRunner` en su `defaultConfig`. El convention plugin no
cambia: el build script de `:app` se evalúa después de sus plugins, así que la sobrescritura prevalece.

**Alternativa descartada:** cambiar el runner en `KotlinAndroid.kt` para todos los módulos. Afectaría a módulos que no
necesitan Hilt y no es necesario para este cambio.

### D3. Identidad con `assertSame` y base de datos real

El test inyecta `CatalogRepository` dos veces y `PracticeDatabase` dos veces, con `@Inject lateinit var` y
`hiltRule.inject()`, y comprueba identidad con `org.junit.Assert.assertSame` (JUnit 4, ya disponible en `androidTest`, sin
dependencia nueva). Usa los `@Provides` y `@Binds` reales de `PersistenceModule`.

El test solo inyecta y compara referencias. No ejecuta consultas, no usa DAOs y no toca el contenido de la base de datos.
Room abre el fichero en la primera consulta, así que el test no lo abre.

**Alternativa descartada:** `@TestInstallIn` con base de datos en memoria. Probaría un cableado distinto del de producción.

Naturaleza del test: es de integración instrumentada. No es unitario ni de UI. Su cobertura no cuenta para Kover, porque
`koverVerify` no mide `androidTest`. No hay fakes: el test usa el grafo real de `PersistenceModule` y la base de datos real.

Alcance de "en el grafo de la aplicación": cada test de Hilt crea su propio `SingletonComponent`, así que el test verifica
el ámbito dentro de un componente. Que ese ámbito sea el proceso depende del ciclo de vida de `Application`, y el test no lo
comprueba directamente. Los casos límite de la app (primer uso, catálogo agotado, cambio de día, TTS) no aplican a este
cableado.

Nombres de test en inglés. En los tests JVM, con comillas invertidas según CLAUDE.md. **Excepción en los tests
instrumentados (`androidTest`):** se usa `givenX_whenY_thenZ` en camelCase con guiones bajos, porque con minSdk 26 el DEX
no admite espacios en los nombres de método (fallo real de `:app:dexBuilderDebugAndroidTest`). Es la misma forma que
usan los tests de UI de `:feature:session`. Ejemplo:
`givenProductionGraph_whenInjectingCatalogTwice_thenBothReferencesAreTheSameInstance`. Cada test lleva en un comentario
el escenario de spec que cubre.

### D4. DataStore fuera de la prueba de identidad

`DataStore` no es un binding de Hilt, así que un `assertSame` no puede comprobarla. La garantía de una sola instancia por
fichero la da la declaración única del delegado `preferencesDataStore`.

Un test de comportamiento con dos `DataStoreSettingsDataSource` pasaría siempre: ambas comparten el mismo delegado. El
fallo real sería declarar dos veces `preferencesDataStore` para el mismo fichero, que según la documentación de DataStore
lanza `IllegalStateException` al primer acceso (verificar en la tarea 3.2). Solo lo cubre una regla Konsist nueva (una única
declaración de `preferencesDataStore` por nombre de fichero), que se registra en la deuda técnica para un cambio aparte.

### D5. Procedimiento de test nacido verde

Como `@Singleton` ya existe, los tests nacen verdes. Esta es una excepción justificada a la regla 2 de CLAUDE.md (rojo
primero): el cableado ya existe, y la prueba de rojo es la mutación del cableado, no un test sin implementación. La
excepción no aplica a código nuevo de producción. La excepción la autoriza la aprobación de este cambio, no ADR-0014, que
sigue en `Propuesto` hasta la tarea 5.2. El grupo 2 se ejecuta bajo esa aprobación. El procedimiento, en apply:

1. Escribir los tests y ejecutarlos: en verde con el cableado real.
2. Quitar `@Singleton` temporalmente de `AssetsCatalogDataSource` y ejecutar: el test de `CatalogRepository` debe fallar
   por aserción, con el mensaje de `assertSame` de JUnit (`expected same:<...> was not:<...>`, que contiene
   `was not`). No debe fallar por compilación. Si el mensaje real difiere, parar y reportar: no se ajusta la cadena
   esperada. Restaurar.
3. Quitar `@Singleton` del `@Provides` de `PracticeDatabase` y ejecutar: el test de la base de datos debe fallar con la
   misma aserción. Restaurar.
4. Restaurar ambas anotaciones y ejecutar con `--rerun`: verde.

Los pasos 2 y 3 se hacen por separado para comprobar que cada test protege lo suyo. Las mutaciones se revierten antes de
cualquier commit o `/clear`: mientras una mutación esté aplicada, el estado es ⛔. Al terminar, el árbol de trabajo debe
quedar igual que antes de la mutación (`git diff` sin cambios en `core/data/` ni en `app/src/main/`). La evidencia no se
commitea: el commit del grupo contiene solo el código final en verde.

### D6. Dependencias y versiones

- `hilt-android-testing` con `version.ref = "hilt"` (2.60.1), en `gradle/libs.versions.toml`, en
  `androidTestImplementation` de `:app`. Es la librería oficial de Hilt para tests instrumentados. **Alternativa
  descartada:** reimplementar a mano el componente de prueba con `HiltTestApplication` propio; duplicaría lógica de
  Hilt sin ganar cobertura.
- `hilt-compiler` en `kspAndroidTest` de `:app`, con la misma versión. KSP 2.3.11 ya está en el catálogo.
- Justificación de ambas en esta sección, según CLAUDE.md. Ninguna otra librería nueva.

### D7. JUnit 4 en tests instrumentados (ADR-0014)

**Estado del ADR-0014.** Se crea en estado `Propuesto` en la tarea 0.1, antes del grupo 1, con cabecera
`Sustituye a: ADR-0007`, y se edita libremente hasta la tarea 5.2. Esa tarea lo pasa a `Aceptado` y pasa ADR-0007 a
`Sustituido por ADR-0014`, después de la revisión de código y de `/opsx:verify` (tarea 5.1), y antes de `/opsx:archive`.
Así una corrección de la revisión no deja un ADR aceptado a medias.

**Contenido.** ADR-0014 sustituye por completo a ADR-0007. Sus secciones siguen la plantilla de `docs/adr/README.md`:
Estado, Fecha, Contexto, Decisión y Consecuencias. Recoge línea a línea lo vigente de ADR-0007:

- **Contexto:** el código lo genera mayoritariamente una IA; los tests escritos después tienden a confirmar lo que el
  código ya hace; los escenarios de OpenSpec deben convertirse en comprobaciones ejecutables.
- **Decisión, TDD obligatorio:** rojo, verde, refactor. Cada escenario GIVEN/WHEN/THEN se traduce primero en un test que
  falla; se escribe el código mínimo; se refactoriza con los tests en verde. En `tasks.md`, cada tarea de implementación
  va precedida de su tarea de test. Excepción acotada de DI: el cableado ya existente se verifica con mutación (prueba de
  rojo), no aplicable a código nuevo de producción.
- **Pirámide y herramientas:**
  - Unitario JVM (`:core:domain`, ViewModels, mapeadores): JUnit 6 Jupiter, Kotest assertions, MockK solo en fronteras,
    Turbine, `kotlinx-coroutines-test`.
  - Integración (`:core:data`): Room en memoria, o tests instrumentados o Robolectric.
  - UI (`:feature:*`): Compose UI Test sobre `XxxScreen` sin estado.
  - Instrumentados (`:app` en este cambio): JUnit 4, por las razones de abajo.
- **Normas:**
  - Preferir fakes en `:core:testing` sobre mocks; los mocks solo para verificar interacciones con fronteras externas.
  - Nombres de test descriptivos con comillas invertidas.
  - Estructura Arrange / Act / Assert (o Given / When / Then), una idea por test.
  - Sin `Thread.sleep` ni dependencias de la fecha real: se inyectan `Clock` y dispatchers de test.
- **Cobertura Kover, verificada en CI:** `:core:domain` 90 %, ViewModels de `:feature:*` 80 %, global 70 %. Para
  `:core:data`, 80 % según ADR-0013.
- **Calidad estática:** detekt 1.23.8 con `detekt-formatting` bloquea el merge si falla (ADR-0011). No se usa ktlint por
  separado.
- **Consecuencias:** positivas (los tests documentan el comportamiento, refactorizar es seguro, `/opsx:verify` tiene
  evidencias objetivas); negativas (desarrollo inicial más lento); descartado (tests escritos después del código, y
  perseguir el 100 % de cobertura).

**Cambio de versión declarado.** ADR-0007 nombra JUnit 5. El proyecto usa JUnit 6.1.3 (`junit = "6.1.3"` en
`gradle/libs.versions.toml`), que mantiene la API de Jupiter y el mismo modelo de programación. ADR-0014 lo formaliza.

**JUnit 4 para instrumentados.** `AndroidJUnitRunner`, `createComposeRule` (ya usado en `feature/session`) y
`HiltAndroidRule` lo requieren. Los tests JVM siguen con JUnit 6 (Jupiter).

**Deuda registrada en este cambio** (`docs/deuda-tecnica.md`):
- La entrada de Hilt cambia su título a "Tests de cableado de Hilt: misma instancia de `CatalogRepository` y
  `PracticeDatabase`", marca su disparador como resuelto en este cambio y deja de pedir identidad de DataStore (D4).
  Se añade la regla Konsist futura de DataStore (nueva, sin implementar: una única declaración de `preferencesDataStore`
  por nombre de fichero). La regla futura sobre data sources con estado, ya registrada en `deuda-tecnica.md` (línea 20)
  y sin implementar, se mantiene con esta definición: un data source está *con estado* si guarda en sus propios campos
  una caché o un valor mutable en memoria. Clasificación actual, leída de las clases: `AssetsCatalogDataSource` está con
  estado (memoización, `@Singleton`); `RoomPracticeDataSource` y `DataStoreSettingsDataSource` están sin estado: solo
  dependen de un recurso que ya es único por proceso (Room y DataStore) y no guardan valores mutables propios.
- La entrada de arranque de `FraseAFraseApp` anota que `HiltTestRunner` ya existe y que solo falta el disparador de
  navegación.
- Siguiente cambio de infraestructura, con prioridad alta: un job de tests instrumentados con emulador en la CI, y los
  tests de UI de `:feature:session`, que nunca han pasado por detekt. Si la tarea 3.1 confirma que `src/androidTest` no se
  analiza, se añade su inclusión en detekt; si lo confirma, no se añade.
- Deuda nueva, no arreglo pendiente: revisar el runner genérico que el convention plugin aplica a módulos sin tests
  instrumentados. El arreglo del runner ya está hecho en `fix-instrumented-test-runner`.

**Limitación explícita:** sin ese job de CI, estos tests solo protegen en local. Un fallo de cableado solo se detecta si
alguien ejecuta `connectedDebugAndroidTest` antes de integrar.

### D8. Memoización del catálogo: sin cambios

`AssetsCatalogDataSourceTest` ya comprueba la memoización con contadores de lecturas del asset (`reads shouldBe 1`
en las pruebas de memoización de éxito y de fallo). Este cambio no añade nada sobre el catálogo en `:app`, y no hay que
anotar deuda de memoización.

## Risks / Trade-offs

- **[Hilt + KSP + AGP 9 para `androidTest` no es compatible en esta versión]** → Verificarlo en el primer grupo de apply
  compilando `:app:compileDebugAndroidTestKotlin` antes de escribir los tests. Si falla, parar y reportar: no se cambian
  versiones del catálogo sin decisión.
- **[Test de identidad pasa aunque se rompa el cableado]** → D5: mutación obligatoria antes de dar la tarea por cerrada.
- **[Solo protege en local]** → D7: limitación declarada y siguiente cambio de infraestructura.
- **[Tests instrumentados fuera de detekt y de Kover]** → Comprobado en la tarea 3.1. Si no se analizan, queda en la deuda
  con prioridad alta; no se cambia la configuración de detekt en este cambio.
- **[Room abre la base de datos real al primer acceso]** → El test no ejecuta consultas ni usa DAOs, así que no abre el
  fichero.

## Migration Plan

No hay despliegue ni migración de datos. Rollback: revertir los commits del cambio. Si el cambio se deshace después de
aceptar ADR-0014, el ADR se revierte a mano junto con la fila del índice.

## Open Questions

Ninguna.

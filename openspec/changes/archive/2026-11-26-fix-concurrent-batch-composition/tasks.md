# Tasks

## 1. Composición única con obtenciones concurrentes (`:core:domain` + test de `:core:data`)

> Nota: el grupo toca dos módulos. El test de integración de `:core:data` es el que reproduce la causa raíz y falla
> con el código actual; el hook de commit exige `check` en verde, así que no puede ir en un grupo separado de la
> corrección de dominio. `:core:data` solo recibe un test, ningún cambio de producción.

- [x] 1.1 Test de integración `ConcurrentObtainPracticeSessionTest` (`:core:data`, `src/test/.../practice`) con Room
  en memoria, barrera en `read()` y progreso previo de 4 frases A1 de nivel 1 con `dueAtSession` 5 (design D3).
  Escenario: practice-persistence → "Dos obtenciones simultáneas sin tanda activa componen una sola tanda". Verificar
  que falla por el motivo esperado (14 frases con progreso y tanda distinta), no por compilación.
- [x] 1.2 Tests rojos de `RegenerateActiveBatchForLevel` con `expected` (design D1, tabla): igual → compone; distinto
  y no nulo → `Existing` con la instantánea sin cambios; distinto y nulo → `NothingAvailable` con la instantánea sin
  cambios. Escenarios: practice-persistence → "La obtención que llega tarde no recompone la tanda ya compuesta", "La
  obtención que llega tarde sin tanda vigente devuelve que no hay frases". Verificar que fallan (falta el parámetro o
  recompone).
- [x] 1.3 Implementar el parámetro obligatorio `expected: ActiveBatch?` y la comparación dentro de `transact` en
  `RegenerateActiveBatchForLevel.kt`; actualizar su KDoc citando los ADR vigentes (ADR-0026, ADR-0019). Verificar
  1.2 en verde.
- [x] 1.4 Tests rojos de `ObtainActiveBatch` con el decorador local `InterleavingPracticeRepository` y los datos de
  partida de la spec (design D3): sin tanda y con cambio de ajustes, intercalados, exactamente una obtención devuelve
  `Created` y la otra `Existing` con la misma tanda, el progreso solo añade las 6 nuevas de esa tanda y los ajustes
  aplicados quedan registrados. Escenarios: practice-persistence → "Dos obtenciones simultáneas sin tanda activa
  componen una sola tanda", "Dos obtenciones simultáneas tras un cambio de ajustes recomponen una sola vez".
  Verificar que fallan: sin tanda, porque las dos devuelven `Created`; con cambio de ajustes, porque la tardía
  devuelve la tanda anterior.
- [x] 1.5 `ObtainActiveBatch` pasa `snapshot.activeBatch` de su `read()` como `expected` en las dos ramas que
  recomponen y, en la rama que no recompone, devuelve la tanda de un segundo `read()` (`NothingAvailable` si ya no
  hay), cubriendo también ese caso con un test (design D1); actualizar su KDoc citando los ADR vigentes (ADR-0026, ADR-0019). Verificar 1.4 y 1.1 en verde.
- [x] 1.6 `./gradlew :core:domain:check :core:data:check -q --console=plain` en verde (los tests existentes de
  `ObtainActiveBatchTest`, `RegenerateActiveBatchForLevelTest` y `RoomPracticeCycleTest` siguen pasando sin cambiar
  lo que verifican).

## 2. La pantalla detecta el cambio de idioma destino (`:feature:session`)

- [x] 2.1 Test rojo del ViewModel (design D3): tanda `pt-PT` cargada y revelada, cambio del idioma destino activo a
  `de-DE` con los mismos ids, orden y origen, y `Resume` → texto de destino de `de-DE`, sin revelar y
  `activeTargetLocale` `de-DE`. Escenario: practice-session → "Mismos ids y mismo origen en otro idioma destino
  muestran el texto del idioma nuevo". Verificar que falla porque el estado no se reemplaza.
- [x] 2.2 `LoadedItem(id, source, targetLocale)` en `PracticeSessionViewModel.kt` y comparación en `hasSameBatchAs`
  (design D2); actualizar sus comentarios. Verificar 2.1 en verde.
- [x] 2.3 `./gradlew :feature:session:check -q --console=plain` en verde (`PracticeSessionSourceLanguageTest` y
  los tests de `Resume` existentes siguen pasando).

## 3. Verificación final (sesión principal)

- [x] 3.1 `./gradlew check -q --console=plain` en verde.
- [x] 3.2 `./gradlew koverVerify -q --console=plain` en verde (dominio 90 %, presentación 80 %, global 70 %).

## Workflow follow-up

- Revisión de código, `/opsx:verify` y `/opsx:archive` en esta rama; el commit de archive abre la PR a `develop`.

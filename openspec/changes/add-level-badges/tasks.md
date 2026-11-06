# Tasks

## 1. Dominio: nivel de consolidación en `PracticeItem`

- [x] 1.1 Test: en `ObtainPracticeSessionTest`, una tanda con una entrada de nivel 3 produce un
  `PracticeItem` con `level` igual a ese nivel (spec `practice-session`, escenario "Insignia visible
  sin revelar"); verificar que el test falla porque `PracticeItem` no tiene `level` todavía.
- [x] 1.2 Implementación: añadir `level: Level` a `PracticeItem`
  (`practice/PracticeSessionResult.kt`) y copiarlo de `entry.level` en
  `ObtainPracticeSession.items()`; verificar que 1.1 pasa.

## 2. Design system: `DsLevelBadge`

- [ ] 2.1 Añadir `androidx-compose-material-icons-extended` a `gradle/libs.versions.toml` (misma
  versión que `androidx-compose-material-icons-core`, design D2) y como dependencia de
  `:core:designsystem`; verificar con `./gradlew :core:designsystem:dependencies` que resuelve sin
  conflicto de versión con el resto del BOM de Compose.
- [ ] 2.2 Test: `DsLevelBadgeTest` (Compose UI test en `:core:designsystem`) comprueba que el nivel 0
  no muestra icono ni etiqueta y que los niveles 1 a 5 muestran las etiquetas "Bronce", "Plata",
  "Oro", "Platino" y "Diamante" respectivamente (spec `level-badges`, escenarios "Nivel 0 no muestra
  insignia", "Nivel 1 muestra Bronce", "Nivel 2 muestra Plata", "Nivel 3 muestra Oro", "Nivel 4
  muestra Platino", "Nivel 5 muestra Diamante"); verificar que el test falla porque `DsLevelBadge` no
  existe.
- [ ] 2.3 Implementación: `DsLevelBadge(level: Int, modifier: Modifier = Modifier)` con la tabla de
  icono/color/etiqueta de design D2, sin tipos del dominio; verificar que 2.2 pasa.
- [ ] 2.4 Test: `DsLevelBadgeTest` comprueba que las insignias de nivel 2 y nivel 3 usan un icono o
  color distinto entre sí (spec `level-badges`, escenario "Oro y Plata no comparten representación");
  verificar que el test falla antes de ajustar la tabla de D2 si hace falta, y pasa después.
- [ ] 2.5 Añadir `@PreviewLightDark` de `DsLevelBadge` para los niveles 0 a 5; verificar visualmente
  en el preview de Android Studio que las cinco insignias son distinguibles en ambos temas.

## 3. Practicar: insignia de la frase actual

- [ ] 3.1 Test: en `PracticeSessionScreenTest` (Compose UI test, `androidTest`), una tanda cuya
  primera frase tiene nivel de consolidación 2 muestra la insignia "Plata" junto al nivel CEFR (spec
  `practice-session`, escenario "Insignia visible sin revelar"); verificar que el test falla porque
  la pantalla no pinta ninguna insignia.
- [ ] 3.2 Implementación: `PracticeSessionScreen` pinta `DsLevelBadge(current.level.value)` junto al
  nivel CEFR existente; verificar que 3.1 pasa.
- [ ] 3.3 Test: `PracticeSessionScreenTest` comprueba que al autoevaluar y pasar a la siguiente
  frase la insignia cambia con ella (spec `practice-session`, escenario "La insignia cambia con la
  frase mostrada"); verificar que pasa sin cambios adicionales de implementación.
- [ ] 3.4 Test: `PracticeSessionScreenTest` comprueba que una frase de nivel 0 no muestra ninguna
  insignia (spec `practice-session`, escenario "Sin insignia en nivel 0"); verificar que pasa.

## 4. Progreso: insignia por fila de nivel

- [ ] 4.1 Test: en `ProgressScreenTest` (Compose UI test, `androidTest`), la fila de nivel 3 muestra
  la insignia "Oro" junto a su número de frases, y la fila de nivel 0 no muestra ninguna insignia
  (spec `progress-overview`, escenarios "Catálogo con frases en varios niveles" y "La fila de nivel 0
  no muestra insignia"); verificar que el test falla porque `ProgressScreen` no pinta insignias.
- [ ] 4.2 Implementación: en `ContentState` de `ProgressScreen.kt`, añadir
  `DsLevelBadge(value)` junto al texto de cada fila de nivel (design D4); verificar que 4.1 pasa.

## 5. Verificación final

- [ ] 5.1 Ejecutar `./gradlew check` y `./gradlew koverVerify`; verificar que ambos terminan en verde
  sin bajar los mínimos de cobertura de `:core:domain`, `:core:designsystem`, `:feature:session` y
  `:feature:progress`.

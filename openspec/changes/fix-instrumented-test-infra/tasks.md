# Tasks

## 1. Runner e instrumentación condicionales (D1)

- [x] 1.1 Confirmar, con un emulador conectado, que `connectedDebugAndroidTest` del proyecto
      completo hoy NO falla en `:core:data` ni `:core:platform` (sin `src/androidTest`): termina
      en `Starting 0 tests... Finished 0 tests... BUILD SUCCESSFUL`, construyendo e instalando
      un APK de test vacío en cada uno. Guardar esa salida como evidencia de que D1 no corrige
      un fallo, sino el trabajo de más (build, empaquetado e instalación de ese APK).
      Evidencia (`./gradlew connectedDebugAndroidTest --no-configuration-cache`, proyecto
      completo, `Pixel_7a(AVD) - 17`): `:app:connectedDebugAndroidTest` y
      `:feature:session:connectedDebugAndroidTest` ejecutan sus tests reales (8 y 2 tests);
      `:core:platform:connectedDebugAndroidTest`, `:core:data:connectedDebugAndroidTest` y
      `:feature:settings:connectedDebugAndroidTest` (sin `src/androidTest`) imprimen
      `Starting 0 tests on Pixel_7a(AVD) - 17` / `Finished 0 tests on Pixel_7a(AVD) - 17` y el
      build termina en `BUILD SUCCESSFUL`, sin ningún fallo.
- [ ] 1.2 En `KotlinAndroid.kt`, condicionar `testInstrumentationRunner` y la llamada a
      `addInstrumentationRunnerDependency()` a que el módulo tenga `src/androidTest` (D1).
      Verificar con `./gradlew :core:data:dependencies --configuration androidTestImplementation`
      y el equivalente en `:core:platform` que `androidx-test:runner` ya no aparece, y que
      `:app:dependencies`/`:feature:session:dependencies` con la misma configuración sigue
      incluyéndolo.
- [ ] 1.3 Con un emulador conectado, verificar en verde que `./gradlew connectedDebugAndroidTest`
      del proyecto completo ya no falla en `:core:data` ni `:core:platform`, y que los tests
      reales de `:app` y `:feature:session` siguen pasando.

## 2. `detekt` analiza `src/androidTest` (D4)

- [ ] 2.1 Confirmar en rojo que `./gradlew :app:detekt` termina en verde hoy aunque
      `PersistenceWiringTest.kt:58` tenga una línea que excede `MaxLineLength` (hallazgo ya
      localizado en la exploración).
- [ ] 2.2 En `Quality.kt`, ampliar el `source` de la tarea `detekt` (no `detektBaseline` ni
      `detektGenerateConfig`) para incluir `src/androidTest/kotlin` cuando ese directorio
      exista (D4). Verificar en verde que `./gradlew :app:detekt` ahora falla señalando
      `PersistenceWiringTest.kt:58`.
- [ ] 2.3 Corregir esa línea (partir el comentario, sin cambiar su contenido) y verificar que
      `./gradlew :app:detekt :feature:session:detekt :core:data:detekt :core:platform:detekt`
      termina en verde.

## 3. Job de CI con emulador (D2, D3)

- [ ] 3.1 Añadir a `.github/workflows/ci.yml` un job que instale un emulador API 34 (sin Google
      APIs) con `reactivecircus/android-emulator-runner` y ejecute
      `./gradlew connectedDebugAndroidTest` del proyecto completo (D2), como paso informativo
      sin bloquear el resto del workflow (D3). Verificar con una ejecución real en GitHub
      Actions (push a esta rama o `workflow_dispatch`) que el job arranca el emulador y corre
      los tests de `:app` y `:feature:session` en verde.
- [ ] 3.2 Publicar los informes de `connectedDebugAndroidTest` (`**/build/reports/androidTests/`)
      como artefacto del job, igual que ya se hace para el resto de informes. Verificar que el
      artefacto aparece en la ejecución de 3.1.

## 4. Documentación

- [ ] 4.1 Actualizar `docs/deuda-tecnica.md`: cerrar "Job de tests instrumentados en la CI" y
      "`src/androidTest` fuera de detekt" (del cambio `add-hilt-wiring-tests`), y el
      "Pendiente" de la sección "Runner de tests instrumentados", enlazando a este cambio.

## 5. Verificación final

- [ ] 5.1 Ejecutar `./gradlew check koverVerify assembleDebug` en verde.
- [ ] 5.2 Ejecutar `./gradlew detekt -PwarningsAsErrors=true` en verde con los cuatro módulos de
      la tarea 2.3 incluidos.

## Workflow follow-up

- Archivar el cambio (`/opsx:archive`) una vez revisado por `android-architect` y
  `code-reviewer` según el presupuesto de revisión de `CLAUDE.md` (cambio ligero: una ronda de
  cada uno).
- Verificar el resultado archivado.

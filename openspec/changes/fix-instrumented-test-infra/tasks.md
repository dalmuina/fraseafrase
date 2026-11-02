# Tasks

## 1. Componente `AndroidTest` desactivado sin `src/androidTest` (D1)

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
- [x] 1.2 En `KotlinAndroid.kt`, desactivar el componente `AndroidTest` del `VariantBuilder` con
      `AndroidComponentsExtension.beforeVariants` cuando el módulo no tiene `src/androidTest`
      (D1; mecanismo final, tras descartar condicionar `testInstrumentationRunner` y la
      dependencia del runner — ver D1 en `design.md`). Verificado con
      `./gradlew :core:data:tasks`: ya no aparece una tarea `connectedDebugAndroidTest`
      específica para `:core:data` (solo los agregados `connectedAndroidTest`/`connectedCheck`).
      `testInstrumentationRunner` y la dependencia `androidx-test:runner` se mantienen sin
      condición en todos los módulos Android (son inertes donde el componente está desactivado).
- [x] 1.3 Con un emulador conectado, verificar en verde que `./gradlew connectedDebugAndroidTest`
      del proyecto completo ya no construye ni ejecuta nada en `:core:data` ni `:core:platform`,
      y que los tests reales de `:app` y `:feature:session` siguen pasando.
      Evidencia (`./gradlew connectedDebugAndroidTest --no-configuration-cache`, proyecto
      completo, `Pixel_7a(AVD) - 17`): solo aparecen `:feature:session:connectedDebugAndroidTest`
      (8 tests) y `:app:connectedDebugAndroidTest` (2 tests), ambos en verde, 0 fallos;
      `:core:data`, `:core:platform`, `:core:designsystem`, `:core:domain`, `:core:testing` y
      `:feature:settings` no generan esa tarea. `BUILD SUCCESSFUL`.

## 2. `detekt` analiza `src/androidTest` (D4)

- [x] 2.1 Confirmar en rojo que `./gradlew :app:detekt` termina en verde hoy aunque
      `PersistenceWiringTest.kt:58` tenga una línea que excede `MaxLineLength` (hallazgo ya
      localizado en la exploración).
      Evidencia: `./gradlew detekt -PwarningsAsErrors=true --no-configuration-cache` termina en
      `BUILD SUCCESSFUL`; `PersistenceWiringTest.kt:58` tiene 123 caracteres (`MaxLineLength`
      por defecto es 120, confirmado sin configuración propia en `config/detekt/detekt.yml`).
- [x] 2.2 En `Quality.kt`, ampliar el `source` de la tarea `detekt` (no `detektBaseline` ni
      `detektGenerateConfig`) para incluir `src/androidTest/kotlin` cuando ese directorio
      exista (D4). Verificar en verde que `./gradlew :app:detekt` ahora falla señalando
      `PersistenceWiringTest.kt:58`.
      Evidencia: `./gradlew :app:detekt --no-configuration-cache` falla con
      `PersistenceWiringTest.kt:58:1: Line detected, which is longer than the defined maximum
      line length... [MaxLineLength]` (`Analysis failed with 1 weighted issues`). `source` no
      acepta directamente una `ConfigurableFileCollection`; se usó `setSource(files(source,
      androidTestDir))`.
- [x] 2.3 Corregir esa línea (partir el comentario, sin cambiar su contenido) y verificar que
      `./gradlew :app:detekt :feature:session:detekt :core:data:detekt :core:platform:detekt`
      termina en verde.
      El `source` ampliado por 2.2 también alcanzó otra línea de 121 caracteres en el mismo
      fichero (`PersistenceWiringTest.kt:43`, bajo el mismo límite `MaxLineLength`): se partió
      igual que la de la línea 58, sin cambiar su contenido, para que el comando de verificación
      termine en verde. Evidencia: `./gradlew :app:detekt :feature:session:detekt
      :core:data:detekt :core:platform:detekt --no-configuration-cache` → `BUILD SUCCESSFUL`.

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

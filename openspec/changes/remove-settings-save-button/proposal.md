# Proposal

## Why

Hoy guardar un ajuste exige un paso explícito (el botón "Guardar") separado del paso que de verdad
importa al usuario: ir a practicar. El usuario quiere editar los ajustes y pasar a la pestaña de
práctica sin ese paso intermedio: la edición se guarda sola y, si corresponde, la tanda activa se
regenera al entrar en práctica. Esa decisión de "si corresponde" (`hasChanged`) hoy solo vive en
memoria del `ViewModel` de Ajustes, ligada a la acción `Save`: si el usuario edita un ajuste y cierra
la app sin pasar por ese botón, el cambio se pierde o, con el autoguardado de esta propuesta, queda
guardado pero nunca llega a aplicarse a la tanda activa. Para que autoguardar sea seguro, esa decisión
debe sobrevivir al cierre de la app: debe basarse en lo que de verdad generó la tanda activa
persistida, no en un estado transitorio del `ViewModel`.

## What Changes

- **BREAKING**: se elimina el botón "Guardar" y la acción `SettingsAction.Save` de la pantalla de
  ajustes. Ya no hay un paso explícito de guardado.
- El nivel CEFR, la N y el idioma destino elegidos en ajustes se persisten automáticamente según se
  eligen (autoguardado), en vez de quedar en un borrador que se descarta si no se confirma. Elegir un
  idioma destino en el desplegable lo activa de inmediato (igual que hoy ocurre al guardar).
- Se elimina la confirmación visual "Ajustes guardados" (`Snackbar`) y el callback
  `onShowSaveConfirmation`: ya no hay un evento de guardado puntual que confirmar.
- Se elimina el requisito de "descartar la edición al salir sin guardar" y el de "no cambiar el
  idioma destino activo al salir sin guardar": ahora toda edición persiste y activa de inmediato.
- El sistema persiste, por idioma destino, con qué nivel CEFR y N se compuso la tanda activa vigente
  ("ajustes aplicados"). Cada vez que se obtiene la tanda activa de un idioma destino (al abrir la
  pantalla de práctica, incluido el arranque de la app) y ya hay ajustes aplicados registrados que
  difieren de los ajustes guardados de ese idioma destino, el sistema la recompone de inmediato con
  los ajustes guardados vigentes, con las mismas garantías que ya existían (sin tocar progreso, racha,
  nivel de consolidación, `dueAtSession` ni el contador de sesiones), y registra esos ajustes como los
  nuevos aplicados. Si coinciden, no se recompone nada.
- Este cambio sustituye al disparador anterior ("al confirmar el guardado"): ahora la comparación
  ocurre siempre que se entra en práctica, sobreviva o no la app de por medio, sin que la pantalla de
  ajustes tenga que intervenir.
- El selector de idioma destino en Ajustes pasa a tener el mismo estilo de tarjeta (fondo blanco,
  esquinas redondeadas, relleno) que las tarjetas de nivel CEFR y tamaño de tanda, en vez de quedar
  suelto en el fondo de la pantalla.

## Capabilities

### New Capabilities

(ninguna)

### Modified Capabilities

- `settings-screen`: el guardado deja de ser una acción explícita confirmada por el usuario y pasa a
  ser automático, incluida la activación del idioma destino elegido; desaparece la confirmación visual
  y el requisito de descarte al salir sin guardar. El selector de idioma destino adopta el estilo de
  tarjeta de las demás secciones.
- `user-settings`: se añade la persistencia de los "ajustes aplicados" (nivel CEFR y N con los que se
  compuso la tanda activa vigente) por idioma destino, independiente de los ajustes editables.
- `practice-persistence`: obtener la tanda activa de un idioma destino pasa a comparar los ajustes
  aplicados persistidos con los ajustes guardados vigentes y, si difieren, recompone la tanda activa
  antes de devolverla, registrando los ajustes aplicados nuevos.

## Impact

- `:core:domain`: `SettingsRepository` gana `appliedSettings(targetLocale)`/
  `recordAppliedSettings(targetLocale, settings)`. `ObtainActiveBatch` compara los ajustes aplicados
  con los guardados y delega en `RegenerateActiveBatchForLevel` cuando difieren (o cuando no hay tanda
  activa); `GenerateSession` registra los ajustes aplicados tras componer. Ninguno de los dos cambia su
  contrato observable salvo esta comparación adicional.
- `:core:data`: `DataStoreSettingsDataSource` añade las claves de ajustes aplicados por idioma destino;
  `:core:testing` (`FakeSettingsRepository`) se actualiza igual.
- `:feature:settings`: `SettingsViewModel` autoguarda cada edición válida (nivel CEFR, N, idioma
  destino) sin ninguna acción de salida especial; se retiran `SettingsAction.Save`,
  `ConsumeSaveConfirmation` y los campos de `SettingsUiState` ligados a la confirmación/navegación de
  guardado. `SettingsScreen` pierde el botón "Guardar" y da estilo de tarjeta al selector de idioma.
  `SettingsRoute` pierde `onShowSaveConfirmation`.
- `:app`: `FraseAFraseApp` deja de necesitar ningún enganche especial entre la barra de navegación
  inferior y Ajustes (la navegación vuelve a ser una simple asignación de pestaña, como en
  `app-navigation`); se retira el `SnackbarHost` de confirmación de guardado, sin otro uso en la app.
- Tests existentes de `SettingsViewModelTest`, `SettingsRouteTest`, `SettingsScreenTest`,
  `FraseAFraseAppTest`, `ObtainActiveBatchTest`/equivalentes de `practice-persistence` y
  `RegenerateActiveBatchForLevelTest` que ejercitan el flujo anterior.
- Respeta el alcance del ADR-0012 (no cambia el método de práctica), el ADR-0006 (sigue siendo
  `UiState` + `Action` con un único `onAction()`) y el ADR-0013 (persistencia offline-first ya
  establecida); no se requiere ningún ADR nuevo.

## Fuera de alcance

- No cambia qué se guarda como ajustes editables (nivel CEFR, N, idioma destino) ni las reglas de
  validación de N.
- No cambia el criterio de qué constituye un "cambio" que regenera la tanda activa (nivel CEFR o N
  distintos de los últimos aplicados), solo dónde y cuándo se evalúa y se persiste.
- No cambia la barra de navegación inferior en sí (sus tres destinos, su aspecto).
- No añade ningún indicador visual de "cambios pendientes" ni de "sesión regenerada".
- No cambia el aspecto de las tarjetas de nivel CEFR ni de tamaño de tanda, solo extiende su mismo
  estilo al selector de idioma destino.

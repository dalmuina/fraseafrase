# Design

## Context

Hoy `SettingsViewModel.save()` hace tres cosas atadas a una única acción `Save`: persiste
`UserSettings` para el idioma destino mostrado, cambia el idioma destino activo
(`SelectActiveLanguage`) y, si el nivel CEFR o N cambiaron respecto a lo que había al abrir Ajustes
(una base de comparación que solo vive en memoria del `ViewModel`), recompone la tanda activa
(`RegenerateActiveBatchForLevel`) y pide navegar a práctica. Ver proposal.md - Why / What Changes
para la motivación de quitar ese paso explícito y de que la comparación sobreviva al cierre de la
app.

`ObtainActiveBatch` (`:core:domain`, capacidad `practice-persistence`) es el único punto por el que
pasa toda entrada a la pantalla de práctica, incluida la que ocurre al arrancar la app (la pestaña de
práctica es la de inicio, `app-navigation`): hoy, si ya hay tanda activa, la devuelve tal cual, sin
mirar los ajustes guardados en absoluto.

## Goals / Non-Goals

**Goals:**
- Quitar el botón "Guardar" y la acción `Save` sin perder ninguna garantía de dominio existente (no
  tocar progreso, no incrementar el contador de sesiones, no recomponer si no hay cambios).
- Autoguardar cada edición válida (nivel CEFR, N, idioma destino) de inmediato, incluida la
  activación del idioma destino elegido.
- Hacer que "¿hay que recomponer la tanda activa?" sea una pregunta que se responde con datos
  persistidos, no con el estado en memoria de un `ViewModel`, para que sobreviva al cierre de la app.
- Dar al selector de idioma destino el mismo estilo de tarjeta que las demás secciones de Ajustes.

**Non-Goals:**
- No cambia `SaveUserSettings`, `SelectActiveLanguage`, `ComposeNextBatch` ni sus contratos.
- No introduce una pila de navegación (`NavHost`) en `:app`.
- No cambia qué aspecto tiene la pantalla de ajustes salvo la ausencia del botón "Guardar" y el
  nuevo estilo del selector de idioma.

## Decisions

### D1 — "Ajustes aplicados" persistidos junto a los ajustes editables, no en memoria

Se añade a `SettingsRepository` (`:core:domain`) un segundo par de valores por idioma destino,
independiente de los ajustes editables:

```kotlin
interface SettingsRepository {
    // ...existentes...
    suspend fun appliedSettings(targetLocale: LanguageTag): UserSettings?
    suspend fun recordAppliedSettings(targetLocale: LanguageTag, settings: UserSettings)
}
```

`appliedSettings` son los ajustes (nivel CEFR + N) con los que se compuso la tanda activa vigente de
ese idioma destino; `null` significa "no registrados todavía" (instalación existente antes de este
cambio, o idioma destino sin tanda activa nunca compuesta). Se implementan en
`DataStoreSettingsDataSource` (única fuente, ADR-0010) con dos claves nuevas por idioma destino
(`applied_cefr_level_<locale>`, `applied_batch_size_<locale>`), con el mismo patrón que las claves de
ajustes editables ya existentes: ninguna lectura falla, un valor ausente o no válido se trata como "no
registrado".

**Alternativas consideradas**: guardar el nivel CEFR en `ActiveBatch`/`active_batch` (Room), ya que N
ya es recuperable de su `capacity`. Se descarta: exige una migración destructiva de `PracticeDatabase`
y tocar `ComposeNextBatch`/`PracticeBatch`/`PracticeSnapshot.storing()` y decenas de puntos de
construcción de `PracticeBatch`/`ActiveBatch` en tests, para un dato que conceptualmente ya vive junto
a los ajustes (incluido su propio ciclo de vida de persistencia en DataStore).

### D2 — `ObtainActiveBatch` decide si recompone, delegando en `RegenerateActiveBatchForLevel`

```kotlin
class ObtainActiveBatch @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val practiceRepository: PracticeRepository,
    private val regenerateActiveBatchForLevel: RegenerateActiveBatchForLevel,
) {
    suspend operator fun invoke(targetLocale: LanguageTag): ActiveBatchResult {
        val settings = settingsRepository.settings(targetLocale)
        val existing = practiceRepository.read(targetLocale).batch()
        if (existing != null) {
            val applied = settingsRepository.appliedSettings(targetLocale)
            if (applied == null || applied == settings) return ActiveBatchResult.Existing(existing)
        }
        val result = regenerateActiveBatchForLevel(targetLocale, settings.cefrLevel, settings.batchSize)
        if (result is ActiveBatchResult.Created) {
            settingsRepository.recordAppliedSettings(targetLocale, settings)
        }
        return result
    }
}
```

`RegenerateActiveBatchForLevel` ya compone "sin tanda activa, directamente" y "con tanda activa,
vaciándola sin resolución", ambas sin incrementar el contador de sesiones ni tocar progreso (ADR-0017):
cubre tanto el primer uso como la recomposición por ajustes distintos con el mismo código, sin
duplicar la composición dentro de `ObtainActiveBatch`. `ObtainActiveBatch` dejaba de necesitar
`ComposeNextBatch`/`ObtainCatalogPhrasesForActiveLanguage` directamente: se los pasa a
`RegenerateActiveBatchForLevel`, que ya los tenía.

`GenerateSession` (la otra composición existente, ADR-0017) registra igual sus ajustes aplicados tras
componer con éxito, para que el invariante "los ajustes aplicados son los que generaron la tanda
activa vigente" se mantenga sin depender del orden de llamadas entre `ObtainActiveBatch` y
`GenerateSession`.

**Alternativas consideradas**: mantener `RegenerateActiveBatchForLevel` como una llamada explícita
solo desde Ajustes (el diseño original de esta propuesta) — descartada porque no sobrevive al cierre
de la app sin la base de comparación en memoria que este cambio elimina (D1).

### D3 — `SettingsViewModel` deja de orquestar la regeneración por completo

Con D1/D2, decidir si hay que recomponer deja de ser responsabilidad de Ajustes: ocurre siempre que se
obtiene la tanda activa, la pida quien la pida (Ajustes→Práctica, Progreso→Práctica, o el arranque de
la app en la pestaña de práctica). `SettingsViewModel` se simplifica a autoguardar cada edición válida
sin ninguna acción ni señal de "salida":

```kotlin
sealed interface SettingsAction {
    data class SelectLanguage(val targetLocale: LanguageTag) : SettingsAction
    data class SelectCefrLevel(val level: CefrLevel) : SettingsAction
    data class ChangeBatchSizeText(val text: String) : SettingsAction
}
```

`selectLanguage()` pasa a llamar a `selectActiveLanguage(targetLocale)` de inmediato (ya no hace
falta diferirlo: ya no dispara ninguna recomposición cara, solo escribe el idioma destino activo).
`selectCefrLevel()`/`changeBatchSizeText()` llaman a `saveUserSettings` cuando el resultado es válido,
igual que antes. `SettingsUiState.Loaded` pierde `saveConfirmationPending` y `navigateToPracticePending`;
`SettingsRoute` pierde `onShowSaveConfirmation` y `onNavigateToPractice`. `FraseAFraseApp` vuelve a
asignar `selectedDestination` directamente desde la barra inferior para los tres destinos, sin ningún
enganche especial para Ajustes, y se retira el `SnackbarHost` de confirmación de guardado (sin otro
uso en la app).

### D4 — Selector de idioma destino con el mismo estilo de tarjeta que CEFR/N

`DsLanguageOptions` se envuelve en un `Card` (`MaterialTheme.shapes.large`,
`CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)`,
`Modifier.padding(AppTheme.spacing.l)`) igual que `CefrLevelCard`/`BatchSizeCard` en
`SettingsScreen.kt`, en vez de quedar directamente sobre el fondo de la pantalla. `DsLanguageOptions`
en sí (`:core:designsystem`) no cambia: la tarjeta es responsabilidad de `SettingsScreen`, que ya
envuelve del mismo modo las otras dos secciones.

## Risks / Trade-offs

- **[Riesgo] `ObtainActiveBatch` ahora hace una lectura adicional (`appliedSettings`) en cada
  obtención de la tanda activa** → **Mitigación**: es una lectura de DataStore, del mismo orden de
  coste que la lectura de `settings(targetLocale)` que ya hacía; no añade ninguna transacción de
  escritura cuando los ajustes coinciden.
- **[Riesgo] Instalaciones existentes no tienen ajustes aplicados registrados** (idiomas destino con
  tanda activa de antes de este cambio) → **Mitigación**: `appliedSettings == null` se trata como "no
  recomponer, confiar en la tanda activa existente" (D2), nunca como "ha cambiado"; el primer registro
  ocurre en la próxima composición real (primer uso, generar sesión, o una recomposición futura por un
  cambio de ajustes posterior a esta actualización).
- **[Riesgo] Doble pulsación rápida sobre "práctica" dispara dos regeneraciones concurrentes** →
  **Mitigación**: ya existente hoy (la `Save` original tenía el mismo riesgo); no se introduce
  mecanismo nuevo porque está fuera del alcance de este cambio.

## Migration Plan

No hay migración de esquema: `appliedSettings`/`recordAppliedSettings` usan claves nuevas en el mismo
DataStore de ajustes (ADR-0013), que empiezan ausentes para toda instalación existente y se tratan
como "no registrado" (ver Risks). No se toca `PracticeDatabase` ni ningún esquema de Room.

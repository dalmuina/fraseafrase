# ADR-0006: UI con Jetpack Compose, MVVM/UDF e inyección con Hilt

- **Estado:** Aceptado
- **Fecha:** 2026-10-29

## Contexto
Necesitamos una capa de presentación declarativa, predecible y testeable sin dispositivo.

## Decisión
- **UI:** Jetpack Compose con Material 3. Sin XML de vistas.
- **Patrón:** MVVM con **flujo de datos unidireccional (UDF)**:
  - cada pantalla expone un único `StateFlow<XxxUiState>` inmutable;
  - la UI envía acciones mediante una interfaz sellada `XxxAction` y un único `onAction(action)`;
  - los efectos de un solo uso (navegación, reproducir audio) se modelan como estado consumible o canal de eventos, nunca con callbacks directos desde el ViewModel.
- Los composables de pantalla se dividen en `XxxRoute` (conecta con el ViewModel) y `XxxScreen` (sin estado, recibe `uiState` y `onAction`), de modo que `XxxScreen` se prueba y previsualiza sin ViewModel.
- Los ViewModels solo dependen de **casos de uso** del dominio, nunca de repositorios ni de APIs de Android.
- **Inyección de dependencias:** Hilt. Los módulos de Hilt se declaran en `:app`, y en `:core:data` / `:core:platform` para sus propias implementaciones.
- **Concurrencia:** corrutinas y `Flow`; los dispatchers se inyectan para que los tests los sustituyan.

## Consecuencias
- **Positivas:** estado de pantalla explícito y fácil de testear; previsualizaciones fiables; separación UI/lógica.
- **Negativas:** algo de código repetitivo (estado, acciones) por pantalla.
- **Descartado:** Koin (resolución en ejecución, errores tardíos); MVI con librerías externas (innecesario a esta escala).

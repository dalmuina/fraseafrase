# Proposal

## Why

El ADR-0012 incluye "progreso básico" (frases certificadas en la tanda activa y total de frases por
nivel) dentro del alcance del MVP, pero no hay spec ni UI para ello todavía (`docs/pendientes-mvp.md`,
punto 3). Sin esto, el usuario no tiene ninguna señal de cuánto catálogo domina ni de cómo va la tanda
actual más allá de lo que ve frase a frase.

## What Changes

- Nueva pantalla de progreso que muestra: cuántas frases de la tanda activa están certificadas (racha
  ≥ 3) sobre el total de la tanda, y el total de frases del catálogo por nivel de consolidación (0 a 5),
  incluyendo las frases nunca vistas en el nivel 0.
- Nueva barra de navegación inferior en `:app` con tres destinos — Práctica, Progreso, Ajustes — que
  sustituye la navegación actual de ajustes por *back stack* (abrir con una acción desde la práctica,
  volver con el botón atrás).
- **BREAKING** (solo navegación interna, sin impacto en datos): se elimina la acción "abrir ajustes"
  de la pantalla de práctica y el botón "volver" de la pantalla de ajustes; ambas pantallas pasan a
  seleccionarse como pestañas de la barra inferior.

## Capabilities

### New Capabilities

- `progress-overview`: certificadas en la tanda activa, total de frases por nivel y la pantalla que
  los muestra, incluidos sus estados sin tanda activa o con catálogo no disponible.
- `app-navigation`: la barra de navegación inferior con sus tres destinos, la pestaña seleccionada al
  abrir la app y el cambio de pestaña.

### Modified Capabilities

- `settings-screen`: los requisitos "Acceso a la pantalla de ajustes desde la práctica" y "Vuelta a la
  práctica desde los ajustes" se sustituyen por la selección de pestaña de `app-navigation`.
- `practice-session`: el requisito "Pantalla de práctica como inicio" se sustituye por la pestaña
  inicial de `app-navigation`.

## Fuera de alcance

- Estadísticas históricas, gráficas o tendencia del progreso en el tiempo.
- Persistir qué pestaña estaba seleccionada entre reinicios: cada apertura empieza en práctica.
- Cualquier puerto o tabla de persistencia nueva para el progreso: se calcula a partir de
  `CatalogRepository` y `PracticeRepository`, ya existentes.

## Impact

- `:app`: `FraseAFraseApp.kt` y `Routes.kt` pasan de un *back stack* de Navigation 3 con una acción de
  apertura a una barra de navegación inferior (`NavigationBar` de Material 3) con tres pestañas.
- `:feature:progress` (nuevo módulo, plugin `fraseafrase.android.feature`): pantalla, ViewModel,
  `UiState` y `Action` del progreso.
- `:core:domain`: nuevo caso de uso que combina `CatalogRepository` y `PracticeRepository` para
  obtener el resumen de progreso. Ningún puerto nuevo: ambos repositorios ya existen.
- `:feature:session` y `:feature:settings`: se retira la acción de abrir/volver que ya no necesitan
  (la navegación pasa a vivir solo en `:app`, como marca ADR-0010).
- `settings.gradle.kts`: se añade `:feature:progress`.
- Sin cambios en `:core:data`, `:core:platform` ni en el esquema de persistencia (ADR-0013): el
  progreso se calcula a partir de datos ya persistidos, sin nueva tabla.
- Respeta el alcance del ADR-0012 (punto 6, "Progreso básico"); no introduce estadísticas avanzadas
  (fuera de alcance explícito del ADR-0012).

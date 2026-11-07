# Proposal

## Why

`:core:designsystem` usa hoy la paleta y la tipografía por defecto de Material 3 (`Color.kt` y
`Type.kt` están marcados explícitamente como provisionales, a la espera del cambio de OpenSpec que
fije el diseño definitivo). El usuario ha entregado 4 mocks de alta fidelidad (Practicar con
traducción oculta y visible, Progreso, Ajustes) con una paleta índigo/verde/rojo, tipografía
redondeada y tarjetas con esquinas muy curvas, además de un set de iconos vectoriales para las 6
insignias de rango (incluida una nueva insignia visual para "sin rango"). Este cambio fija ese
diseño definitivo y lo aplica a las tres pantallas existentes.

## What Changes

- Fijar la paleta de color definitiva (claro y oscuro) en `core/designsystem/theme/Color.kt`,
  derivada de los mocks: primario índigo (`#3346C8`), superficie de fondo gris muy claro
  (`#F5F6FA`), contenedor de traducción revelada en tono índigo claro, verde de acierto, rojo de
  fallo. Paleta oscura derivada de los mismos tonos.
- Fijar la tipografía definitiva en `core/designsystem/theme/Type.kt` con una Google Font
  redondeada para titulares y una complementaria para cuerpo de texto (propuesta concreta en
  `design.md`, vía downloadable fonts de Compose).
- Actualizar `AppTheme` (formas/`Shapes`) a esquinas muy redondeadas acorde a los mocks (tarjetas,
  botones, chips).
- Sustituir los iconos de `DsLevelBadge` (Material Icons `MilitaryTech`/`WorkspacePremium`/`Diamond`)
  por los 6 vectores entregados por el usuario (`ic_rango_sin_rango`, `ic_rango_bronce`,
  `ic_rango_plata`, `ic_rango_oro`, `ic_rango_platino`, `ic_rango_diamante`), uno por nivel de 0 a 5.
- **BREAKING** (comportamiento observable): el nivel 0 pasa a mostrar una insignia visual ("sin
  rango", icono + etiqueta), en vez de no mostrar nada como hoy.
- Añadir iconos a los tres destinos de la barra de navegación inferior (`:app`), hoy vacíos
  (`icon = {}`).
- Rediseñar la pantalla de Progreso: sustituir la línea de certificadas de la tanda activa
  (`certifiedInBatch`) por una tarjeta superior con el total de frases disponibles del catálogo
  (suma de `byLevel`) y el nivel CEFR vigente.
- Restilizar Practicar y Ajustes sobre los nuevos tokens, sin cambiar su lógica ni sus datos.

### ADR afectados

- ADR-0012/ADR-0015/ADR-0016/ADR-0017 (modelo de práctica y alcance del MVP): el punto 6 de
  "Alcance del MVP" ("Progreso básico: frases certificadas en la tanda activa y total de frases por
  nivel") seguía vigente sin cambios y exigía mostrar las certificadas de la tanda activa. Este
  cambio lo contradice, así que requiere un ADR nuevo que lo sustituya: **ADR-0018**, que actualiza
  ese punto para reflejar que, con la resolución al instante de ADR-0017, ese contador deja de
  aportar información útil (ver `docs/adr/0018-progreso-basico-sin-certificadas-de-la-tanda-activa.md`).
  El resto del modelo de práctica (tandas, racha, certificación, niveles, cuotas, enfriamiento) no
  se toca.
- ADR-0003, ADR-0004/ADR-0013, ADR-0010: no se tocan; ver `design.md` para cómo este cambio respeta
  cada uno (tipografía empaquetada por offline-first, dependencias de ViewModel solo a casos de uso).

### Fuera de alcance

- Cualquier cambio de comportamiento de dominio, repetición espaciada, tandas o ajustes más allá de
  lo descrito arriba y en ADR-0018.
- Pantallas o flujos no cubiertos por los 4 mocks (p. ej. onboarding, corrección por voz).
- Animaciones o `motion` nuevas más allá de las ya definidas en `AppTheme.motion`.
- Asegurar contraste WCOAG/accesibilidad más allá de lo que ya exige el proyecto; se usará buen
  juicio pero no es un criterio de aceptación de este cambio.

## Capabilities

### New Capabilities

(ninguna: este cambio solo modifica comportamiento visual y de contenido de pantallas existentes)

### Modified Capabilities

- `progress-overview`: la pantalla deja de mostrar "certificadas en la tanda activa" y pasa a
  mostrar el total de frases disponibles del catálogo y el nivel CEFR vigente en su lugar.
- `level-badges`: el nivel 0 pasa a tener una insignia visual propia ("sin rango") en vez de no
  mostrar ninguna.
- `app-navigation`: cada destino de la barra de navegación inferior MUST mostrar un icono además de
  su etiqueta de texto.
- `practice-session`: la pantalla de práctica pasa a mostrar la insignia "sin rango" para la frase
  actual cuando su nivel de consolidación es 0, en vez de no mostrar ninguna insignia.

## Impact

- `core/designsystem`: `theme/Color.kt`, `theme/Type.kt`, `theme/Shape.kt` (nuevo), `component/DsLevelBadge.kt`,
  nuevos recursos `drawable/ic_rango_*.xml`, `build.gradle.kts` (dependencia de Google Fonts
  downloadable si se usa esa vía).
- `feature/progress`: `ProgressUiState.kt` (quitar `certifiedInBatch`/`CertifiedCount`, añadir total
  disponible y nivel CEFR), `ProgressScreen.kt`, su ViewModel y tests.
- `feature/session`: `PracticeSessionScreen.kt` (restilizado, sin cambios de estado).
- `feature/settings`: `SettingsScreen.kt` (restilizado, sin cambios de estado).
- `app`: `FraseAFraseApp.kt` (iconos de navegación), `build.gradle.kts` (añadir
  `material-icons-extended` como `implementation`, hoy solo transitiva dentro de
  `:core:designsystem`).
- Specs: `progress-overview`, `level-badges`, `app-navigation`, `practice-session`.

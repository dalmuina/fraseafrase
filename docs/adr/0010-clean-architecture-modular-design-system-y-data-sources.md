# ADR-0010: Clean Architecture modular con design system y data sources

- **Estado:** Aceptado
- **Fecha:** 2026-10-29
- **Sustituye a:** ADR-0005

## Contexto
El ADR-0005 definía un módulo `:core:ui` genérico y no distinguía entre repositorios y fuentes de datos.
Se necesita:

1. Un **design system centralizado**: tema, tokens, animaciones y componentes reutilizables en un único lugar,
   para que un mismo botón o badge no se duplique en varias features.
2. Un vocabulario claro en la capa de datos que evite repositorios "de paso" que solo delegan en una única fuente.

## Decisión

### Mapa de módulos

```
:app                    → punto de entrada, navegación, grafo de DI
:feature:session        → UI y ViewModel de la sesión diaria
:feature:settings       → UI y ViewModel de ajustes
:core:designsystem      → tema, tokens, animaciones y componentes Compose reutilizables
:core:data              → data sources y repositorios (Room, DataStore, assets)
:core:platform          → adaptadores de Android: TTS, reconocimiento de voz, reloj
:core:domain            → entidades, casos de uso, puertos (interfaces). Kotlin puro.
:core:testing           → fakes, builders y utilidades de test compartidas
```

### Regla de dependencias (solo hacia dentro)

- `:core:domain` no depende de ningún módulo ni de Android (`kotlin("jvm")`).
- `:core:designsystem` no depende de ningún módulo del proyecto: ni de `:core:domain` ni de ninguna feature.
- `:core:data` y `:core:platform` dependen de `:core:domain` e implementan sus puertos.
- `:feature:*` dependen de `:core:domain` y `:core:designsystem`; **nunca** de `:core:data`, `:core:platform` ni de otras features.
- Solo `:app` conoce todas las implementaciones y las une mediante DI.

### Design system (`:core:designsystem`)

**Contenido:**

```
theme/        AppTheme, colores (claro/oscuro), tipografía, formas
tokens/       espaciados, tamaños, elevaciones, radios (expuestos con CompositionLocal)
motion/       duraciones, curvas de easing y transiciones/animaciones reutilizables
component/    botones, badges, tarjetas, barras de progreso, diálogos…
icon/         iconos del proyecto
```

**Reglas:**

- `AppTheme` envuelve a `MaterialTheme` (Material 3) y añade los tokens propios. Toda pantalla se renderiza dentro de `AppTheme`.
- Las features **no definen** colores, tipografías, dimensiones ni curvas de animación propias: usan los tokens del design system. No hay valores literales de color, `dp` o duración en `:feature:*`.
- Los componentes del design system llevan el prefijo `Ds` (`DsButton`, `DsBadge`, `DsCard`) para distinguirlos de los de Material.
- Los componentes son **sin estado y agnósticos del dominio**: reciben tipos primitivos o propios del design system (`String`, `Boolean`, lambdas, enums de variante), nunca entidades del dominio.
- Todo componente expone `modifier: Modifier = Modifier` como primer parámetro opcional y tiene `@Preview` en tema claro y oscuro.
- **Regla de promoción:** un componente nace en su feature. Cuando una segunda feature lo necesita, se generaliza y se mueve al design system. Los componentes genéricos evidentes (botones, badges) pueden nacer directamente en él.
- Un componente que necesite entidades del dominio y se comparta entre features **no** va al design system; si llega ese caso, se crea `:core:ui` mediante un ADR nuevo.

### Capa de datos: Repository y DataSource

- **Dominio:** los puertos de acceso a datos se declaran **siempre** como interfaces `XxxRepository` en `:core:domain`. El dominio no conoce el concepto de DataSource ni cuántas fuentes hay detrás de un repositorio.
- **`DataSource`:** clase de `:core:data` que accede a **una única fuente** (Room, DataStore, assets, red en el futuro). Encapsula los tipos del framework (DAOs, entidades de Room, `Preferences`).
- **`Repository` (implementación):** clase de `:core:data` que **coordina dos o más** data sources (caché, sincronización, fusión de resultados).

**Cómo se implementa un puerto `XxxRepository` del dominio:**

| Fuentes | Implementación | Nombre |
|---|---|---|
| Una | Un `DataSource` implementa directamente la interfaz del dominio y mapea a entidades del dominio. No hay clase intermedia. | `<Tecnología><Entidad>DataSource` → `RoomPhraseDataSource : PhraseRepository` |
| Dos o más | Un repositorio implementa la interfaz del dominio y compone data sources, que pasan a ser interfaces internas de `:core:data`. | `<Estrategia><Entidad>Repository` → `OfflineFirstProgressRepository : ProgressRepository`, que usa `ProgressLocalDataSource` y `ProgressRemoteDataSource` |

**Evolución de una a varias fuentes:** el data source existente deja de implementar la interfaz del dominio y se crea el repositorio que lo compone. El cambio afecta solo a `:core:data` y al binding de DI; el dominio, los casos de uso y las features no cambian.

**Testing:**

- Data sources: tests de integración contra la tecnología real (Room en memoria, DataStore en fichero temporal).
- Repositorios con varias fuentes: tests unitarios con fakes de sus data sources.
- Casos de uso y ViewModels: fakes de los `XxxRepository` del dominio, alojados en `:core:testing`.

### Convenciones que se mantienen del ADR-0005

- Un caso de uso = una clase con una única operación pública (`operator fun invoke`).
- Entidades del dominio inmutables y sin anotaciones de Room ni de serialización; los mapeos viven en `:core:data`.
- Errores esperables modelados como tipos sellados, no como excepciones.
- El tiempo se inyecta mediante el puerto `Clock`.
- Configuración de build común en convention plugins (`build-logic/`) y versiones en `gradle/libs.versions.toml`.

## Consecuencias
- **Positivas:**
  - Un único lugar para el aspecto visual: cambiar un color, un botón o una animación afecta a toda la app de forma coherente.
  - El design system es reutilizable y testeable de forma aislada al no depender del dominio.
  - Sin repositorios vacíos de paso en el caso habitual de una sola fuente.
  - Añadir una fuente (por ejemplo, sincronización en la nube) no modifica el dominio.
- **Negativas:**
  - Disciplina necesaria para no saltarse los tokens en las features (se revisará en code review y, más adelante, con reglas de detekt).
  - Un data source que pasa a formar parte de un repositorio requiere una pequeña refactorización dentro de `:core:data`.
- **Descartado:**
  - Declarar interfaces `XxxDataSource` en el dominio: el dominio conocería la topología de datos y cualquier cambio en el número de fuentes obligaría a renombrar puertos y modificar casos de uso.
  - Mantener `:core:ui` como cajón de sastre de UI compartida.

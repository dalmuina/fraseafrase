# Architecture Decision Records

Registro de decisiones de arquitectura del proyecto. Cada ADR es inmutable una vez aceptado:
si una decisión cambia, se crea un ADR nuevo que **sustituye** al anterior (y el antiguo pasa a estado `Sustituido por ADR-XXXX`).

| ADR | Título | Estado |
|-----|--------|--------|
| [0001](0001-registrar-decisiones-con-adr.md) | Registrar decisiones de arquitectura con ADR | Aceptado |
| [0002](0002-desarrollo-guiado-por-especificaciones-openspec.md) | Desarrollo guiado por especificaciones con OpenSpec | Aceptado |
| [0003](0003-portugues-europeo-modelo-multivariante.md) | Portugués europeo con modelo de datos multivariante | Aceptado |
| [0004](0004-offline-first-sin-backend-ni-ia.md) | Offline-first, sin backend ni IA en el MVP | Aceptado |
| [0005](0005-clean-architecture-modular.md) | Clean Architecture modular | Sustituido por 0010 |
| [0006](0006-ui-compose-mvvm-udf-hilt.md) | UI con Jetpack Compose, MVVM/UDF e inyección con Hilt | Aceptado |
| [0007](0007-estrategia-de-testing-tdd.md) | Estrategia de testing y TDD obligatorio | Aceptado |
| [0008](0008-minsdk-26.md) | minSdk 26 | Aceptado |
| [0009](0009-alcance-del-mvp.md) | Alcance del MVP | Sustituido por 0012 |
| [0010](0010-clean-architecture-modular-design-system-y-data-sources.md) | Clean Architecture modular con design system y data sources | Aceptado |
| [0011](0011-detekt-1-23-con-daemon-gradle-jdk-21.md) | detekt 1.23 con el daemon de Gradle en JDK 21 | Aceptado |
| [0012](0012-modelo-de-practica-por-tandas.md) | Modelo de práctica por tandas y alcance del MVP | Aceptado |

## Plantilla

```markdown
# ADR-XXXX: Título

- **Estado:** Propuesto | Aceptado | Sustituido por ADR-YYYY
- **Fecha:** AAAA-MM-DD

## Contexto
Qué problema o fuerza obliga a decidir.

## Decisión
Qué se decide, en presente y de forma concreta.

## Consecuencias
Positivas, negativas y lo que queda descartado.
```

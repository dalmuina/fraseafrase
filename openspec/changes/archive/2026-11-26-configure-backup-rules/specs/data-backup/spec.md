# Spec Delta

## Purpose
Define qué datos de la app entran en la copia de seguridad del sistema y en la transferencia entre dispositivos, y qué
ocurre al restaurarlos, para que el usuario conserve su progreso y sus ajustes sin recuperar un catálogo antiguo.

## ADDED Requirements

### Requirement: El catálogo queda fuera de la copia de seguridad
La app MUST excluir la base del catálogo (`catalog.db`) y sus ficheros auxiliares de SQLite (`catalog.db-wal`,
`catalog.db-shm`, `catalog.db-journal`) de la copia de seguridad en la nube y de la transferencia entre dispositivos, en
todas las versiones de Android que soporta (minSdk 26).

#### Scenario: Copia en la nube en Android 12 o superior
- **GIVEN** las reglas de extracción de datos de la app
- **WHEN** se leen las reglas de la copia en la nube
- **THEN** excluyen `catalog.db`, `catalog.db-wal`, `catalog.db-shm` y `catalog.db-journal` del dominio de bases de datos

#### Scenario: Transferencia entre dispositivos en Android 12 o superior
- **GIVEN** las reglas de extracción de datos de la app
- **WHEN** se leen las reglas de la transferencia entre dispositivos
- **THEN** excluyen `catalog.db`, `catalog.db-wal`, `catalog.db-shm` y `catalog.db-journal` del dominio de bases de datos

#### Scenario: Copia completa en Android 8 a 11
- **GIVEN** las reglas de copia completa de la app
- **WHEN** se leen
- **THEN** excluyen `catalog.db`, `catalog.db-wal`, `catalog.db-shm` y `catalog.db-journal` del dominio de bases de datos

#### Scenario: El manifiesto declara las reglas
- **GIVEN** el manifiesto de la app
- **WHEN** se lee la declaración de la aplicación
- **THEN** la copia de seguridad está permitida y referencia tanto las reglas de extracción de datos como las de copia
  completa

### Requirement: El progreso y los ajustes se copian juntos
La app MUST incluir en la copia en la nube y en la transferencia entre dispositivos la base de progreso
(`practice.db` con `practice.db-wal`, `practice.db-shm` y `practice.db-journal`) y el almacén de ajustes
(`datastore/settings.preferences_pb`), y MUST NOT excluir ninguno de ellos por separado, de modo que una restauración
devuelva ambos o ninguno.

#### Scenario: Ninguna regla excluye el progreso
- **GIVEN** las reglas de copia en la nube, de transferencia entre dispositivos y de copia completa
- **WHEN** se buscan exclusiones que afecten a `practice.db` o a sus ficheros auxiliares
- **THEN** no hay ninguna

#### Scenario: Ninguna regla excluye los ajustes
- **GIVEN** las reglas de copia en la nube, de transferencia entre dispositivos y de copia completa
- **WHEN** se buscan exclusiones que afecten a `datastore/settings.preferences_pb` o a la carpeta `datastore/`
- **THEN** no hay ninguna

#### Scenario: Las reglas no restringen la copia a una lista de inclusión
- **GIVEN** las reglas de copia en la nube, de transferencia entre dispositivos y de copia completa
- **WHEN** se leen
- **THEN** ninguna contiene reglas de inclusión, de modo que todo lo que no está excluido (el progreso y los ajustes)
  entra en la copia

### Requirement: Restauración sin catálogo
Tras restaurar una copia, que contiene el progreso y los ajustes pero no el catálogo, la app MUST sembrar el catálogo
desde los assets del APK instalado en el primer uso, igual que en una instalación nueva, y MUST conservar el progreso y
los ajustes restaurados.

#### Scenario: Primer arranque tras restaurar
- **GIVEN** una base de progreso con progreso guardado y ninguna base de catálogo
- **WHEN** se consulta el catálogo por primera vez
- **THEN** se siembra con las frases de los assets instalados y el progreso guardado no cambia

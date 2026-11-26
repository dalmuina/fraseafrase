# ADR-0031: Política de copia de seguridad

- **Estado:** Propuesto
- **Fecha:** 2026-11-26

## Contexto

La app no tiene backend ni cuentas (ADR-0013), así que la copia de seguridad del sistema (Auto Backup en la nube y
transferencia entre dispositivos) es la única forma de que el usuario no pierda su progreso al cambiar de móvil o
reinstalar. Hasta ahora el manifiesto permitía la copia sin reglas, y Android copiaba todo el almacenamiento interno:

- `catalog.db`: el catálogo de frases, una caché que se siembra desde los assets del APK y se resiembra cuando cambia
  la huella de su contenido (ADR-0028, ADR-0030). La huella se guarda dentro del propio `catalog.db`.
- `practice.db`: progreso por frase, tanda activa y contador de sesiones, por idioma destino. Solo cambia con migración
  (ADR-0030).
- `datastore/settings.preferences_pb`: ajustes por idioma destino y el idioma destino activo.

Restaurar `catalog.db` no aporta nada, porque se recrea, y puede traer un catálogo o un esquema que no corresponde al
APK instalado. Además consume la cuota de copia. El progreso, en cambio, no se puede recrear, y sin los ajustes pierde
su sentido: el idioma destino activo decide qué progreso se muestra.

## Decisión

- La copia de seguridad está activada.
- Se excluyen `catalog.db` y sus ficheros auxiliares de SQLite (`-wal`, `-shm`, `-journal`) de la copia en la nube y de
  la transferencia entre dispositivos, en todas las versiones de Android soportadas.
- `practice.db` (con sus ficheros auxiliares) y los ajustes se copian y se restauran juntos. Ninguna regla excluye uno
  sin el otro.
- Las reglas son solo de exclusión: el estado del usuario entra por defecto. Toda caché o dato recreable nuevo se añade
  a las exclusiones en el mismo cambio que lo crea.
- Tras restaurar, el catálogo se siembra desde los assets en el primer uso, como en una instalación nueva.

## Consecuencias

- Positivas: el usuario conserva progreso y ajustes al cambiar de dispositivo; ninguna restauración deja un catálogo
  ajeno al APK instalado; la copia ocupa menos.
- Negativas: el primer arranque tras restaurar siembra el catálogo completo, igual que una instalación nueva.
- Una copia con ids de frase que el catálogo instalado no tiene haría fallar la composición de la tanda; los ids son
  permanentes desde la publicación (ADR-0013, ADR-0029), lo que lo evita.
- Descartado: listas de inclusión explícitas (un fichero nuevo de estado quedaría fuera sin aviso) y desactivar la
  copia (el usuario perdería su progreso al cambiar de móvil).

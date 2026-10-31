# ADR-0013: Persistencia con catálogo JSON en assets y estado del usuario en Room 3

- **Estado:** Aceptado
- **Fecha:** 2026-10-31
- **Sustituye a:** ADR-0004

## Contexto
El ADR-0004 decidió que el catálogo se distribuye como "base de datos precargada" y que el progreso va en
Room. Al diseñar la persistencia (cambio `add-persistence`) se ha visto que el catálogo es inmutable en tiempo de
ejecución: cambia solo con una versión nueva de la app. Guardarlo en Room obligaría a preparar una base de datos
precargada y a escribir migraciones cada vez que se amplía el contenido, sin que el usuario gane nada.

Además, el ADR-0004 no fijaba cómo se verifica el contenido, cómo se mantienen los ids de las frases ni cómo
se garantiza que el cierre de una tanda sea atómico. Este ADR lo fija.

## Decisión
Este ADR sustituye por completo al ADR-0004. Conserva lo vigente y cambia el almacenamiento del catálogo.

**Se mantiene del ADR-0004:**
- La app funciona completamente sin conexión.
- No hay backend, cuentas de usuario, sincronización en la nube, analítica remota ni llamadas a modelos de IA en
  tiempo de ejecución.
- El catálogo se elabora fuera de la app y se revisa manualmente antes de incluirse.
- Los ajustes se guardan con DataStore.

**Cambia:**
- El catálogo es un documento JSON empaquetado en los assets de `:core:data`, cargado en memoria. No se guarda en
  Room. Su raíz declara `schemaVersion: 1`, `targetLocale` y `sourceLocale` (etiquetas BCP-47), y el array
  `phrases` en orden de catálogo.
- Room 3 (`androidx.room3`) guarda solo el estado del usuario: progreso por frase, tanda activa y cola de
  arrastradas. Su esquema se exporta al repositorio y se versiona. La versión inicial es 1, sin
  `fallbackToDestructiveMigration`.
- El cierre de una tanda es atómico: el puerto de práctica ejecuta la transición en una única transacción de
  escritura de Room, con una función pura del dominio dentro.

**Reglas de ampliación del catálogo:**
- Los ids de las frases son permanentes: no se reutilizan ni se borran.
- Las frases nuevas se añaden al final de la lista.
- Un test de contrato verifica en CI que todos los ids publicados siguen presentes y en el mismo orden.
- Con estas reglas, ampliar el catálogo en una versión futura consiste en editar el JSON, sin migraciones de Room.

**Excepciones que este ADR autoriza:**
- **Línea de estado de ADR-0001:** un ADR aceptado no se edita, salvo una única modificación: el cambio de su línea
  de estado a `Sustituido por ADR-XXXX` cuando se acepta el ADR que lo sustituye. Esta es la única edición
  permitida a un ADR aceptado. Un ADR `Propuesto` puede editarse libremente hasta su aceptación.
- **Detekt:** `UnnecessaryAbstractClass` se configura con `ignoreAnnotated: ['Database']` en
  `config/detekt/detekt.yml`. La clase anotada con `@Database` de Room es abstracta por requisito de la API y
  solo declara miembros abstractos. La exención es por anotación, sin `@Suppress`.
- **Cobertura de `:core:data`:** mínimo del 80 % de líneas. ADR-0007 no fija un umbral para la capa de datos. Las
  clases generadas por Room (`*_Impl`, `*Database_Impl`, `*Dao_Impl`) se excluyen de Kover; excluir código
  generado no baja el umbral.
- **Ajuste de variante (ADR-0003):** la variante activa como ajuste del usuario queda diferida. En el MVP no es
  seleccionable, como dice el ADR-0003 en sus consecuencias. El locale de la raíz del catálogo es la única fuente
  de locales en este cambio.
- **Invariantes en triggers:** Room no expresa `CHECK` en sus anotaciones, así que las invariantes del estado de
  práctica se aplican con triggers `BEFORE INSERT` y `BEFORE UPDATE`. Se crean en `onOpen` con
  `CREATE TRIGGER IF NOT EXISTS`, para que se regeneren en cada apertura, también después de una migración que
  recree tablas. Cambiar la definición de un trigger exige una migración con `DROP TRIGGER` antes de crearlo de
  nuevo, porque `IF NOT EXISTS` no sustituye una definición existente. Un test comprueba en `sqlite_master` que
  existen todos.
- **Versión de KSP:** se usa KSP 2.3.11. La 2.3.13 no estaba publicada en Gradle Plugin Portal cuando se intentó
  (tarea 1.1). Subirla es un cambio aparte, que Dependabot propondrá.

## Consecuencias
- **Positivas:**
  - Ampliar el catálogo no requiere migraciones de Room: basta con editar el JSON y respetar las reglas de ids.
  - El contenido es revisable en diffs de texto.
  - La atomicidad del cierre queda garantizada por una única transacción de escritura, con la transición pura del
    dominio dentro.
  - Los tests de Room corren en JVM local con el mismo SQLite que el dispositivo (`sqlite-bundled`).
- **Negativas:**
  - El catálogo solo crece con nuevas versiones de la app, como ya decía el ADR-0004.
  - Un id publicado que se quitara del catálogo dejaría progreso huérfano. Las reglas de permanencia y el test de
    contrato lo impiden, pero no hay política de descarte: si hiciera falta retirar una frase, requiere un ADR nuevo.
  - La carga del catálogo ocupa memoria. Con el tamaño previsto para el MVP es despreciable.
- **Descartado:**
  - Catálogo como base de datos Room precargada: obliga a migraciones para cada ampliación.
  - Catálogo en un fichero separado por nivel CEFR sin reglas de ids: complica la permanencia de ids.
  - Robolectric para los tests de Room: no es necesario, porque el driver JVM de SQLite permite probarlo localmente.
  - Política de descarte de frases en este cambio: se pospone hasta que haga falta.

# Plan: cerrar la MVP1 (1.0.0) y preparar la MVP2

Estado: **borrador** (2026-11-26). No sustituye a OpenSpec ni a los ADR: cada fila se convierte en un cambio con su
rama, creada por el usuario desde `develop`.

## Principios

- La MVP1 se publica con las bases de datos en **versión 1** y sin migraciones.
- Desde la publicación, `practice.db` solo cambia con migración y test. `catalog.db` es una caché y puede recrearse.
- El paquete de una frase se **deduce de su id**. No se añade ningún campo a la base de datos para ello.
- Los paquetes son temáticos (viajes, negocios…). El campo `topic` actual se mantiene como está.
- Los ids son permanentes desde la publicación: su formato se decide **antes**.

## Decisiones de la MVP1

| # | Decisión (2026-11-26) |
|---|---|
| D1 | Los ids se cambian ya, incluidos los de la base, para que los paquetes futuros no colisionen. Formato: `<bloque>-<nivel>-<tema>-<nnn>`, con `0000` para la base (`0000-a1-saludos-001`) y `1000`, `2000`… para los paquetes |
| D2 | El idioma de origen se detecta por idioma aunque cambie la región: coincidencia exacta; si no, el mismo idioma (`en-US` → `en-GB`, `es-MX` → `es-ES`); si no, `es-ES` |
| D3 | La app se traduce a todos los idiomas del catálogo: es, pt, en, fr, de, it, ru |
| D7 | Las frases nuevas se eligen al azar dentro del nivel CEFR seleccionado, no en orden de catálogo |
| D8 | El filtro futuro usará un registro de **categorías y subcategorías**. La subcategoría es el `topic` actual y cada una pertenece a una categoría. La categoría no va en el id ni en la frase. Nombres visibles en `strings.xml`. Se implementa después de la MVP1 |

Sobre D2: el historial no muestra que la detección por idioma llegara a implementarse. Las tres versiones de
`ResolveSourceLocale` (`f98b409`, `6010123`, `27cfd96`) comparan la etiqueta exacta. Quedó planificada como
`device-language-by-base-language` en `remove-pt-br` ("Fuera de alcance") y no se hizo.

## MVP1: cerrar y depurar (1.0.0)

En este orden: A1 (resembrado), A2 (ids nuevos) y A3 (versión 1) van seguidos.

| # | Cambio (rama `1.0.0/…`) | Tipo | Contenido | Hecho cuando |
|---|---|---|---|---|
| A1 | `fix-catalog-reseed` | normal | Resembrar cuando cambie la huella del contenido de cada documento, no `schemaVersion` (ADR-0028). Delta de `phrase-catalog` | Un test añade frases a un catálogo ya sembrado, las ve, y el progreso no cambia |
| A2 | `renumber-phrase-ids` | normal | ADR con el formato de id (D1). Renombrar con script los 7 assets, `released-ids`, tests y escenarios (por delta). Test de contrato del formato y de la lista cerrada de temas (hoy 114) | Los 7 × 1000 ids cumplen el formato y siguen siendo paralelos |
| A3 | `reset-database-v1` | normal | Ambas bases a `version = 1`, esquemas regenerados, fuera `PracticeMigrationTest` y el escenario "Una base anterior se abre vacía". Fuera `fallbackToDestructiveMigration()` en `practice.db`. ADR con la política de migraciones | `check` en verde y solo existe `1.json` por base |
| A4 | `configure-backup-rules` | ligero | Excluir `catalog.db` de la copia de seguridad y decidir `practice.db` y ajustes | Restaurar una copia no deja un catálogo antiguo |
| A5 | `check-duplicate-phrases` | ligero | Test de texto repetido por idioma, script para casi iguales y corrección de los repetidos actuales (`en-GB`, `fr-FR`, `ru-RU`) | Test en verde sin excepciones injustificadas |
| A6 | `match-source-language` | normal | Regla D2 en `ResolveSourceLocale`. ADR nuevo que sustituye esa regla del 0023 | Escenarios `en-US`, `es-MX`, `pt-BR` e idioma sin catálogo cubiertos |
| A7 | `pick-new-phrases-randomly` | normal | D7. ADR que modifica el orden de las nuevas (0025/0027). La aleatoriedad entra en el dominio por un puerto, como el reloj | Tests deterministas; la tanda guardada no cambia al reabrir |
| A8 | `localize-interface` | normal | D3. `values-{pt,en,fr,de,it,ru}`; `values/` sigue en español. Mover a recursos las insignias de `DsLevelBadge`. Rehacer "Te faltan N aciertos…" (hoy en dos trozos) con plurales | Sin textos fijos y sin `MissingTranslation` |
| A9 | `remove-hardcoded-locales` | vía rápida | Valores `pt-PT` por defecto en los `UiState` y ViewModels. Si cambia comportamiento, pasa a ligero | Sin literales de idioma en el estado de las features |
| A10 | `prepare-release-1-0` | ligero | Icono de la app (el manifiesto no declara ninguno), `versionName = 1.0.0`, prueba de `release` con R8 en dispositivo | APK firmado probado de principio a fin |
| A11 | `freeze-released-ids` | contenido | Rellenar los siete `released-ids-*.txt` con los ids nuevos. **Último cambio antes de publicar** | El contrato protege las 7 × 1000 frases |

### Avisos

- **A2 invalida el progreso guardado.** Con ids antiguos en `practice.db`, `ComposeNextBatch` lanza una excepción
  ("Some progress belongs to phrases missing from the catalog"). Desinstalar o borrar datos tras A2 y A3.
- **A8 y A6 deben coincidir.** Android elige los textos con toda la lista de idiomas del dispositivo; el idioma de
  origen usa solo el primero. A6 debe decidir si usa también la lista.
- **A8:** las traducciones de la interfaz necesitan revisión, sobre todo los plurales en ruso.

### Comprobaciones sin código antes de publicar

- Revisión lingüística de las traducciones, sobre todo `pt-PT`.
- Audio en un dispositivo sin voz instalada para el idioma destino (no verificado).
- Política de privacidad, ficha de Play, cuenta de Play Console y pista de pruebas.
- Copia de seguridad del keystore (`docs/release.md`).

## Después de la MVP1: categorías

Registro de categorías (id estable) con sus subcategorías (`topic`), y filtro en la app. No requiere cambiar ids ni
`practice.db`. Al crearlo, revisar temas muy parecidos (`mascotas` / `mascotas_cuidado`, `ropa` / `ropa_compras`).
El bloque del id dice de qué paquete viene una frase; la categoría, de qué trata.

## MVP2: paquetes temáticos de pago (1.1.0)

**Aplazada** hasta resolver la MVP1. Las decisiones D4 (entrega), D5 (identificación) y D6 (entrada de un paquete
en la tanda) siguen abiertas. D7 cubre parte de D6: las frases de un paquete competirían al azar con las nuevas de
la base desde la primera tanda.

| # | Cambio | Contenido |
|---|---|---|
| B1 | ADR de paquetes | Entrega (D4), identificación (D5), entrada en la tanda (D6), compra sin servidor, reembolsos |
| B2 | `add-phrase-packs-catalog` | Ficheros por paquete e idioma, siembra por (paquete, idioma), orden estable entre ficheros, paquete deducido del id, contratos de ids paralelos por paquete |
| B3 | `add-pack-composition` | Solo entran frases desbloqueadas (tanda y progreso). Reglas de D6, ADR que modifica el 0027 |
| B4 | `add-pack-indicators` | Componente `Ds` con color **e** icono o texto (el color solo no basta). Marca "Nueva" a partir de `last_answered_session` vacío |
| B5 | `add-pack-purchases` | Play Billing, puerto de derechos, caché sin conexión, compra pendiente, confirmación, restauración y reembolso |
| B6 | `add-pack-store` | `:feature:store` y navegación |
| B7 | Contenido del primer paquete | 1000 frases × 7 idiomas, repartidas entre A1–C1, sin duplicados contra la base |

### Por qué la versión 1 aguanta la MVP2

- El paquete sale del id, y el progreso ya se guarda por (id, idioma): `practice.db` no necesita cambios.
- La marca "Nueva" usa columnas que ya existen.
- Los derechos de compra se cachean fuera de Room (DataStore).
- Lo que cambie en `catalog.db` es recreable.

### Riesgos a resolver en B1

- El filtro CEFR es de nivel exacto (ADR-0021): un paquete sin reparto por niveles queda invisible para parte de los
  usuarios.
- Con 1000 frases por bloque, decidir si un paquete puede crecer después de venderse.
- Cada idioma nuevo obliga a traducir la base y todos los paquetes.

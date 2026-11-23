## Context

`pt-BR` se añadió como segundo idioma destino con contenido propio (ADR-0019). El catálogo son documentos
`phrases-<idioma>.json` en `core/data/src/main/assets/catalog/`, que `RoomCatalogDataSource` descubre con
`assets.list("catalog")`: no hay una lista de idiomas en código de producción, así que borrar el documento basta
para que `pt-BR` deje de estar disponible. La única referencia de producción a `pt-BR` está en la presentación:
`DsLanguageBadge` (icono) y `LanguageTagDsLanguageOptionMapping` (etiqueta), más las previews de ajustes. El resto
son tests y specs que usaban `pt-BR` como "segundo idioma destino" para probar aislamiento entre idiomas.

Decisión del usuario: no hay datos de usuario que conservar, así que no hay migración. La rama
`1.0.0/backup-pt-br-catalog` conserva el estado previo.

## Goals / Non-Goals

**Goals:**
- `pt-BR` deja de estar disponible como idioma destino: sin catálogo, ids publicados, icono ni etiqueta.
- Las specs y los tests conservan el aislamiento entre idiomas destino usando otro idioma.

**Non-Goals:**
- Migrar o borrar filas `pt-BR` de Room o DataStore; cambiar esquemas (`CatalogDatabase` y `PracticeDatabase`
  no cambian, sin nueva versión).
- Resolver el origen por idioma base (segundo cambio, `device-language-by-base-language`).

## Decisions

1. **Sin ADR nuevo.** ADR-0003 (modelo multivariante, etiquetas BCP-47, sin valores de idioma fijos en el
   dominio) sigue vigente: solo cambia qué documentos se distribuyen. ADR-0019/0020 están sustituidos. ADR-0009,
   0012 y 0015 ya listan `pt-BR` como fuera de alcance. No se edita ningún ADR aceptado.
2. **Segundo idioma de ejemplo = `en-GB`.** `es-ES` no sirve: es el origen por defecto y no puede ser destino
   (specs `user-settings` y `settings-screen`). `en-GB` existe en el catálogo, tiene icono y etiqueta y no es el
   origen en los escenarios afectados. Si un test concreto fija `en-GB` como origen, usar `de-DE`.
3. **Idioma guardado que desaparece.** El requisito "Idioma destino activo" ya cubre un guardado no disponible
   (cae al primero disponible). Ese escenario se conserva con `pt-BR` literal como idioma retirado: es la
   regresión de esta retirada y no necesita código nuevo. Los tests de dominio que lo ejercitan
   (en `:core:data`, `DataStoreSettingsDataSourceTest`: "pt-BR is no longer available then returns the first available") mantienen `pt-BR` solo en ese caso.
4. **Contrato de catálogo (`RealCatalogContractTest`).** Se quita `pt-BR` de `PACKAGED_LOCALES` y del
   `@ValueSource`, se elimina su test de "sin ids publicados" (lo cubre el de `es-ES`) y se borra
   `src/test/resources/catalog/released-ids-pt-BR.txt`. Se añade el test del requisito nuevo "Portugués europeo
   como única variante de portugués", que falla mientras exista `phrases-pt-BR.json`.
5. **Presentación.** `DsLanguageBadge` pierde la rama `"pt-BR"`, su entrada en `AllLanguageCodes` y el drawable
   `ic_flag_pt_br`; un código sin icono sigue el comportamiento actual de código desconocido.
   `LanguageTagDsLanguageOptionMapping` pierde `"pt-BR" -> "Português (Brasil)"`. Las previews usan `en-GB`.

## Módulos y dependencias

Sin clases nuevas ni cambios en dependencias entre módulos (ADR-0010). Se tocan `:core:data` (asset, ids
publicados y tests), `:core:domain` (fixtures), `:core:designsystem`, `:feature:settings`, `:feature:session` y
`:feature:progress` (tests). Sin puertos, fakes ni librerías nuevos.

## Estrategia de test

- `:core:data`: test de contrato rojo antes de borrar el asset; el resto de tests de datos usan `en-GB` como
  segundo idioma (fixtures, sin lógica nueva).
- `:core:domain` y features: ajuste de fixtures `pt-BR` → `en-GB` con los tests existentes en verde; no hay
  comportamiento nuevo. Los `androidTest` (Ds y ajustes) se actualizan pero no se ejecutan en `check`.

## Risks / Trade-offs

- [Dispositivo con `pt-BR` activo guardado] → cae al primer idioma disponible (spec existente); su progreso
  `pt-BR` queda huérfano en Room y se ignora. Aceptado por decisión del usuario.
- [Fixtures `pt-BR` que no eran de "segundo idioma"] → revisar caso a caso; si el test solo necesita una etiqueta
  distinta de `pt-PT`, usar `en-GB`.

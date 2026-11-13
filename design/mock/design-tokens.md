# Design tokens · App de frases

Fuente: mock «App de frases – Mock» (5 pantallas de 390×844). **1 px del mock = 1 dp** (y 1 sp para texto). Todos los valores salen del código del mock; nada es aproximado.

## Colores

| Token | Hex | Uso |
|---|---|---|
| `accent` | `#5B3DF5` | Botón primario, chip de nivel, barra de progreso, botón Escuchar, icono activo del nav, tarjeta de Progreso |
| `accentStrong` | `#3A1FC2` | Texto sobre `accentSoft` (contador, etiqueta TRADUCCIÓN, +/−), hover de enlaces |
| `accentSoft` | `#ECE7FF` | Pastilla del contador, indicador del tab activo, panel de traducción, botones +/−, textos claros sobre `accent` |
| `background` | `#F7F5FF` | Fondo de pantalla y del campo numérico |
| `surface` | `#FFFFFF` | Tarjetas y barra de navegación |
| `textPrimary` | `#14171F` | Texto principal, `<strong>`, etiqueta del tab activo |
| `textSecondary` | `#5A5670` | Texto de ayuda y etiquetas inactivas del nav en Practicar/Sesión |
| `textSecondaryAlt` | `#5A6070` | Igual que el anterior, pero en Progreso y Ajustes (el mock no es coherente aquí: **unificar en `#5A5670`** salvo que se decida lo contrario) |
| `textOnLevel` | `#3A404D` | Botón de nivel no seleccionado |
| `track` | `#E4DFF5` | Fondo de la barra de progreso, borde del nav, bordes de botones de nivel y del campo |
| `trackSoft` | `#EFECF8` | Barras de rango, separadores de lista, botón − deshabilitado |
| `dashedBorder` | `#D3CCF0` | Borde discontinuo de la zona «Traduce la frase…» |
| `disabled` | `#8A909C` | Icono deshabilitado, «Sin rango» |
| `fab` / `fabContent` | `#FF7A50` / `#2B0E02` | Botón flotante de volver a la frase anterior |
| `hintBg` / `hintText` | `#FFF1EA` / `#5C2A12` | Mensaje «Te faltan N aciertos…» |
| `success` / `successBg` / `successText` | `#0B8A4A` / `#E3F6EC` / `#08693A` | Botón Acierto, estadística Aciertos |
| `successStatus` | `#1B6B44` | «Ajustes guardados» |
| `error` / `errorBg` / `errorText` | `#D92D3F` / `#FFECEE` / `#B4232F` | Botón Fallo (borde, fondo, texto), estadística Fallos |
| `rankUpText` | `#9A3412` | Estadística «Suben de rango» (fondo `hintBg`) |

### Rangos (insignia hexagonal de dos tonos: exterior / interior)

| Rango | Exterior | Interior | Chip (texto / fondo) | Barra de Progreso |
|---|---|---|---|---|
| Sin rango | círculo discontinuo `#8A909C`, trazo 2, guion 3/3 | — | — | `#8A909C` |
| Bronce | `#B4693A` | `#E3A47C` | (no aparece en el mock) | `#B4693A` |
| Plata | `#8E98A5` | `#D5DAE1` | `#3D434D` / `#EEF0F3` | `#8E98A5` |
| Oro | `#D9A21B` | `#F4CF6B` | `#6B4A06` / `#FBF1D6` | `#D9A21B` |
| Platino | `#3E9C9C` | `#A9DCDC` | (no aparece en el mock) | `#3E9C9C` |
| Diamante | `#4F7DF0` + facetas `#C9D8FF` (trazo 1,2) | — | (no aparece en el mock) | `#4F7DF0` |

> El icono del mensaje «Te faltan N aciertos para subir a **X**» es la insignia del rango **X** (el siguiente), **no** una estrella ni el rango actual.

## Tipografía

Fuentes: **Space Grotesk** (títulos, frases, números) e **Inter** (resto). Ambas de Google Fonts.

| Rol | Fuente | Tamaño | Peso | Interlineado | Tracking |
|---|---|---|---|---|---|
| Título de pantalla | Space Grotesk | 28 | 700 | — | −0,5 |
| Frase a traducir (corta) | Space Grotesk | 28 | 600 | 1,25 | −0,3 |
| Frase a traducir (larga) | Space Grotesk | 22 | 600 | 1,3 | 0 |
| Título «¡Has completado la sesión!» | Space Grotesk | 28 | 700 | — | −0,4 |
| Número grande (Progreso) | Space Grotesk | 48 | 700 | 1,1 | 0 |
| Número de estadística / campo numérico | Space Grotesk | 24 | 700 | — | 0 |
| Botón primario / Fallo / Acierto | Inter | 17 | 600 | — | 0 |
| Traducción | Inter | 17 | 500 | 1,4 | 0 |
| Título de sección (Ajustes) | Inter | 17 | 600 | — | 0 |
| Contador «Frase 3 de 10» | Inter | 15 | 600 | — | 0 |
| Nombre de rango / «Ver mi progreso» | Inter | 15 | 600 | — | 0 |
| Texto de ayuda / mensaje de rango | Inter | 14 | 400 | 1,4–1,5 | 0 |
| Chips de nivel y rango | Inter | 13 | 700 / 600 | — | 0 |
| Etiqueta «TRADUCCIÓN» | Inter | 12 | 700, mayúsculas | — | +0,8 |
| Etiqueta del nav | Inter | 12 | 700 activo / 500 inactivo | — | 0 |

## Radios

| Elemento | Radio |
|---|---|
| Tarjeta principal (frase, Progreso, Ajustes, Sesión) | 24 |
| Panel de traducción, zona discontinua | 20 |
| Botón flotante de volver | 18 |
| **Botón primario, Fallo, Acierto** | **16** (no son píldoras) |
| Estadísticas de sesión | 16 |
| Mensaje de rango, botones de nivel, +/−, campo numérico | 14 |
| Chips de nivel y rango | 8 |
| Contador, barra de progreso, indicador del tab activo, botón Escuchar | completo (píldora / círculo) |

## Tamaños y espaciado

- **Cabecera**: padding 24 arriba, 20 lados, 12 abajo; 14 entre título y barra. Barra de progreso de **8** de alto.
- **Contenido**: padding 8 arriba, 20 lados, 20 abajo; 16 entre bloques (12 en «traducción mostrada»).
- **Tarjeta de frase**: padding 20 y 20 entre filas (oculta); padding 14/14/18/18 y 12 entre filas (mostrada).
- **Chips**: padding 5×10 (el de rango 5/10/5/6), 8 entre chips, insignia de 18 y 6 de separación.
- **Mensaje de rango**: padding 12×14, 10 entre icono y texto, icono de **24** (22 en la versión compacta: padding 10×12).
- **Botón primario**: alto **56**, icono 22, 10 entre icono y texto. Sombra `0 6 16 rgba(91,61,245,.30)`.
- **Botones Fallo/Acierto**: 56 de alto, borde 2, 12 entre ellos, icono 22 con 8 de separación.
- **Botón flotante de volver**: 56×56, a 20 de la izquierda y **172** del borde inferior de la pantalla (encima del botón primario), icono 24. Sombra `0 6 16 rgba(255,122,80,.40)`.
- **Botón Escuchar**: 44×44 circular, icono 22.
- **Nav inferior**: alto 80, borde superior 1 `track`, padding lateral 8, 3 columnas iguales. Indicador del tab activo 64×32 con 4 entre icono y etiqueta; iconos de 22.
- **Ajustes**: botones de nivel 52 de alto en rejilla de 5 con 8 de separación; +/− y campo de 52.
- **Fila de rango (Progreso)**: icono 40, 14 de separación, padding 12 vertical, barra de 6.

## Sombras

- Tarjeta (Practicar/Sesión): `0 1 2 rgba(58,31,194,.08)` + `0 8 24 rgba(58,31,194,.08)`.
- Tarjeta (Progreso/Ajustes): `0 1 2 rgba(20,23,31,.06)` + `0 4 16 rgba(20,23,31,.05)`.
- En Compose no hay sombras de color multicapa nativas: usar `Modifier.shadow(elevation, shape, ambientColor, spotColor)` con el color indicado, o un `drawBehind` con `BlurMaskFilter` si se quiere exactitud.

## Iconos

Todos en `icons/` (SVG fuente + VectorDrawable). **No usar Material Icons en estas pantallas.**

| Icono | Fichero | Trazo | Dónde |
|---|---|---|---|
| Ojo | `ic_eye` | 2 | Botón «Mostrar traducción» |
| Flecha circular antihoraria | `ic_undo_previous` | 2,2 | Botón flotante de volver |
| Flecha circular horaria | `ic_new_session` | 2,2 | «Generar sesión nueva» |
| Altavoz | `ic_volume` | 2 | Botón Escuchar |
| Aspa | `ic_close` | 2,4 | Fallo |
| Check | `ic_check` / `ic_check_save` / `ic_check_complete` | 2,4 / 2,2 / 2,6 | Acierto / Guardar / Sesión completada |
| Menos / Más | `ic_minus` / `ic_plus` | 2,4 | Ajustes |
| Dos cuadrados superpuestos | `ic_nav_practicar` | 2 | Nav · Practicar |
| Barras | `ic_nav_progreso` | 2 | Nav · Progreso |
| Deslizadores | `ic_nav_ajustes` | 2 | Nav · Ajustes |
| Insignias de rango | `ic_rank_*` | — | Chips, mensaje de rango, Progreso |
| Hexágono liso | `ic_hex_solid` | — | Decoración (Sesión completada, tarjeta de Progreso) |

Iconos de trazo: monocromos, se tiñen con el color del contenido. Insignias: **multicolor, sin tinte** (`Image(...)` o `Icon(..., tint = Color.Unspecified)`).

# Referencia de diseño · App de frases

Este directorio es la **fuente de verdad visual** de la app. Cópialo al repo (por ejemplo en `design/mock/`) y referéncialo desde los changes de OpenSpec.

## Contenido

| Ruta | Qué es |
|---|---|
| `screens/*.html` | Las 5 pantallas del mock en HTML estático (390×844). Se abren en cualquier navegador y enlazan entre sí. **Es lo que debe leer el modelo**: cada valor (color, radio, tamaño, icono) está escrito en el código. |
| `reference-png/*.png` | Render de cada pantalla a 2x (780×1688). Para comparación visual y como imagen de referencia en tests de captura. |
| `design-tokens.md` | Colores, tipografía, radios, espaciado, sombras e iconos, con su uso. |
| `icons/svg/` | Iconos fuente del mock (viewBox 24). |
| `icons/android-drawable/` | Los mismos iconos como VectorDrawable, listos para `res/drawable/`. |
| `compose/DesignTokens.kt` | Tokens como constantes de Jetpack Compose. Ajusta el `package`. |

Pantallas:

1. `01-practicar-oculta.html`: frase sin traducción.
2. `02-practicar-traduccion.html`: traducción mostrada, con Fallo/Acierto y botón Escuchar.
3. `03-sesion-completada.html`
4. `04-progreso.html`
5. `05-ajustes.html` (estado inicial: B1 seleccionado, 10 frases; el texto «Ajustes guardados» solo aparece tras pulsar Guardar).

## Reglas para el modelo (pegar en `openspec/project.md` o en el `design.md` del change)

```
- La fuente de verdad visual es design/mock/screens/*.html y design/mock/design-tokens.md.
  Las capturas PNG son solo apoyo. Si una captura y el HTML discrepan, manda el HTML.
- Todos los valores (colores, radios, tamaños, paddings, pesos de fuente) se toman
  literalmente del HTML: 1 px = 1 dp / 1 sp. Prohibido redondear o sustituir por
  valores por defecto de Material 3.
- Prohibido usar Material Icons (Icons.Filled.*, Icons.Outlined.*, etc.) en estas
  pantallas. Usar solo los drawables de design/mock/icons/android-drawable/,
  copiados a res/drawable/ con el mismo nombre.
- Las insignias de rango (ic_rank_*) son multicolor: se pintan con Image() o con
  tint = Color.Unspecified. Nunca con el tint por defecto de Icon().
- El icono del mensaje «Te faltan N aciertos para subir a X» es la insignia del rango X.
- Los botones primarios tienen radio 16 dp, no forma de píldora.
- Componentes de Material 3 (NavigationBar, Button…) solo si se sobrescriben sus
  colores, formas, alturas e indicadores para que coincidan con el mock; si no,
  componentes propios.
- Cada tarea de UI termina comparando la pantalla con su PNG de referencia.
```

## Cómo dividir las tareas en `tasks.md`

Una tarea por componente, citando el fragmento HTML correspondiente:

- [ ] Tokens: copiar `compose/DesignTokens.kt` y los drawables; añadir las fuentes Inter y Space Grotesk a `res/font/`.
- [ ] Cabecera de Practicar (título, contador en píldora, barra de 8 dp).
- [ ] Tarjeta de frase: chip de nivel, chip de rango con insignia, frase, mensaje de rango con la insignia del rango siguiente.
- [ ] Zona discontinua «Traduce la frase mentalmente…».
- [ ] Botón primario (56 dp, radio 16, `ic_eye`, sombra de color).
- [ ] Botón flotante de volver (56×56, radio 18, `#FF7A50`, `ic_undo_previous`, a 172 dp del fondo).
- [ ] Barra de navegación (80 dp, indicador 64×32 `#ECE7FF`, etiqueta activa `#14171F` en peso 700).
- [ ] Pantalla con traducción: botón Escuchar, panel de traducción, Fallo/Acierto.
- [ ] Sesión completada, Progreso y Ajustes.
- [ ] Tests de captura (Roborazzi/Paparazzi) de cada pantalla a 390×844 dp, comparados con `reference-png/`.

## Notas sobre el mock

- El gris de las etiquetas inactivas del nav es `#5A5670` en Practicar y `#5A6070` en Progreso/Ajustes. Probablemente es un descuido: conviene unificarlo.
- El círculo discontinuo de «Sin rango» no puede ser un VectorDrawable (no admite trazo discontinuo). Está en `icons/svg/ic_rank_sin_rango.svg` y se dibuja en Compose con `PathEffect.dashPathEffect(floatArrayOf(3f, 3f))` escalado al tamaño (ver `DesignTokens.kt`).
- Los textos del mock (frases, cifras) son de ejemplo; los datos reales vienen de la app.

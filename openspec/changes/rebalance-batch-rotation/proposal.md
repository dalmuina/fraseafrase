# Proposal

## Why

Al probar la app, el usuario se queda atascado siempre en las mismas frases, incluso cuando ya están en Oro,
y en cada sesión solo ve 2 frases nuevas de 10, al final de la tanda. Estas son las causas en las reglas
vigentes (ADR-0017 y ADR-0021):

- Un acierto que no certifica devuelve la frase en la sesión siguiente, sea cual sea su nivel. Para subir de
  nivel hacen falta 3 aciertos, así que una frase Oro aparece en 3 sesiones seguidas: los niveles altos no se
  espacian más que los bajos.
- El bloque de dominadas (25 % de N) vuelve a ofrecer, antes de que les toque, frases de nivel 4 y 5, y siempre
  las primeras del catálogo. Si se aciertan ahí, su vencimiento pasa a la sesión siguiente y vuelven a
  "a reforzar".
- Las frases a reforzar se ordenan por catálogo: siempre ganan las mismas y las del final esperan sin límite.
- Las nuevas son un 20 % fijo de N y van siempre al final de la tanda.

El usuario quiere que la repetición siga siendo la base, pero que las frases cambien más; que los niveles
altos se vean menos; y que la tanda no se rehaga con un porcentaje fijo de nuevas en cada sesión.

## What Changes

- **Espera tras un acierto que no certifica, según el nivel.** Se mantienen los 3 aciertos para subir de
  nivel. El `dueAtSession` pasa a ser la sesión actual más una espera que crece con el nivel: 1 sesión en los
  niveles 0, 1 y 2; 2 en el 3; 4 en el 4; y 8 en el 5. No cambian ni el enfriamiento tras certificar
  (1/2/4/8/16) ni el fallo (sesión siguiente, ADR-0024).
- **BREAKING: se elimina el bloque de dominadas y las cuotas fijas.** La tanda se compone con dos tipos de
  frase, siempre del nivel CEFR exacto elegido (ADR-0021):
  - **Pendientes**, ordenadas por la prioridad `nivel − sesiones de retraso`. Una frase fallada (baja de nivel)
    o que lleva tiempo esperando gana puestos.
  - **Nuevas, según cómo va el aprendizaje.** Entran más cuando las frases de nivel 0 se consolidan y menos
    cuando se acumulan:
    - Hasta el 40 % de N.
    - Hasta la mitad de N si no hay ninguna frase pendiente (por ejemplo, en la primera tanda de un nivel CEFR).
- **Las nuevas se reparten a lo largo de la tanda,** no todas al final.
- **ADR-0025 nuevo**, "Rotación de la tanda por prioridad y aprendizaje". Sustituye a ADR-0017 en la regla del
  acierto que no certifica y en la composición por cuotas con bloque de dominadas. Se crea como `Propuesto` y
  pasa a `Aceptado` al cerrar el cambio.

ADR afectados:
- ADR-0017: sustituido en parte.
- ADR-0021: sigue vigente; su filtro exacto se aplica a los dos tipos de frase que quedan.
- ADR-0024: sin cambios.

Respeta el alcance del MVP (ADR-0012 y ADR-0018): cambia reglas del método, no añade funcionalidad.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `spaced-repetition`: el vencimiento tras un acierto que no certifica pasa a depender del nivel.
- `practice-batch`: la composición de la tanda nueva cambia: sin cuotas ni dominadas, con prioridad de
  pendientes, nuevas según el aprendizaje y nuevas repartidas. También cambia la primera tanda, que se llena
  hasta la mitad de N.
- `practice-session`: el orden de la tanda siguiente al generar sesión (pendientes por prioridad, nuevas
  repartidas).
- `practice-persistence`: la primera tanda de un idioma destino se llena hasta la mitad de N, no hasta N.

## Fuera de alcance

- Cambiar el número de aciertos para certificar (3), el enfriamiento tras certificar o la regla de fallo.
- Guardar un contador de errores o cualquier campo nuevo en Room. No hay migración.
- Limitar las autoevaluaciones a una por frase y sesión: la pantalla ya lo garantiza, porque al completar la
  tanda deja de ofrecer frases y las respondidas pasan al final de la lista (ver design.md).
- Cambios visuales, de textos o de Ajustes (N sigue entre 10 y 30).
- Hacer configurables las proporciones (40 % y 50 %).

## Impact

- `:core:domain`:
  - `ReviewSchedule`: nueva espera intra-nivel.
  - `PhraseProgress.recordHit`.
  - `ComposeNextBatch`: se reescribe la composición.
  - `BatchComposition`: solo el KDoc.
  - Sus tests, y los de casos de uso que fijan el orden o el tamaño de la tanda compuesta.
- `:feature:session` y `:core:data`: solo tests que dependen del orden o del tamaño de la tanda compuesta. Sin
  cambios de código de producción previstos.
- `docs/adr/`: ADR-0025 nuevo; al cerrar, la línea de estado de ADR-0017 y la tabla del README.
- Sin librerías nuevas y sin cambios de esquema.

# ADR-0025: Rotación de la tanda por prioridad y aprendizaje

- **Estado:** Aceptado
- **Fecha:** 2026-11-24
- **Sustituye a:** ADR-0017 (parcialmente, en dos reglas: el `dueAtSession` de un acierto que no certifica y la
  composición de la tanda nueva por cuotas con bloque de dominadas). El filtro CEFR exacto de ADR-0021 y la
  regla de fallo de ADR-0024 siguen vigentes.

## Contexto

Al probar la app, el usuario se queda atascado en las mismas frases, incluso en Oro, y ve pocas frases
nuevas, siempre al final de la tanda. Con las reglas de ADR-0017:

- Un acierto que no certifica devuelve la frase en la sesión siguiente, en cualquier nivel. Como hacen falta
  3 aciertos por nivel, una frase de nivel alto aparece tan a menudo como una de nivel bajo hasta que
  certifica.
- El bloque de dominadas (resto de N, un 25 % con N = 20) vuelve a ofrecer frases de nivel 4 y 5 antes de su
  vencimiento, siempre las primeras del catálogo. Un acierto ahí las devuelve a "a reforzar" en la sesión
  siguiente.
- "A reforzar" se ordena por catálogo: siempre entran las mismas frases y las del final pueden no entrar nunca.
- Las nuevas son un 20 % fijo, al final de la tanda.

## Decisión

Se mantienen las reglas vigentes de ADR-0017 (con ADR-0018, ADR-0021 y ADR-0024) salvo en lo siguiente.

### Espera tras un acierto que no certifica

El `dueAtSession` pasa a ser la sesión actual más la **espera intra-nivel** del nivel actual: la mitad del
enfriamiento de ese nivel, con mínimo 1.

| Nivel actual | 0 | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|---|
| Espera (sesiones) | 1 | 1 | 1 | 2 | 4 | 8 |

No cambian: la certificación con 3 aciertos, el enfriamiento tras certificar (1/2/4/8/16) y el fallo (sesión
siguiente, ADR-0024).

### Composición de la tanda nueva

Se eliminan las cuotas fijas y el bloque de dominadas. Con N el tamaño de la tanda y solo frases del nivel
CEFR elegido:

1. **Pendientes** (`dueAtSession ≤ sesión actual`), ordenadas por `nivel − sesiones de retraso`, de menor a
   mayor. Desempata el orden de catálogo.
2. **Nuevas:** `min(disponibles, techo, max(hueco, N − pendientes))`, donde:
   - `techo` es `floor(N × 0,4)`, o `floor(N × 0,5)` si no hay pendientes (por ejemplo, la primera tanda de un
     nivel CEFR);
   - `hueco` es `max(0, floor(N × 0,5) − frases de nivel 0 con progreso del nivel CEFR elegido, estén o no
     pendientes)`.
3. Las pendientes llenan el resto hasta N.
4. Las nuevas se reparten entre las pendientes, en la posición `floor((i + 1) × total / (k + 1))`, y nunca
   ocupan la última posición si hay alguna pendiente.

Una frase con progreso que aún no está pendiente no entra en la tanda, sea cual sea su nivel.

## Consecuencias

- **Positivas:**
  - Los niveles altos se ven menos, también mientras acumulan los 3 aciertos para subir.
  - Las frases rotan: una frase fallada (baja de nivel) o que lleva tiempo esperando gana prioridad, y ninguna
    espera sin límite.
  - Entran más nuevas cuando el aprendizaje va bien y menos cuando se atasca. Nunca hay un porcentaje fijo por
    sesión.
  - No hay cambios de esquema ni migración: todo se calcula con el progreso que ya se guarda.
- **Negativas:**
  - La tanda puede salir menor que N cuando hay pocas pendientes.
  - Se pierde el repaso ligero de frases dominadas antes de su vencimiento.
  - Las reglas de composición son algo menos intuitivas que unas cuotas fijas.
- **Descartado:**
  - Bajar a 1 acierto por nivel a partir de Bronce: el usuario prefiere mantener los 3 aciertos.
  - Subir el porcentaje fijo de nuevas: seguiría metiendo nuevas aunque el aprendizaje se atasque.
  - Limitar las autoevaluaciones a una por frase y sesión: la pantalla ya lo garantiza.
  - Contar los errores en un campo persistido: exigiría migración, y la prioridad por nivel y retraso ya
    refleja los fallos.

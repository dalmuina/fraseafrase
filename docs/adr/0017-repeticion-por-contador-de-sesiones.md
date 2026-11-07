# ADR-0017: Repetición por contador de sesiones, sin cola de arrastradas

- **Estado:** Sustituido por ADR-0018
- **Fecha:** 2026-11-06
- **Sustituye a:** ADR-0016 (parcialmente). ADR-0015 ya está `Sustituido por ADR-0016`; este ADR no lo toca
  de nuevo, solo hereda y mantiene vigentes sus reglas de racha/nivel vía ADR-0016.

## Contexto

La prueba manual de ADR-0016 reveló dos fallos: (1) cambiar el nivel CEFR en Ajustes podía mezclar frases de
niveles muy distintos, porque solo se filtraba por CEFR el bloque de frases nuevas; (2) ninguna frase llegaba
nunca a certificarse, porque la racha se reiniciaba a 0 en cada tanda compuesta y cada tanda solo ofrece cada
frase una vez: racha 3 (el umbral de certificación) era matemáticamente imposible. Además, medir el repaso en
días naturales (ADR-0015) no tiene sentido para una app donde el usuario puede generar muchas sesiones en un
mismo día: una frase que sube de nivel hoy no podría volver a aparecer como pendiente hasta "mañana", sea
cuantas sesiones se generen hoy.

## Decisión

Se mantienen sin cambios, de ADR-0012/ADR-0015/ADR-0016: la lista circular, la certificación por racha ≥ 3, la
bajada instantánea de nivel ante un fallo, el modelo de "generar sesión" en un paso y el mensaje de tanda
completada.

Cambian: la base temporal del repaso (sesiones en vez de días), el momento en que se resuelve cada
autoevaluación (al instante, no al generar sesión), el alcance del filtro CEFR, las cuotas de composición, y
se retira la cola de arrastradas.

### Conceptos nuevos o modificados

| Término | Significado |
|---|---|
| **Sesión generada** | Cada vez que "generar sesión" compone una tanda nueva. Un contador global, persistido, cuenta cuántas se han generado. |
| **`dueAtSession`** | Sesión a partir de la cual una frase vuelve a estar disponible para "a reforzar". Sustituye a la fecha de próximo repaso (`nextReview`); no está ligado al nivel, así que una frase de nivel 0 también puede tenerlo. |
| **Enfriamiento** | Número de sesiones que debe esperar una frase recién certificada antes de volver a ofrecerse, creciente con el nivel alcanzado. |

### Resolución al instante de la autoevaluación

Cada autoevaluación se resuelve en el momento, no al generar la siguiente sesión (ya no hace falta esperar:
con "tanda completa" definida como una sola vuelta, una frase recibe como mucho una autoevaluación por sesión
generada, así que resolver al instante es equivalente):

1. **Acierto que certifica** (racha llega a 3): nivel sube a `min(nivel + 1, 5)`, racha vuelve a 0,
   `dueAtSession` = sesión actual + enfriamiento del nuevo nivel.
2. **Acierto que no certifica**: nivel sin cambios, `dueAtSession` = sesión actual + 1.
3. **Fallo**: nivel baja en 1 si era ≥ 1 (sin cambios de ADR-0015), `dueAtSession` = sesión actual + 1, en
   cualquier nivel — una frase recién fallada vuelve a estar disponible en la sesión inmediatamente siguiente.

La racha **ya no se reinicia** por el solo hecho de entrar en una tanda recién compuesta: persiste de una
sesión a la siguiente hasta un fallo real.

### Enfriamiento en sesiones por nivel

| Nivel | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|
| Sesiones hasta la próxima vez | 1 | 2 | 4 | 8 | 16 |

### Composición de la tanda nueva

Se retira la cola de arrastradas: el `dueAtSession` de cada frase basta para decidir si debe reaparecer, sin
una estructura aparte que mover al generar sesión. La tanda se llena hasta N frases en tres bloques con
cuota propia sobre N: **nuevas (20 %)**, **a reforzar (55 %)** y **dominadas (el resto)**. Si un bloque no
alcanza su cuota, el hueco se reparte en el orden a reforzar → nuevas → dominadas.

- **A reforzar**: frases con `dueAtSession` igual o anterior a la sesión actual, **y** de nivel CEFR igual o
  superior al elegido.
- **Dominadas**: frases de nivel de consolidación 4 o 5 cuyo `dueAtSession` todavía no venció (repaso
  ligero), **y** de nivel CEFR igual o superior al elegido.
- **Nuevas**: frases nunca vistas del nivel CEFR exacto elegido (sin cambios).

El filtro CEFR, antes limitado a las nuevas, se extiende a reforzar y dominadas: ninguna frase por debajo del
nivel elegido vuelve a ofrecerse. Si falta material del nivel elegido o superior, la tanda sale más pequeña.

### Ajustes: CEFR y N se comportan igual

Al no haber ya una resolución de cierre de la que cambiar N formara parte y cambiar CEFR no, ambos pasan a
comportarse igual: cualquier cambio de CEFR y/o N en Ajustes recompone la tanda activa de inmediato, sin
aviso, sin incrementar el contador de sesiones generadas y sin tocar racha, nivel ni `dueAtSession` de
ninguna frase.

### Migración de datos

El proyecto está en desarrollo, versión 1.0.0 todavía no publicada, sin usuarios en producción. La migración
de Room es destructiva para las tablas afectadas (cola de arrastradas, columna de próximo repaso): el
progreso existente se pierde al actualizar, igual que una instalación nueva.

## Consecuencias

- **Positivas:**
  - Una frase puede certificar y subir de nivel a lo largo de varias sesiones generadas, no solo (de forma
    imposible) dentro de una.
  - El repaso vuelve a tener sentido con varias sesiones por día: ya no depende del calendario.
  - Cambiar el nivel CEFR nunca mezcla frases de niveles muy distintos.
  - El modelo se simplifica: sin cola de arrastradas ni resolución de cierre, `ForceCloseActiveBatch` se
    retira y `GenerateSession`/`RegenerateActiveBatchForLevel` quedan reducidos a "vaciar y recomponer, con o
    sin avanzar el contador".
- **Negativas:**
  - Los números de enfriamiento (1, 2, 4, 8, 16 sesiones) son una propuesta de partida sin validar con datos
    reales de uso; puede hacer falta ajustarlos en un cambio posterior.
  - Cambiar N en Ajustes deja de certificar ni resetear racha, un cambio de comportamiento respecto a lo que
    ADR-0016 acababa de fijar.
  - La migración destructiva pierde el progreso de cualquier instalación existente (aceptable sin usuarios en
    producción, pero a tener en cuenta si cambia esa condición antes de implementar).
- **Descartado:**
  - Mantener la resolución por lote al generar sesión (como con `ResolveBatchClosure`): ya no aporta nada,
    porque cada frase solo recibe una autoevaluación por sesión generada.
  - Mantener `nextReview` nulo para nivel 0: impide marcar "disponible pronto" a una frase de nivel 0 recién
    fallada, que es justo uno de los dos problemas que motiva este ADR.
  - Reutilizar los mismos números de días que ADR-0015 pero en sesiones (1, 3, 7, 14, 30): para un uso de
    varias sesiones diarias, haría que una frase de nivel 5 tardase semanas en volver a repasarse.

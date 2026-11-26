# ADR-0027: Rotación anti-bloqueo de la tanda

- **Estado:** Propuesto
- **Fecha:** 2026-11-26
- **Sustituye a:** ADR-0026 (parcialmente, en el orden de relleno de la tanda nueva) y ADR-0025 (parcialmente, en
  el cálculo de nuevas: techo, hueco y `N − pendientes`). La prioridad `nivel + (dueAtSession − sesión actual)` de
  ADR-0025 y ADR-0026 se aplica ahora dentro de cada tramo y ya no decide sola qué frases entran. Siguen
  vigentes la espera intra-nivel de ADR-0025, el reparto de las nuevas de ADR-0025, la tanda llena hasta
  N y el acierto sobre una frase aún no pendiente de ADR-0026, el filtro CEFR exacto de ADR-0021 y la regla de
  fallo de ADR-0024.

## Contexto

Con las reglas de ADR-0025 y ADR-0026, una simulación con N = 10 y un 70 % de aciertos durante 60 sesiones
muestra que el usuario se atasca:

- Las sesiones 2 a 6 no tienen ninguna frase nueva y repiten el 100 % de la sesión anterior.
- Una misma frase aparece hasta 15-19 sesiones seguidas: en los niveles 0 a 2 la espera intra-nivel es 1, así que
  la frase está pendiente en cada sesión hasta que certifica, y el relleno hasta N vuelve a meter las mismas.
- Cerca del 30 % de las frases falladas tardan 3-5 sesiones en volver, porque compiten por prioridad con el resto
  de pendientes.

El usuario fija estos objetivos, medidos desde la sesión 4 (en las sesiones 2 y 3 no son alcanzables por
aritmética: tras 10 nuevas en la primera tanda y como mucho 2 nuevas después, la sesión 2 repite al menos el 80 %):

- Como mucho `floor(N / 5)` nuevas por tanda tras la primera (2 con N = 10).
- Entre un 25 % y un 40 % de la tanda repetida de la sesión anterior.
- Ninguna frase más de 6 sesiones seguidas.
- Una frase fallada vuelve en 1 o 2 sesiones.

Ninguno de los cuatro se puede garantizar con lo que hoy se guarda: no se sabe en qué sesiones se respondió cada
frase ni cuándo se falló por última vez.

## Decisión

### Registro de respuestas por frase

Cada frase con progreso guarda, además de nivel, racha y `dueAtSession`:

- **Última sesión respondida:** la sesión generada en la que se autoevaluó por última vez.
- **Sesiones seguidas:** cuántas sesiones generadas consecutivas, terminando en la última sesión respondida, se
  ha autoevaluado.
- **Última sesión fallada:** la sesión generada de su último fallo, si lo hay.

Se actualizan al autoevaluar, no al componer: recomponer la tanda por un cambio de ajustes no los toca, y una
frase que estuvo en una tanda pero no se respondió no cuenta como vista. Al autoevaluar en la sesión `s`, las
sesiones seguidas suben en 1 si la última sesión respondida era `s − 1`, no cambian si era `s` y vuelven a 1 en
otro caso. Revertir la última autoevaluación restaura también estos tres valores.

### Composición de la tanda nueva

Con N el tamaño de la tanda, `s` la sesión actual y solo frases del nivel CEFR elegido:

- **Repetida:** frase cuya última sesión respondida es `s − 1`.
- **En descanso:** repetida con 6 o más sesiones seguidas.
- **Fallada por volver:** frase cuya última sesión fallada es su última sesión respondida, y esta es `s − 2`,
  `s − 1` o `s` (el caso `s` se da al recomponer la tanda en la misma sesión por un cambio de ajustes).
- **Nuevas:** en la primera tanda de un nivel CEFR (ninguna frase del nivel con progreso), `min(N, disponibles)`.
  En las demás, `min(disponibles, floor(N / 5))` mientras las frases de nivel 0 con progreso del nivel CEFR
  elegido sean menos de `N + floor(N / 5)`; si no, 0.
- **Límites de repetidas:** mínimo `ceil(N / 4)` y máximo `floor(2N / 5)`.

La tanda se rellena por tramos. Los tramos 1 a 4 se detienen cuando las no nuevas llegan a `N − nuevas`; los 5 a
7, cuando las no nuevas más las nuevas llegan a N.
Dentro de cada tramo se sigue la prioridad de ADR-0025 y ADR-0026 (`nivel + (dueAtSession − s)`, desempate por
catálogo) y ninguna frase en descanso entra antes del tramo 7.

1. **Falladas por volver.** Una repetida no entra si ya hay el máximo de repetidas.
2. **Repetidas** hasta el mínimo.
3. **Pendientes**, saltando las repetidas una vez alcanzado el máximo.
4. **Repaso adelantado**, con la misma regla de máximo.
5. **Relleno:** el resto de frases con progreso que no están en descanso, aunque superen el máximo de repetidas.
6. **Más nuevas** en orden de catálogo.
7. **Frases en descanso.**

Las no nuevas ocupan la tanda en el orden de sus tramos y las nuevas se reparten entre ellas con la fórmula de
posición de ADR-0025.

## Consecuencias

- **Positivas:**
  - En la simulación (20 semillas, N = 10/20/30, 50/70/90 % de aciertos, 60 sesiones), desde la sesión 4 el
    95-98 % de las sesiones tienen entre un 25 % y un 40 % de repetidas; ninguna frase pasa de 6 sesiones
    seguidas y el 100 % de las falladas vuelven en 1 o 2 sesiones.
  - La sesión 2 ya tiene `floor(N / 5)` nuevas.
  - Las reglas de la espera intra-nivel, la certificación, el fallo y el repaso adelantado no cambian.
- **Negativas:**
  - Cambio de esquema: `PracticeDatabase` sube a la versión 5 con migración destructiva (`fallbackToDestructiveMigration`
    ya configurado). Se pierde el progreso guardado; la app no tiene usuarios reales.
  - Con el límite de nuevas, cuando hay pocas pendientes entra repaso adelantado, también de niveles altos, antes
    que más nuevas.
  - Si hay muy pocas frases con progreso y muchas en descanso, el tramo 6 puede meter más de `floor(N / 5)` nuevas
    (2 de 180 simulaciones, ambas con un 50 % de aciertos y N = 10).
  - Las sesiones 2 y 3 siguen repitiendo un 80-100 %.
  - La composición tiene más reglas que la de ADR-0026.
- **Descartado:**
  - Subir la espera intra-nivel de los niveles 0-2 a 2 sesiones: reduce las sesiones seguidas (6-9), pero no
    garantiza el tope ni la vuelta de las falladas.
  - Derivar las repetidas de la tanda activa al generar sesión: no sirve para las sesiones seguidas ni para la
    recomposición por ajustes, que vacía la tanda en la misma sesión.
  - Guardar el registro al componer: una recomposición o una frase no respondida contarían como vistas.
  - Una migración que conserve el progreso: no hay usuarios reales y el precedente (versiones 2 a 4) es la
    migración destructiva.

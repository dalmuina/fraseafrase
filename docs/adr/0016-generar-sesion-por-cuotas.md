# ADR-0016: Generar sesión por cuotas en vez de cerrar tanda con aviso

- **Estado:** Sustituido por ADR-0017
- **Fecha:** 2026-11-06
- **Sustituye a:** ADR-0015 (parcialmente)

## Contexto

ADR-0015 mantiene de ADR-0012 dos reglas que, en uso real, no aportan valor al usuario:

1. **Cierre manual con aviso.** El usuario debe pedir explícitamente "cerrar tanda" y confirmar un aviso con el
   número de frases no certificadas que pasarían a la siguiente. Una tanda se practica y, en cuanto se completa
   una vuelta, lo natural es que la app ofrezca generar la siguiente, sin que el usuario tenga que decidir un
   número que no cambia su decisión.
2. **Composición solo por prioridad estricta.** La tanda siguiente se llena primero con arrastradas, después
   con pendientes y solo con el hueco restante con frases nuevas. Esto puede dejar fuera, indefinidamente,
   tanto las frases nuevas (si siempre hay arrastradas o pendientes de sobra) como el repaso de frases ya muy
   dominadas, que nunca vuelven a aparecer hasta que su repetición espaciada las hace pendientes, por lejana que
   sea esa fecha.

Además, hoy cambiar el nivel CEFR en Ajustes fuerza el mismo cierre que un cierre manual: certifica y resetea la
racha de toda la tanda activa, cuando cambiar de nivel solo debería cambiar qué frases se ofrecen, sin alterar
el progreso de ninguna.

Este ADR es la base del *change* de OpenSpec `replace-batch-closure-with-session-generation`.

## Decisión

Se mantienen sin cambios, de ADR-0012/ADR-0015: los conceptos de tanda, nivel y pendiente; la lista circular; el
incremento/reinicio de racha por acierto/fallo; la certificación por racha ≥ 3; la bajada instantánea de nivel
ante un fallo (regla 6 de ADR-0015); los intervalos de repaso; y el alcance del MVP, salvo lo que se indica
abajo.

Cambian el cierre de tanda, la composición de la tanda nueva, y se añade el concepto de **tanda completa** y la
regeneración por cambio de nivel CEFR.

### Conceptos nuevos o modificados

| Término | Significado |
|---|---|
| **Tanda completa** | Toda frase de la tanda activa tiene al menos una autoevaluación en esa tanda. |
| **Generar sesión** | Acción del usuario que resuelve la tanda activa y compone la siguiente en un solo paso. |

### Generación de sesión en vez de cierre con aviso

1. Ya no hay un aviso previo con el número de no certificadas. En su lugar, cuando la tanda activa está
   **completa**, la app muestra un mensaje positivo con la opción de generar la siguiente sesión.
2. Al generar sesión, cada frase de la tanda se resuelve exactamente con la misma tabla de ADR-0015 (certificada
   sube de nivel y sale de la tanda; respondida no certificada se arrastra sin cambios de nivel; nunca
   respondida se arrastra sin cambios), y la tanda siguiente se compone de inmediato, por cuotas (ver abajo),
   sin esperar a una lectura posterior.
3. No existe ya un número de "no certificadas esperado" que pueda quedar desfasado: generar sesión actúa
   siempre sobre la tanda activa guardada en ese momento.

### Composición de la tanda nueva por cuotas

La tanda se llena hasta N frases en tres bloques, cada uno con una cuota calculada sobre N:

| Bloque | Cuota sobre N | Candidatos |
|---|---|---|
| Nuevas | `floor(N * 0.4)` | Frases nunca vistas, del nivel CEFR elegido, en orden de catálogo |
| A reforzar | `floor(N * 0.4)` | Arrastradas (primero, en orden de cola) y pendientes (por nivel ascendente, después por repaso más antiguo, después por catálogo) |
| Dominadas | el resto de N | Frases de nivel de consolidación 4 o 5 que no sean ya arrastradas ni pendientes hoy, en orden de catálogo |

Si un bloque no tiene suficientes candidatos para llenar su cuota, el hueco pasa a los demás bloques con
candidatos disponibles, en el orden **a reforzar → nuevas → dominadas**. Una frase que no entra en ningún
bloque por falta de hueco conserva su posición y su progreso para una composición posterior, igual que hoy.

Con N = 20 (el valor por defecto), las cuotas son 8 nuevas, 8 a reforzar y 4 dominadas.

### Cambio de nivel CEFR en Ajustes

Cambiar el nivel CEFR elegido, sin cambiar N, recompone la tanda activa de inmediato con el nuevo nivel, **sin**
pasar por la resolución de cierre: ninguna frase certifica, sube o baja de nivel de consolidación, ni cambia su
próximo repaso por este motivo. Las frases de la tanda activa en ese momento pasan a la cola de arrastradas, en
su orden de tanda, delante de la cola sobrante existente, y la tanda siguiente se compone por cuotas con el
nuevo nivel CEFR.

Si en el mismo guardado de Ajustes cambia también N (con o sin cambio de CEFR), gana el comportamiento de
**cambiar N**: se cierra la tanda activa con la resolución normal (certifica, resetea racha), igual que hoy, y
la siguiente se compone por cuotas con el nuevo nivel CEFR y la nueva N. Es decir: solo el cambio de CEFR *en
solitario* evita la resolución de cierre; cualquier cambio de N, solo o junto con el de CEFR, sigue cerrando la
tanda activa como un cierre deliberado desde ajustes, sin aviso previo.

### Alcance del MVP

Sin cambios respecto a ADR-0015, salvo que el punto 2 ("cierre con aviso y composición de la tanda nueva") pasa
a leerse como "generación de sesión y composición de la tanda nueva por cuotas", según este ADR.

## Consecuencias

- **Positivas:**
  - El usuario deja de gestionar dos parámetros por frase (racha y nivel) y una decisión de cierre sin
    consecuencia real; solo ve cuántos aciertos le faltan para la siguiente insignia y genera sesión cuando
    termina una vuelta.
  - Las frases nuevas y las ya dominadas tienen hueco garantizado en cada tanda, en vez de depender de que no
    haya suficientes arrastradas o pendientes.
  - Cambiar el nivel CEFR deja de tener el efecto colateral de certificar y resetear racha de toda la tanda
    activa.
- **Negativas:**
  - Con cuotas, una tanda puede dejar fuera arrastradas o pendientes que antes habrían entrado por tener hueco
    disponible en N; esas frases esperan a una composición posterior, igual que ya ocurre hoy cuando no caben
    en N.
  - Cambiar CEFR y N a la vez certifica y resetea racha (como un cierre normal), mientras que cambiar solo CEFR
    no lo hace: son dos comportamientos distintos para el mismo formulario de Ajustes, que hay que documentar
    bien en la pantalla para no confundir al usuario.
- **Descartado:**
  - Que cambiar CEFR "gane" siempre sobre un cambio de N simultáneo (sin cerrar ni certificar en ningún caso):
    se descarta porque mezclaría, en una sola acción de guardado, dos tandas con historiales de resolución
    distintos (una parte de las frases certificadas y otra no), más difícil de razonar para el usuario que la
    regla simple "cambiar N siempre cierra".
  - Topes duros por bloque de cuota (nunca superar la cuota aunque haya hueco): se descarta porque dejaría
    tandas más pequeñas que N incluso habiendo material disponible, contradiciendo la regla ya vigente de que
    la tanda se compone con hasta N frases cuando hay al menos una disponible.

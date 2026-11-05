# ADR-0015: Bajada instantánea de nivel de consolidación ante un fallo

- **Estado:** Aceptado
- **Fecha:** 2026-11-05
- **Sustituye a:** ADR-0012

## Contexto
ADR-0012 solo resetea la **racha** de una frase a 0 cuando falla dentro de la tanda activa; el **nivel**
de consolidación no cambia hasta que la tanda se cierra, y solo si la frase no llega a certificarse
(racha ≥ 3) antes de ese cierre. Esto permite que una frase de nivel alto (por ejemplo, nivel 3) falle
una vez al presentarse como pendiente y, si el usuario la acierta 3 veces seguidas antes de cerrar la
tanda, termine subiendo de nivel (`min(nivel + 1, 5)`) sin que el fallo haya dejado ningún rastro. El
nivel de consolidación debe reflejar que la frase ya no se domina con la fiabilidad que indicaba, desde
el momento del fallo, no solo si el resto de la tanda no basta para recuperarla.

Este ADR es bloqueante para el *change* de OpenSpec que añadirá insignias de progreso (P4): esas
insignias se derivan del nivel de consolidación, así que la regla de cuándo y cuánto baja ese nivel debe
quedar fijada antes.

## Decisión

Se mantienen sin cambios los conceptos, los intervalos de repaso, la composición de la tanda nueva y el
alcance del MVP de ADR-0012. Cambian la regla de fallo dentro de una tanda y, como consecuencia, la tabla
de cierre de tanda.

### Conceptos

| Término | Significado |
|---|---|
| **Tanda** | Lista de hasta N frases que el usuario practica. Solo hay una tanda activa. |
| **Nivel** | Grado de consolidación de una frase: 0 (nueva o en aprendizaje) a 5. |
| **Racha** | Aciertos consecutivos de una frase dentro de la tanda activa. |
| **Certificada** | Frase con racha ≥ 3 en el momento de cerrar la tanda. |
| **Pendiente** | Frase con nivel ≥ 1 cuya fecha de próximo repaso es hoy o anterior. |

### Práctica dentro de una tanda

1. La tanda es una **lista circular**: cada frase respondida, acierte o falle, pasa al final de la lista. Ninguna frase desaparece; se puede practicar la tanda tantas veces como se quiera.
2. El usuario ve la frase en español, la dice en portugués, revela la respuesta y se autoevalúa: **acierto** o **fallo**.
3. Un acierto incrementa la racha en 1; un fallo la pone a 0.
4. Una frase con racha ≥ 3 puede mostrarse como "sabida" en la interfaz, pero sigue en la lista.
5. La racha de todas las frases empieza en 0 en cada tanda nueva.
6. **(Nuevo)** Un fallo en una frase con nivel ≥ 1 baja su nivel en 1 (`max(nivel - 1, 0)`) **al instante**,
   no solo al cerrar la tanda, y recalcula su fecha de próximo repaso según el intervalo del nivel
   resultante (fecha de la respuesta + intervalo de ese nivel), igual que si la tanda cerrara en ese
   momento para esa frase. Una frase en nivel 0 que falla no tiene nivel que bajar: solo se resetea su
   racha (regla 3, sin cambios). Si la misma frase vuelve a fallar más adelante en la misma tanda, la
   regla se aplica de nuevo sobre su nivel ya reducido.

### Cierre de una tanda

El usuario decide cuándo empezar una tanda nueva. La tanda activa se cierra en ese momento:

1. Si hay frases **no certificadas**, la app avisa antes de cerrar con el número de frases que pasarán a la siguiente tanda. El usuario puede confirmar o seguir practicando.
2. Al confirmar, cada frase de la tanda se resuelve así:

| Situación al cerrar | Nivel resultante | Próximo repaso | Destino |
|---|---|---|---|
| Certificada (racha ≥ 3) | `min(nivel + 1, 5)` | fecha de cierre + intervalo del nuevo nivel | Sale de la tanda |
| Respondida y no certificada | sin cambios (ya aplicado por la regla 6 ante cada fallo) | sin cambios (ya aplicado por la regla 6 ante cada fallo) | Se arrastra a la siguiente tanda |
| Nunca respondida en esta tanda | sin cambios | sin cambios | Se arrastra a la siguiente tanda |

A diferencia de ADR-0012, el cierre ya **no** fuerza a nivel 0 a las frases respondidas y no certificadas:
su nivel y su próximo repaso ya quedaron fijados, fallo a fallo, por la regla 6 durante la práctica.

### Intervalos de repaso (en días naturales)

| Nivel | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|
| Días hasta el próximo repaso | 1 | 3 | 7 | 14 | 30 |

- Los intervalos se miden en **días naturales** según la fecha local del dispositivo (puerto `Clock`), no en número de tandas.
- Una frase de nivel 5 que se vuelve a certificar se mantiene en nivel 5 y vuelve a los 30 días.
- Una frase de nivel 0 no tiene próximo repaso (no es pendiente; solo entra en una tanda nueva como frase nueva o arrastrada).

### Composición de una tanda nueva

La tanda se llena hasta **N frases**, en este orden de prioridad:

1. Las frases **arrastradas** de la tanda anterior, en el orden que tenían.
2. Las frases **pendientes**, ordenadas por nivel ascendente y, a igual nivel, por fecha de repaso más antigua.
3. **Frases nuevas** (nunca vistas) del nivel CEFR elegido por el usuario, en el orden del catálogo, solo si queda hueco.

Lo que no cabe espera a tandas posteriores sin perder su nivel. Mientras haya arrastradas o pendientes que no caben, no entran frases nuevas.

Otras reglas:

- **N** se configura en ajustes: por defecto 20, entre 10 y 30. Un cambio de N se aplica a la siguiente tanda.
- La **primera tanda** se crea automáticamente la primera vez que se usa la app.
- Si no hay suficientes frases disponibles, la tanda puede tener menos de N. Si no hay ninguna (nada pendiente ni nuevo), no se crea tanda y la app lo indica.

### Alcance del MVP

**Dentro:**

1. Catálogo de frases precargado en `pt-PT` con traducción al español, clasificado por nivel (A1–C1) y tema.
2. Práctica por tandas según este ADR: lista circular, autoevaluación, rachas, certificación, bajada
   instantánea de nivel ante un fallo, cierre con aviso y composición de la tanda nueva.
3. Repetición espaciada con los niveles e intervalos de este ADR.
4. Audio (TTS): escuchar la frase en portugués.
5. Ajustes: nivel CEFR y tamaño de tanda N.
6. Progreso básico: frases certificadas en la tanda activa y total de frases por nivel.

**Fuera** (cada uno será un *change* de OpenSpec posterior):

- Corrección automática por voz (reconocimiento de voz y comparación).
- Dirección inversa (portugués → español) y otros modos de práctica.
- Variante `pt-BR` u otros idiomas.
- Cuentas, sincronización en la nube, IA generativa, notificaciones, estadísticas avanzadas, monetización.

## Consecuencias
- **Positivas:**
  - El nivel de consolidación refleja la fiabilidad real de la frase en todo momento, no solo al cerrar
    la tanda: un fallo aislado en una frase de nivel alto deja rastro aunque el usuario la acierte después.
  - Las insignias de progreso (P4), derivadas del nivel, serán coherentes con lo que el usuario acaba de
    experimentar en la tanda, en vez de solo con el resultado final al cerrarla.
  - La bajada es gradual (`nivel - 1`), no un reinicio a 0: un fallo puntual en una frase muy consolidada
    (nivel 4 o 5) no le hace perder todo el progreso, solo lo penaliza.
- **Negativas:**
  - Una frase puede subir y bajar de nivel varias veces dentro de la misma tanda (fallo → certificación
    posterior → fallo de nuevo), lo que complica algo más el seguimiento manual del progreso por parte
    del usuario mientras practica.
  - Recalcular la fecha de próximo repaso en cada fallo, y no solo al cerrar la tanda, añade una
    escritura de persistencia por fallo en vez de una sola al cierre (impacto de rendimiento esperado
    como despreciable: frecuencia de interacción humana, sin lotes grandes).
- **Descartado:**
  - Reiniciar a nivel 0 ante cualquier fallo (opción más simple, pero penaliza igual un fallo aislado en
    nivel 1 que en nivel 5, perdiendo toda la repetición espaciada acumulada).
  - Mantener la bajada solo al cierre de tanda (regla actual de ADR-0012): permite que una racha de
    aciertos posterior "tape" el fallo y no refleja el nivel real mientras se practica.

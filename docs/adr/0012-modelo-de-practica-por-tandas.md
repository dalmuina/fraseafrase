# ADR-0012: Modelo de práctica por tandas y alcance del MVP

- **Estado:** Aceptado
- **Fecha:** 2026-10-30
- **Sustituye a:** ADR-0009

## Contexto
El ADR-0009 planteaba una "sesión diaria" ligada al calendario. El método de estudio real es otro:
se practican las mismas frases repetidas veces, en distintos momentos, sin introducir frases nuevas
hasta haber consolidado las actuales. El ritmo lo decide el usuario: hay días en que se puede trabajar
más y días en que no. Este ADR fija las reglas del método, que son la base de las especificaciones
`practice-batch` y `spaced-repetition`, y actualiza el alcance del MVP.

## Decisión

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

### Cierre de una tanda

El usuario decide cuándo empezar una tanda nueva. La tanda activa se cierra en ese momento:

1. Si hay frases **no certificadas**, la app avisa antes de cerrar con el número de frases que pasarán a la siguiente tanda. El usuario puede confirmar o seguir practicando.
2. Al confirmar, cada frase de la tanda se resuelve así:

| Situación al cerrar | Nivel resultante | Próximo repaso | Destino |
|---|---|---|---|
| Certificada (racha ≥ 3) | `min(nivel + 1, 5)` | fecha de cierre + intervalo del nuevo nivel | Sale de la tanda |
| Respondida y no certificada | 0 | — | Se arrastra a la siguiente tanda |
| Nunca respondida en esta tanda | sin cambios | sin cambios | Se arrastra a la siguiente tanda |

### Intervalos de repaso (en días naturales)

| Nivel | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|
| Días hasta el próximo repaso | 1 | 3 | 7 | 14 | 30 |

- Los intervalos se miden en **días naturales** según la fecha local del dispositivo (puerto `Clock`), no en número de tandas.
- Una frase de nivel 5 que se vuelve a certificar se mantiene en nivel 5 y vuelve a los 30 días.

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
2. Práctica por tandas según este ADR: lista circular, autoevaluación, rachas, certificación, cierre con aviso y composición de la tanda nueva.
3. Repetición espaciada con los niveles e intervalos de este ADR.
4. Audio (TTS): escuchar la frase en portugués.
5. Ajustes: nivel CEFR y tamaño de tanda N.
6. Progreso básico: frases certificadas en la tanda activa y total de frases por nivel.

**Fuera** (cada uno será un *change* de OpenSpec posterior):

- Corrección automática por voz (reconocimiento de voz y comparación). Es el **primer cambio tras el MVP**.
- Dirección inversa (portugués → español) y otros modos de práctica.
- Variante `pt-BR` u otros idiomas.
- Cuentas, sincronización en la nube, IA generativa, notificaciones, estadísticas avanzadas, monetización.

## Consecuencias
- **Positivas:**
  - El ritmo lo decide el usuario: en los días con más tiempo, varias tandas seguidas incorporan más frases nuevas, porque los repasos aún no están pendientes.
  - Nunca se acumula más trabajo del que cabe en una tanda, y lo más frágil se practica primero.
  - Las reglas son deterministas, con números explícitos, y se traducen directamente en escenarios testeables.
- **Negativas:**
  - Exigir 3 aciertos seguidos implica al menos 3 pasadas por cada frase para avanzar.
  - El progreso depende de la autoevaluación honesta del usuario hasta que llegue la corrección por voz.
- **Descartado:**
  - Sesión diaria ligada al calendario (ADR-0009).
  - Intervalos medidos en tandas: varias tandas seguidas acortarían artificialmente el espaciado.
  - Algoritmos SM-2 completo y FSRS: requieren cuatro grados de respuesta. El algoritmo queda detrás de un puerto del dominio, así que puede sustituirse con un ADR posterior.

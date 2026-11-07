# ADR-0018: Progreso básico sin certificadas de la tanda activa

- **Estado:** Aceptado
- **Fecha:** 2026-11-07
- **Sustituye a:** ADR-0017 (parcialmente, solo el punto 6 de "Alcance del MVP")

## Contexto

El punto 6 de "Alcance del MVP" (fijado en ADR-0012 y heredado sin cambios por ADR-0015/ADR-0016/
ADR-0017) dice: "Progreso básico: frases certificadas en la tanda activa y total de frases por
nivel". Con el modelo de ADR-0017, cada autoevaluación se resuelve **al instante**: una frase que
certifica (racha llega a 3) sube de nivel y su racha vuelve a 0 en la misma autoevaluación que la
certificó, en vez de quedar marcada como "certificada" a la espera de un cierre de tanda posterior
(como sí ocurría con el modelo de cierre de ADR-0012/ADR-0015). El dato "certificadas en la tanda
activa" era útil cuando certificar y cerrar tanda eran dos momentos distintos; con resolución al
instante, ese contador vale casi siempre 0 - 1 (según si la frase mostrada en ese preciso instante
acaba de certificar o no) y deja de informar nada útil sobre el progreso real del usuario.

En paralelo, el cambio de OpenSpec `new-style` rediseña la pantalla de progreso a partir de mocks
que sustituyen ese dato por el total de frases disponibles del catálogo (la misma suma por nivel que
ya exige el punto 6) y el nivel CEFR vigente, ambos ya visibles o derivables de datos existentes.

## Decisión

El punto 6 de "Alcance del MVP" pasa a decir: "Progreso básico: total de frases disponibles del
catálogo, nivel CEFR vigente y total de frases por nivel". El resto del alcance del MVP fijado en
ADR-0012 y heredado por ADR-0015/ADR-0016/ADR-0017 no cambia.

La pantalla de progreso deja de mostrar cuántas frases de la tanda activa están certificadas. Sigue
mostrando el desglose de frases del catálogo por nivel de consolidación (0 a 5), y pasa a mostrar
además el total de frases con progreso registrado (la suma de ese desglose) y el nivel CEFR
guardado en los ajustes vigentes.

## Consecuencias

- **Positivas:**
  - El progreso básico vuelve a mostrar datos que informan algo estable sobre el avance del usuario,
    en vez de un contador que oscila entre 0 y 1 frase por el modelo de resolución al instante.
  - No hace falta ningún caso de uso ni puerto nuevo: el total disponible es la misma suma por nivel
    que ya exige el punto 6, y el nivel CEFR ya se puede leer con `ObtainUserSettings`.
- **Negativas:**
  - Se pierde la única señal visible de "cuánto llevo certificado en esta pasada", aunque con
    resolución al instante esa señal ya no era fiable.
- **Descartado:**
  - Mantener "certificadas en la tanda activa" junto al dato nuevo: habría dos números en la misma
    tarjeta, uno de ellos (certificadas) casi siempre 0, sin aportar información adicional sobre el
    progreso.

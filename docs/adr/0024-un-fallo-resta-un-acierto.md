# ADR-0024: Un fallo resta un acierto, no un nivel

- **Estado:** Propuesto
- **Fecha:** 2026-11-22
- **Sustituye a:** ADR-0017 (parcialmente, solo la regla de fallo: punto 3 de "Resolución al instante de la
  autoevaluación" y la regla "un fallo pone la racha a 0" heredada de ADR-0012/ADR-0015)

## Contexto

Con ADR-0017, un fallo pone la racha a 0 y, si el nivel de consolidación es ≥ 1, lo baja en 1. Al probar la
app, esta penalización resulta demasiado severa: una frase en Plata con racha 2 (a un acierto de Oro) que se
falla una vez acaba en Bronce con racha 0, a tres aciertos de recuperar Plata. Un solo fallo borra cinco
aciertos de trabajo y desanima a seguir practicando las frases ya consolidadas.

## Decisión

El progreso de una frase se interpreta como una escala continua de tres aciertos por nivel
(`nivel × 3 + racha`). Un fallo resta **un solo acierto** en esa escala:

1. **Racha mayor que 0:** la racha baja en 1 y el nivel no cambia.
2. **Racha 0 y nivel ≥ 1:** el nivel baja en 1 y la racha queda en 2, es decir, a un acierto de recuperar
   el nivel que tenía.
3. **Racha 0 y nivel 0:** nivel y racha no cambian.

Ejemplo: Plata con racha 0 ("3 para Oro") → fallo → Bronce con racha 2 ("1 para Plata").

En todos los casos, `dueAtSession` = sesión actual + 1, como en ADR-0017.

Se mantienen sin cambios el resto de ADR-0017 y ADR-0018: certificación por racha 3 (sube de nivel y la
racha vuelve a 0), enfriamiento por nivel, composición por cuotas, reversión de la última autoevaluación y
alcance del MVP.

## Consecuencias

- Positivas:
  - Un fallo aislado en una frase consolidada cuesta un acierto, no un nivel entero más la racha.
  - La penalización es predecible y simétrica con el acierto (+1 / −1 en la misma escala).
  - No cambia el modelo persistido: nivel y racha siguen guardándose igual y no hace falta migración.
- Negativas:
  - Una frase puede bajar y subir de nivel con fallos y aciertos alternos cerca del límite de un nivel.
  - Hacen falta más fallos para que una frase mal aprendida vuelva a nivel 0 (tres fallos por nivel).
- Alternativas consideradas:
  - Mantener ADR-0017 (racha a 0 y bajada de nivel): demasiado severo según la prueba manual.
  - Racha a 0 sin bajar de nivel: sigue borrando hasta dos aciertos y nunca refleja un retroceso de nivel.
  - Bajar de nivel sin tocar la racha: penaliza igual un fallo con racha 2 que con racha 0.

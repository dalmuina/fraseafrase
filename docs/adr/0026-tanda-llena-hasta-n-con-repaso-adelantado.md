# ADR-0026: Tanda llena hasta N con repaso adelantado

- **Estado:** Propuesto
- **Fecha:** 2026-11-25
- **Sustituye a:** ADR-0025 (parcialmente, en la composición de la tanda nueva: el techo de la primera tanda y la
  regla de que una frase con progreso que aún no está pendiente no entra en la tanda) y en la resolución de un
  acierto sobre una frase que aún no está pendiente. La espera intra-nivel, la
  prioridad de las pendientes, el cálculo de nuevas dentro del límite y su reparto de ADR-0025 siguen vigentes,
  igual que el filtro CEFR exacto de ADR-0021 y la regla de fallo de ADR-0024.

## Contexto

Con las reglas de ADR-0025, la tanda sale más pequeña que N siempre que haya pocas pendientes. En una prueba en
emulador con N = 10 (it-IT, A1) las sesiones tuvieron 5, 9 y luego 10 frases: la primera tanda se limita a la
mitad de N y, en las siguientes, una frase con progreso que aún no está pendiente nunca entra, aunque sobre
sitio. El usuario practica sesiones demasiado cortas justo al empezar, cuando más material nuevo hay.

## Decisión

La tanda tiene siempre N frases mientras el nivel CEFR elegido tenga frases suficientes en el catálogo. Con N el
tamaño de la tanda y solo frases del nivel CEFR elegido, se rellena en este orden:

1. **Pendientes** por prioridad, como en ADR-0025.
2. **Nuevas dentro del límite:** el número que da la fórmula de ADR-0025 (techo, hueco y `N − pendientes`).
3. **Repaso adelantado:** frases con progreso que aún no están pendientes (`dueAtSession` posterior a la sesión
   actual), ordenadas por `nivel + (dueAtSession − sesión actual)`, de menor a mayor, y desempatadas por orden de
   catálogo. Es la misma prioridad de ADR-0025 aplicada a un retraso negativo.
4. **Más nuevas** en orden de catálogo, si aún falta para N.

La primera tanda de un nivel CEFR tiene, por tanto, `min(N, frases del nivel)` nuevas. La tanda solo falta
cuando el nivel CEFR elegido no tiene ninguna frase en el catálogo.

En la tanda, las frases de repaso adelantado van tras las pendientes y las nuevas se reparten entre ambas con la
fórmula de posición de ADR-0025.

### Autoevaluación de una frase que aún no está pendiente

Un **acierto** sobre una frase cuyo `dueAtSession` es posterior a la sesión actual no cambia su nivel, su racha
ni su `dueAtSession`: solo la marca como respondida en la tanda. Un **fallo** se resuelve como cualquier otro
(ADR-0024). La regla se evalúa al responder, con el progreso de ese momento, así que también se aplica a una
segunda autoevaluación de la misma frase en la misma sesión.

No hay cambio de esquema: todo se calcula con el progreso que ya se guarda.

## Consecuencias

- **Positivas:**
  - Las sesiones tienen la duración que el usuario eligió, también la primera.
  - El repaso adelantado no adelanta la certificación: un acierto antes de tiempo no acorta los intervalos de
    repetición espaciada, y un fallo sí devuelve la frase pronto.
  - El estado "no hay frases para practicar" queda reservado a un nivel CEFR sin frases en el catálogo.
- **Negativas:**
  - Con pocas pendientes vuelven a verse frases que aún no tocaban, también de niveles altos si no hay otras.
  - Un acierto en repaso adelantado cuenta en el resumen de la tanda pero no acerca la insignia, lo que puede
    sorprender al usuario.
  - Una segunda autoevaluación con acierto en la misma sesión deja de sumar.
- **Descartado:**
  - Rellenar solo con nuevas: llenaría el aprendizaje de frases a medio aprender y dispararía el nivel 0.
  - Marcar en la tanda qué entradas son repaso adelantado: exigiría una columna nueva y una migración.
  - Que el acierto en repaso adelantado sume racha: acortaría los intervalos de ADR-0025 sin evidencia de
    recuerdo a largo plazo.

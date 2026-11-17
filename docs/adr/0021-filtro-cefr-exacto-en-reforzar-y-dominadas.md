# ADR-0021: Filtro CEFR exacto en "a reforzar" y "dominadas"

- **Estado:** Aceptado
- **Fecha:** 2026-11-17
- **Sustituye a:** ADR-0017 (parcialmente, solo el alcance del filtro CEFR en "a reforzar" y "dominadas")

## Contexto

ADR-0017 extendió el filtro CEFR, antes limitado al bloque de nuevas, a los bloques "a reforzar" y
"dominadas", pero con una condición de **nivel CEFR igual o superior** al elegido en Ajustes, no
igual. La intención era permitir un repaso ligero de frases de niveles más altos ya dominadas,
aunque el usuario bajara temporalmente el nivel CEFR elegido para incorporar material más fácil.

El uso real muestra que esto es contraproducente: si un usuario empieza a practicar en un nivel
demasiado alto (por ejemplo B1) y decide bajar a uno más cómodo (A1), el algoritmo le sigue
ofreciendo las frases de B1 ya iniciadas en los bloques "a reforzar" y "dominadas", porque B1 es
superior a A1 y la regla actual las admite. El usuario esperaba dejar atrás el nivel alto al
bajarlo en Ajustes, y en su lugar sigue viendo frases que considera demasiado difíciles, lo cual
resulta frustrante y contradice la expectativa de que "Ajustes" controla qué frases se practican.

El caso simétrico (subir de nivel) ya funcionaba correctamente: ADR-0017 excluye los niveles
**inferiores** al elegido, así que al subir de A1 a A2 las frases de A1 ya no deberían entrar en
"a reforzar" ni "dominadas". Este ADR no cambia esa parte de la regla.

## Decisión

Se mantienen sin cambios todas las reglas de ADR-0012/ADR-0015/ADR-0016/ADR-0017 salvo la siguiente:

El filtro CEFR de los bloques "a reforzar" y "dominadas" pasa de "nivel CEFR igual o superior al
elegido" a **"nivel CEFR exactamente igual al elegido"**, igual que ya exige el bloque de nuevas.
Una frase cuyo nivel CEFR en el catálogo no coincide con el nivel elegido en Ajustes —ya sea
inferior o superior— no es candidata a ningún bloque de la tanda mientras ese nivel esté elegido.

El progreso de esa frase (nivel de consolidación, racha, `dueAtSession`) no se borra ni se
modifica: solo deja de ser candidata a la tanda mientras el nivel CEFR elegido sea otro. Si el
usuario vuelve a elegir ese nivel CEFR más adelante, la frase vuelve a ser candidata con el mismo
progreso que tenía, como si nunca hubiera dejado de estarlo.

No se añade ningún campo, tabla ni migración: el filtro sigue resolviéndose en memoria, comparando
el nivel CEFR del catálogo de cada frase (inmutable) con el nivel CEFR elegido en Ajustes en el
momento de componer la tanda, exactamente igual que ya ocurre hoy para el bloque de nuevas.

## Consecuencias

- **Positivas:**
  - Bajar el nivel CEFR en Ajustes deja de mostrar frases de un nivel que el usuario considera
    demasiado difícil; "Ajustes" vuelve a controlar de forma predecible qué frases se practican.
  - El comportamiento de los tres bloques (nuevas, a reforzar, dominadas) queda unificado bajo la
    misma regla de filtro CEFR, más fácil de explicar y de razonar.
  - El progreso de otros niveles CEFR no se pierde: cambiar de nivel y volver atrás restaura
    exactamente el mismo progreso, sin lógica adicional.
- **Negativas:**
  - Se pierde el repaso ligero de frases ya dominadas de niveles superiores que ADR-0017 buscaba
    ofrecer al bajar de nivel CEFR; un usuario que domina B1 y baja a A1 para dar soltura al idioma
    no verá ya esas frases de B1 hasta que vuelva a elegir ese nivel.
  - Si el usuario cambia de nivel CEFR con frecuencia, puede percibir que "pierde" frases en las
    que ya había progresado, aunque el progreso siga intacto y solo esté oculto temporalmente.
- **Descartado:**
  - Mantener "igual o superior" solo para el bloque de dominadas (nivel de consolidación 4 o 5) y
    exigir igualdad exacta solo en "a reforzar": se descarta porque introduciría dos reglas CEFR
    distintas en la misma composición, más difícil de explicar al usuario que una regla única, y el
    caso reportado (B1 dominada mostrándose al bajar a A1) ocurre igual en el bloque de dominadas.
  - Añadir un ajuste explícito para que el usuario decida si quiere o no repaso de niveles
    superiores: se descarta por aumentar la complejidad de Ajustes para un matiz que el propio
    informe de uso real señala como indeseado por defecto; puede reconsiderarse en un cambio
    posterior si se pide explícitamente.

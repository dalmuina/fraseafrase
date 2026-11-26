# spaced-repetition Specification

## Purpose

Define las reglas deterministas de racha, certificación y resolución de la autoevaluación, y el repaso espaciado medido en sesiones generadas, según ADR-0012, ADR-0017 y ADR-0025. Son independientes de cómo se guarde el progreso y de cómo se componga la tanda.

Quedan fuera de esta especificación: la composición de la tanda nueva, el catálogo agotado y la primera tanda (`practice-batch`), y la voz TTS (cambio de audio).

## Requirements

### Requirement: Racha persistente entre sesiones

Salvo el acierto sobre una frase aún no pendiente (requisito "Acierto sobre una frase aún no pendiente"), un
acierto MUST incrementar en 1 la racha de una frase. Un fallo sobre una frase con racha mayor que 0 MUST
restarle 1 a la racha (ADR-0024); el efecto de un fallo con racha 0 lo fija "Resolución de la
autoevaluación". La racha MUST NOT reiniciarse por el solo hecho de que la frase entre en una tanda recién
compuesta: persiste de una sesión generada a la siguiente.

#### Scenario: Acierto suma uno a la racha

- **GIVEN** una frase con racha 1 y `dueAtSession` igual o anterior a la sesión actual
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** su racha pasa a 2

#### Scenario: Fallo resta uno a la racha

- **GIVEN** una frase de nivel 2 con racha 2
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** su racha pasa a 1 y su nivel sigue siendo 2

#### Scenario: Fallo reinicia la racha

- **GIVEN** una frase de nivel 2 con racha 1
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** su racha pasa a 0 y su nivel sigue siendo 2

#### Scenario: La racha sobrevive a una sesión generada de por medio

- **GIVEN** una frase con racha 1 que, tras responderla, queda fuera de la tanda activa al generarse la
  siguiente sesión
- **WHEN** la frase vuelve a ofrecerse en una sesión posterior y la pantalla la muestra
- **THEN** su racha sigue siendo 1, no 0

### Requirement: Resolución de la autoevaluación

Cada autoevaluación MUST resolverse al instante, sin esperar a generar la siguiente sesión. Salvo el acierto sobre
una frase aún no pendiente (requisito "Acierto sobre una frase aún no pendiente"): un acierto que hace que la
racha llegue a 3 MUST certificar la frase en ese momento, y un acierto que no certifica MUST fijar su
`dueAtSession` en la sesión actual más la espera intra-nivel de su nivel (ADR-0025). Un fallo MUST restar
un solo acierto (ADR-0024): con racha mayor que 0, la racha baja en 1 y el nivel no cambia; con racha 0 y
nivel ≥ 1, el nivel baja en 1 y la racha queda en 2; con racha 0 y nivel 0, nivel y racha no cambian. Un
fallo, en cualquier caso, MUST fijar su `dueAtSession` en la sesión siguiente.

#### Scenario: Fallo en nivel 3 baja a nivel 2 y fija la sesión siguiente

- **GIVEN** una frase de nivel 3 con racha 0, sesión actual 10
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** la frase queda en nivel 2, racha 2, y `dueAtSession` igual a 11

#### Scenario: Fallo en nivel 1 baja a nivel 0 y fija la sesión siguiente

- **GIVEN** una frase de nivel 1 con racha 0, sesión actual 10
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** la frase queda en nivel 0, racha 2, y `dueAtSession` igual a 11

#### Scenario: Fallo con racha 0 en nivel 5 baja a nivel 4 con racha 2

- **GIVEN** una frase de nivel 5 con racha 0, sesión actual 10
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** la frase queda en nivel 4, racha 2, y `dueAtSession` igual a 11

#### Scenario: Fallo con racha mayor que 0 no baja de nivel

- **GIVEN** una frase de nivel 3 con racha 2, sesión actual 10
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** la frase queda en nivel 3, racha 1, y `dueAtSession` igual a 11

#### Scenario: Fallo en nivel 0 no cambia el nivel pero sí fija la sesión siguiente

- **GIVEN** una frase de nivel 0 con racha 0, sesión actual 10
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** la frase permanece en nivel 0, racha 0, con `dueAtSession` igual a 11

#### Scenario: Fallo en nivel 0 con racha mayor que 0 resta uno a la racha

- **GIVEN** una frase de nivel 0 con racha 2, sesión actual 10
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** la frase queda en nivel 0, racha 1, y `dueAtSession` igual a 11

#### Scenario: Acierto que certifica sube de nivel y entra en enfriamiento

- **GIVEN** una frase de nivel 1 con racha 2, sesión actual 10
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** la frase queda en nivel 2, racha 0, y `dueAtSession` igual a 12 (sesión actual 10 más el
  enfriamiento del nivel 2, que es 2 sesiones)

#### Scenario: Acierto que no certifica fija la sesión siguiente

- **GIVEN** una frase de nivel 1 con racha 0, sesión actual 10
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** la frase permanece en nivel 1, racha 1, y `dueAtSession` igual a 11 (sesión 10 más la espera
  intra-nivel del nivel 1, que es 1 sesión)

#### Scenario: Acierto que no certifica en nivel 3 espera dos sesiones

- **GIVEN** una frase de nivel 3 con racha 1, sesión actual 10
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** la frase permanece en nivel 3, racha 2, y `dueAtSession` igual a 12

#### Scenario: Acierto que no certifica en nivel 5 espera ocho sesiones

- **GIVEN** una frase de nivel 5 con racha 0, sesión actual 10
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** la frase permanece en nivel 5, racha 1, y `dueAtSession` igual a 18

#### Scenario: Una frase nueva certificada por primera vez pasa a nivel 1

- **GIVEN** una frase de nivel 0 con racha 2, sesión actual 10
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** la frase queda en nivel 1, racha 0, y `dueAtSession` igual a 11 (sesión 10 + enfriamiento de
  nivel 1, que es 1 sesión)

### Requirement: Frase respondida en la tanda

Una frase MUST considerarse respondida en una tanda cuando haya recibido al menos una autoevaluación
(acierto o fallo) en esa tanda. Una frase sin autoevaluaciones en la tanda MUST considerarse nunca
respondida.

#### Scenario: Frase con un fallo cuenta como respondida

- **GIVEN** una frase de nivel 2 que en la tanda activa solo ha recibido un fallo
- **WHEN** se evalúa si está respondida
- **THEN** la frase queda marcada como respondida en esa tanda

#### Scenario: Frase sin autoevaluaciones cuenta como nunca respondida

- **GIVEN** una frase de la tanda sin autoevaluaciones en esa tanda
- **WHEN** se evalúa si está respondida
- **THEN** la frase cuenta como nunca respondida

### Requirement: Niveles de consolidación

El nivel de una frase MUST ser un entero entre 0 y 5 inclusive. Un nivel 0 MUST significar que la frase es nueva o está en aprendizaje.

#### Scenario: Nivel nunca baja de 0

- **GIVEN** una frase de nivel 0 con racha 0 que nunca fue respondida en la tanda activa
- **WHEN** se evalúa su nivel
- **THEN** la frase queda en nivel 0

### Requirement: Enfriamiento en sesiones tras certificar

La función que calcula el enfriamiento tras certificar MUST devolver, para el nivel alcanzado: 1 sesión para
nivel 1, 2 sesiones para nivel 2, 4 sesiones para nivel 3, 8 sesiones para nivel 4 y 16 sesiones para nivel 5.

#### Scenario: Enfriamiento de cada nivel

- **GIVEN** los niveles 1, 2, 3, 4 y 5
- **WHEN** se calcula su enfriamiento en sesiones
- **THEN** los resultados son 1, 2, 4, 8 y 16 respectivamente

### Requirement: Frase pendiente de repaso por sesión

Una frase MUST considerarse pendiente en una sesión `currentSession` cuando su `dueAtSession` es igual o
anterior a `currentSession`, sin excepción de nivel: una frase de nivel 0 también puede estar pendiente si fue
fallada recientemente.

#### Scenario: Sesión igual al vencimiento está pendiente

- **GIVEN** una frase con `dueAtSession` igual a 12
- **WHEN** se evalúa si está pendiente en la sesión 12
- **THEN** está pendiente

#### Scenario: Sesión anterior al vencimiento no está pendiente

- **GIVEN** una frase con `dueAtSession` igual a 12
- **WHEN** se evalúa si está pendiente en la sesión 11
- **THEN** no está pendiente

#### Scenario: Una frase de nivel 0 recién fallada está pendiente en la sesión siguiente

- **GIVEN** una frase de nivel 0 con `dueAtSession` igual a 11, fijado por un fallo en la sesión 10
- **WHEN** se evalúa si está pendiente en la sesión 11
- **THEN** está pendiente

### Requirement: Invariantes de PhraseProgress con sesión de vencimiento

Una frase MUST NOT poder construirse en un estado incoherente: el nivel MUST estar entre 0 y 5, la racha
MUST NOT ser negativa, y `dueAtSession` MUST NOT ser negativo.

#### Scenario: Nivel fuera de rango se rechaza

- **GIVEN** un nivel -1 o un nivel 6
- **WHEN** se construye un `Level` con ese valor
- **THEN** se lanza `IllegalArgumentException`

#### Scenario: Niveles límite se aceptan

- **GIVEN** un nivel 0 o un nivel 5
- **WHEN** se construye un `Level` con ese valor
- **THEN** se acepta el valor sin error

#### Scenario: Racha negativa se rechaza

- **GIVEN** una racha -1
- **WHEN** se construye un `Streak` con ese valor
- **THEN** se lanza `IllegalArgumentException`

#### Scenario: `dueAtSession` negativo se rechaza

- **GIVEN** un `dueAtSession` igual a -1
- **WHEN** se construye la frase
- **THEN** se lanza `IllegalArgumentException`

### Requirement: Reversión de la última autoevaluación

Revertir la autoevaluación más reciente de toda la tanda MUST restaurar el nivel, la racha, el
`dueAtSession` y el registro de respuestas (última sesión respondida, sesiones seguidas y última sesión
fallada) que la frase afectada tenía inmediatamente antes de esa autoevaluación. Solo MUST
poder revertirse la autoevaluación más reciente de toda la tanda. Tras revertirla, o tras cualquier
autoevaluación posterior (de esa frase o de otra), la posibilidad de revertir esa autoevaluación
concreta MUST desaparecer.

#### Scenario: Revertir un fallo restaura el nivel, la racha y el vencimiento anteriores

- **GIVEN** una frase de nivel 3 con racha 0 y `dueAtSession` 5, sesión actual 10, que el usuario
  autoevalúa con fallo y queda en nivel 2, racha 2 y `dueAtSession` 11
- **WHEN** se revierte esa autoevaluación
- **THEN** la frase vuelve a nivel 3, racha 0 y `dueAtSession` 5

#### Scenario: Revertir un acierto que certificó restaura el nivel y la racha anteriores

- **GIVEN** una frase de nivel 1 con racha 2 y `dueAtSession` 9, sesión actual 10, que el usuario
  autoevalúa con acierto y queda en nivel 2, racha 0 y `dueAtSession` 12
- **WHEN** se revierte esa autoevaluación
- **THEN** la frase vuelve a nivel 1, racha 2 y `dueAtSession` 9

#### Scenario: Revertir un fallo restaura el registro de respuestas

- **GIVEN** una frase respondida por última vez en la sesión 9 con 2 sesiones seguidas y fallada en la sesión 8,
  sesión actual 10, que el usuario autoevalúa con fallo y queda con última sesión respondida 10, 3 sesiones
  seguidas y última sesión fallada 10
- **WHEN** se revierte esa autoevaluación
- **THEN** la frase vuelve a última sesión respondida 9, 2 sesiones seguidas y última sesión fallada 8

#### Scenario: Solo la autoevaluación más reciente de toda la tanda puede revertirse

- **GIVEN** una tanda en la que el usuario autoevalúa primero la frase A y después la frase B
- **WHEN** se pide revertir la autoevaluación de A
- **THEN** no es posible, porque B es la autoevaluación más reciente de toda la tanda, no A

#### Scenario: La reversión no se acumula

- **GIVEN** una autoevaluación que ya fue revertida
- **WHEN** se pide revertir de nuevo
- **THEN** no es posible, porque no queda ninguna autoevaluación pendiente de revertir

### Requirement: Espera intra-nivel tras un acierto que no certifica

La función que calcula la espera tras un acierto que no certifica MUST devolver, según el nivel actual de la
frase: 1 sesión para los niveles 0, 1 y 2; 2 sesiones para el nivel 3; 4 sesiones para el nivel 4; y 8
sesiones para el nivel 5. Es la mitad del enfriamiento de ese nivel, con un mínimo de 1 sesión (ADR-0025).

#### Scenario: Espera intra-nivel de cada nivel

- **GIVEN** los niveles 0, 1, 2, 3, 4 y 5
- **WHEN** se calcula su espera intra-nivel en sesiones
- **THEN** los resultados son 1, 1, 1, 2, 4 y 8 respectivamente

### Requirement: Acierto sobre una frase aún no pendiente

Un acierto sobre una frase cuyo `dueAtSession` es posterior a la sesión actual en el momento de responder
(repaso adelantado, o una frase ya respondida en la misma sesión) MUST NOT cambiar su nivel, su racha ni su
`dueAtSession`, y MUST marcarla como respondida en la tanda. Un fallo sobre esa frase MUST resolverse como
cualquier otro fallo (ADR-0024, ADR-0026).

#### Scenario: Acierto en repaso adelantado no suma

- **GIVEN** una frase de nivel 2 con racha 1 y `dueAtSession` 14, sesión actual 12
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** la frase permanece en nivel 2, racha 1 y `dueAtSession` 14, y queda respondida en la tanda

#### Scenario: Acierto en repaso adelantado con racha 2 no certifica

- **GIVEN** una frase de nivel 1 con racha 2 y `dueAtSession` 14, sesión actual 12
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** la frase permanece en nivel 1, racha 2 y `dueAtSession` 14

#### Scenario: Fallo en repaso adelantado cuenta

- **GIVEN** una frase de nivel 2 con racha 1 y `dueAtSession` 14, sesión actual 12
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** la frase queda en nivel 2, racha 0 y `dueAtSession` 13

#### Scenario: Un segundo acierto en la misma sesión no suma

- **GIVEN** una frase de nivel 1 con racha 0 y `dueAtSession` 10, sesión actual 10, que el usuario autoevalúa con
  acierto y queda con racha 1 y `dueAtSession` 11
- **WHEN** el usuario vuelve a autoevaluarla con acierto en la misma sesión
- **THEN** la frase permanece en nivel 1, racha 1 y `dueAtSession` 11

#### Scenario: Revertir un acierto en repaso adelantado deja el progreso igual

- **GIVEN** una frase de nivel 2 con racha 1 y `dueAtSession` 14, sesión actual 12, que el usuario autoevalúa con
  acierto
- **WHEN** se revierte esa autoevaluación
- **THEN** la frase sigue en nivel 2, racha 1 y `dueAtSession` 14

### Requirement: Registro de respuestas por frase

Cada frase con progreso MUST registrar su última sesión respondida, sus sesiones seguidas y su última sesión
fallada (ADR-0027). Al autoevaluarla en la sesión `s`, aunque aún no esté pendiente, la última sesión respondida
MUST pasar a `s`; las sesiones seguidas MUST subir en 1 si era `s − 1`, no cambiar si era `s` y pasar a 1 en otro
caso. Un fallo MUST fijar la última sesión fallada en `s`; un acierto no la cambia.

#### Scenario: La primera respuesta inicia el registro

- **GIVEN** una frase nueva de la tanda, nunca respondida, en la sesión 7
- **WHEN** el usuario la autoevalúa con acierto
- **THEN** su última sesión respondida es 7, sus sesiones seguidas son 1 y no tiene última sesión fallada

#### Scenario: Responder en la sesión siguiente suma una sesión seguida

- **GIVEN** una frase respondida por última vez en la sesión 6 con 3 sesiones seguidas, en la sesión 7
- **WHEN** el usuario la autoevalúa con acierto
- **THEN** su última sesión respondida es 7 y sus sesiones seguidas son 4

#### Scenario: Una segunda respuesta en la misma sesión no suma

- **GIVEN** una frase respondida en la sesión 7 con 4 sesiones seguidas, en la sesión 7
- **WHEN** el usuario la autoevalúa de nuevo
- **THEN** su última sesión respondida sigue siendo 7 y sus sesiones seguidas siguen siendo 4

#### Scenario: Una sesión sin responderla reinicia las sesiones seguidas

- **GIVEN** una frase respondida por última vez en la sesión 4 con 5 sesiones seguidas, en la sesión 7
- **WHEN** el usuario la autoevalúa con acierto
- **THEN** su última sesión respondida es 7 y sus sesiones seguidas son 1

#### Scenario: Un fallo registra la sesión fallada

- **GIVEN** una frase de nivel 2 respondida por última vez en la sesión 6 sin fallos previos, en la sesión 7
- **WHEN** el usuario la autoevalúa con fallo
- **THEN** su última sesión fallada es 7 y su última sesión respondida es 7

#### Scenario: Un acierto conserva la última sesión fallada

- **GIVEN** una frase fallada por última vez en la sesión 5, en la sesión 7
- **WHEN** el usuario la autoevalúa con acierto
- **THEN** su última sesión fallada sigue siendo 5

#### Scenario: Un acierto en repaso adelantado también se registra

- **GIVEN** una frase de nivel 2 con racha 1 y `dueAtSession` 9, respondida por última vez en la sesión 6 con 1
  sesión seguida, en la sesión 7
- **WHEN** el usuario la autoevalúa con acierto
- **THEN** su nivel, su racha y su `dueAtSession` no cambian, su última sesión respondida es 7 y sus sesiones
  seguidas son 2

### Requirement: Componer una tanda no cambia el registro de respuestas

Generar sesión o recomponer la tanda activa MUST NOT cambiar el registro de respuestas de ninguna frase, entre o
no en la tanda nueva: solo una autoevaluación o su reversión lo cambian.

#### Scenario: Generar sesión no cambia el registro

- **GIVEN** una frase respondida por última vez en la sesión 6 con 2 sesiones seguidas y fallada en la sesión 6
- **WHEN** se genera la sesión 7 y la frase entra en la tanda
- **THEN** su última sesión respondida sigue siendo 6, sus sesiones seguidas 2 y su última sesión fallada 6

### Requirement: Invariantes del registro de respuestas

El registro de respuestas de una frase MUST rechazarse si alguna de sus sesiones es negativa, si tiene sesiones
seguidas sin última sesión respondida o una última sesión respondida con menos de 1 sesión seguida, o si su última
sesión fallada es posterior a su última sesión respondida o existe sin ella.

#### Scenario: Sesión respondida negativa se rechaza

- **GIVEN** un registro con última sesión respondida −1
- **WHEN** se construye
- **THEN** se rechaza

#### Scenario: Sesiones seguidas sin sesión respondida se rechazan

- **GIVEN** un registro sin última sesión respondida y con 2 sesiones seguidas
- **WHEN** se construye
- **THEN** se rechaza

#### Scenario: Sesión respondida con cero sesiones seguidas se rechaza

- **GIVEN** un registro con última sesión respondida 4 y 0 sesiones seguidas
- **WHEN** se construye
- **THEN** se rechaza

#### Scenario: Sesión fallada posterior a la respondida se rechaza

- **GIVEN** un registro con última sesión respondida 4, 1 sesión seguida y última sesión fallada 5
- **WHEN** se construye
- **THEN** se rechaza

#### Scenario: Registro coherente se acepta

- **GIVEN** un registro con última sesión respondida 0, 1 sesión seguida y última sesión fallada 0
- **WHEN** se construye
- **THEN** se acepta

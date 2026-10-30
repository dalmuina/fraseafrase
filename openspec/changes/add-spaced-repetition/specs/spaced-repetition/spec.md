## Purpose

Define las reglas deterministas de racha, certificación, resolución al cerrar una tanda y niveles con intervalos de repaso en días naturales, según ADR-0012. Son independientes de cómo se guarde el progreso y de cómo se componga la tanda.

Quedan fuera de esta especificación: la composición de la tanda nueva, el catálogo agotado y la primera tanda (`practice-batch`), y la voz TTS (cambio de audio).

## ADDED Requirements

### Requirement: Racha de aciertos dentro de la tanda

El sistema MUST incrementar en 1 la racha de una frase cuando el usuario se autoevalúa con acierto, y MUST poner la racha a 0 cuando se autoevalúa con fallo.

#### Scenario: Acierto suma uno a la racha

- **GIVEN** una frase con racha 1 en su tanda activa
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** su racha pasa a 2

#### Scenario: Fallo reinicia la racha

- **GIVEN** una frase con racha 2 en su tanda activa
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** su racha pasa a 0

### Requirement: Certificación por racha al cerrar la tanda

Una frase MUST estar certificada en el momento de cerrar la tanda cuando su racha es mayor o igual que 3. Una frase con racha menor que 3 MUST NOT estar certificada.

#### Scenario: Racha de tres certifica la frase al cerrar

- **GIVEN** una frase con racha 3 en la tanda activa
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase está certificada

#### Scenario: Racha de dos no certifica la frase al cerrar

- **GIVEN** una frase con racha 2 en la tanda activa
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase no está certificada

#### Scenario: Un fallo tras tres aciertos impide la certificación

- **GIVEN** una frase con racha 3 en la tanda activa
- **WHEN** el usuario se autoevalúa con fallo y después se cierra la tanda el 10-03-2026
- **THEN** la frase no está certificada y su racha es 0

### Requirement: Frase respondida en la tanda

Una frase MUST considerarse respondida en una tanda cuando haya recibido al menos una autoevaluación (acierto o fallo) en esa tanda. Una frase sin autoevaluaciones en la tanda MUST considerarse nunca respondida.

#### Scenario: Frase con un fallo cuenta como respondida

- **GIVEN** una frase que en la tanda activa solo ha recibido un fallo
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase queda marcada como respondida en esa tanda y, al cerrar la tanda, sale arrastrada en nivel 0 sin próximo repaso

#### Scenario: Frase sin autoevaluaciones cuenta como nunca respondida

- **GIVEN** una frase de la tanda sin autoevaluaciones en esa tanda
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase queda marcada como nunca respondida en esa tanda

### Requirement: Resolución de frase certificada al cerrar la tanda

Al cerrar la tanda, una frase certificada MUST subir su nivel a `min(nivel + 1, 5)`, MUST fijar su próximo repaso en la fecha de cierre más el intervalo del nuevo nivel, y MUST salir de la tanda.

#### Scenario: Certificada de nivel 0 pasa a nivel 1

- **GIVEN** una frase de nivel 0 con racha 3 en la tanda activa
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase queda en nivel 1 con próximo repaso el 11-03-2026 y sale de la tanda

#### Scenario: Certificada de nivel 1 pasa a nivel 2

- **GIVEN** una frase de nivel 1 con racha 3 en la tanda activa
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase queda en nivel 2 con próximo repaso el 13-03-2026 y sale de la tanda

#### Scenario: Certificada de nivel 2 pasa a nivel 3

- **GIVEN** una frase de nivel 2 con racha 3 en la tanda activa
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase queda en nivel 3 con próximo repaso el 17-03-2026 y sale de la tanda

#### Scenario: Certificada de nivel 3 pasa a nivel 4

- **GIVEN** una frase de nivel 3 con racha 4 en la tanda activa
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase queda en nivel 4 con próximo repaso el 24-03-2026 y sale de la tanda

#### Scenario: Certificada de nivel 4 pasa a nivel 5

- **GIVEN** una frase de nivel 4 con racha 3 en la tanda activa
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase queda en nivel 5 con próximo repaso el 09-04-2026 y sale de la tanda

#### Scenario: Certificada de nivel 5 se mantiene en nivel 5

- **GIVEN** una frase de nivel 5 con racha 3 en la tanda activa
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase queda en nivel 5 con próximo repaso el 09-04-2026 y sale de la tanda

### Requirement: Resolución de frase respondida no certificada al cerrar la tanda

Al cerrar la tanda, una frase respondida y no certificada MUST pasar a nivel 0, MUST quedar sin fecha de próximo repaso y MUST arrastrarse a la siguiente tanda.

#### Scenario: Respondida no certificada de nivel 4 vuelve a nivel 0

- **GIVEN** una frase de nivel 4 con racha 1 que fue respondida en la tanda activa
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase queda en nivel 0, sin próximo repaso, y se arrastra a la siguiente tanda

#### Scenario: Respondida no certificada de nivel 5 vuelve a nivel 0

- **GIVEN** una frase de nivel 5 con racha 0 que fue respondida en la tanda activa
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase queda en nivel 0, sin próximo repaso, y se arrastra a la siguiente tanda

#### Scenario: Respondida no certificada de nivel 0 permanece en nivel 0

- **GIVEN** una frase de nivel 0 con racha 0 que fue respondida en la tanda activa
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase queda en nivel 0, sin próximo repaso, y se arrastra a la siguiente tanda

### Requirement: Resolución de frase nunca respondida al cerrar la tanda

Al cerrar la tanda, una frase nunca respondida en esa tanda MUST conservar su nivel y su próximo repaso sin cambios, y MUST arrastrarse a la siguiente tanda.

#### Scenario: Nunca respondida conserva su nivel y su repaso

- **GIVEN** una frase de nivel 3 con próximo repaso el 20-03-2026 que no fue respondida en la tanda activa
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase sigue en nivel 3 con próximo repaso el 20-03-2026 y se arrastra a la siguiente tanda

### Requirement: Estado de la frase tras cerrar la tanda

Al cerrar la tanda, toda frase, certificada o arrastrada, MUST quedar con racha 0 y con `answeredInBatch` a falso. El comportamiento de la racha de una frase certificada cuando vuelve en una tanda posterior se especifica en `practice-batch`, fuera de este cambio.

#### Scenario: Frase arrastrada queda con racha 0 y sin respuestas

- **GIVEN** una frase de nivel 2 con racha 2 que fue respondida en la tanda activa
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase se arrastra con racha 0 y queda como nunca respondida en la nueva tanda

#### Scenario: Frase certificada sale con racha 0 y sin respuestas

- **GIVEN** una frase de nivel 1 con racha 3 que fue respondida en la tanda activa
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase sale de la tanda con racha 0 y `answeredInBatch` a falso

### Requirement: Niveles de consolidación

El nivel de una frase MUST ser un entero entre 0 y 5 inclusive. Un nivel 0 MUST significar que la frase es nueva o está en aprendizaje.

#### Scenario: Nivel nunca baja de 0

- **GIVEN** una frase de nivel 0 con racha 0 que nunca fue respondida en la tanda activa
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase queda en nivel 0

### Requirement: Próximo repaso según el nivel

La función `nextReviewAfter(level, from)` MUST devolver la fecha `from` más los días de intervalo del nivel: 1 día para nivel 1, 3 días para nivel 2, 7 días para nivel 3, 14 días para nivel 4 y 30 días para nivel 5. Para nivel 0 MUST devolver nulo. Los días MUST contarse como días naturales.

#### Scenario: Intervalo del nivel 1

- **GIVEN** la fecha 10-03-2026
- **WHEN** se calcula `nextReviewAfter` para nivel 1
- **THEN** el resultado es 11-03-2026

#### Scenario: Intervalo del nivel 2

- **GIVEN** la fecha 10-03-2026
- **WHEN** se calcula `nextReviewAfter` para nivel 2
- **THEN** el resultado es 13-03-2026

#### Scenario: Intervalo del nivel 3

- **GIVEN** la fecha 10-03-2026
- **WHEN** se calcula `nextReviewAfter` para nivel 3
- **THEN** el resultado es 17-03-2026

#### Scenario: Intervalo del nivel 4

- **GIVEN** la fecha 10-03-2026
- **WHEN** se calcula `nextReviewAfter` para nivel 4
- **THEN** el resultado es 24-03-2026

#### Scenario: Intervalo del nivel 5

- **GIVEN** la fecha 10-03-2026
- **WHEN** se calcula `nextReviewAfter` para nivel 5
- **THEN** el resultado es 09-04-2026

#### Scenario: Nivel 0 no tiene próximo repaso

- **GIVEN** la fecha 10-03-2026
- **WHEN** se calcula `nextReviewAfter` para nivel 0
- **THEN** el resultado es nulo

#### Scenario: Cambio de año en el intervalo del nivel 1

- **GIVEN** la fecha 31-12-2026
- **WHEN** se calcula `nextReviewAfter` para nivel 1
- **THEN** el resultado es 01-01-2027

#### Scenario: Cambio de año en el intervalo del nivel 5

- **GIVEN** la fecha 15-12-2026
- **WHEN** se calcula `nextReviewAfter` para nivel 5
- **THEN** el resultado es 14-01-2027

#### Scenario: Año bisiesto en el intervalo del nivel 1

- **GIVEN** la fecha 28-02-2028
- **WHEN** se calcula `nextReviewAfter` para nivel 1
- **THEN** el resultado es 29-02-2028

### Requirement: Frase pendiente de repaso

Una frase MUST considerarse pendiente en una fecha `today` cuando su nivel es mayor o igual que 1 y su próximo repaso es igual o anterior a `today`. Una frase de nivel 0 MUST NOT considerarse pendiente. La evaluación MUST devolver el mismo resultado para la misma frase y la misma fecha.

#### Scenario: Repaso de hoy está pendiente

- **GIVEN** una frase de nivel 2 con próximo repaso el 13-03-2026
- **WHEN** se evalúa si está pendiente el 13-03-2026
- **THEN** está pendiente

#### Scenario: Repaso futuro no está pendiente

- **GIVEN** una frase de nivel 2 con próximo repaso el 14-03-2026
- **WHEN** se evalúa si está pendiente el 13-03-2026
- **THEN** no está pendiente

#### Scenario: Cambio de día hace pendiente una frase

- **GIVEN** una frase de nivel 2 con próximo repaso el 13-03-2026
- **WHEN** se evalúa si está pendiente el 12-03-2026 y después el 13-03-2026
- **THEN** no está pendiente el 12-03-2026 y sí está pendiente el 13-03-2026

#### Scenario: Nivel 0 nunca está pendiente

- **GIVEN** una frase de nivel 0 sin próximo repaso
- **WHEN** se evalúa si está pendiente el 13-03-2026
- **THEN** no está pendiente

### Requirement: Invariantes de PhraseProgress

Una frase MUST NOT poder construirse en un estado incoherente: el nivel MUST estar entre 0 y 5, un nivel 0 MUST NOT tener próximo repaso, un nivel de 1 a 5 MUST tener próximo repaso, una racha mayor que 0 MUST requerir que la frase haya sido respondida en la tanda, y una racha MUST NOT ser negativa.

#### Scenario: Nivel fuera de rango se rechaza

- **GIVEN** un nivel -1 o un nivel 6
- **WHEN** se construye un `Level` con ese valor
- **THEN** se lanza `IllegalArgumentException`

#### Scenario: Niveles límite se aceptan

- **GIVEN** un nivel 0 o un nivel 5
- **WHEN** se construye un `Level` con ese valor
- **THEN** se acepta el valor sin error

#### Scenario: Nivel 0 con próximo repaso se rechaza

- **GIVEN** un nivel 0 con próximo repaso el 13-03-2026
- **WHEN** se construye la frase
- **THEN** se lanza `IllegalArgumentException`

#### Scenario: Nivel 1 sin próximo repaso se rechaza

- **GIVEN** un nivel 1 sin próximo repaso
- **WHEN** se construye la frase
- **THEN** se lanza `IllegalArgumentException`

#### Scenario: Racha mayor que 0 sin respuesta se rechaza

- **GIVEN** una racha 1 con `answeredInBatch` a falso
- **WHEN** se construye la frase
- **THEN** se lanza `IllegalArgumentException`

#### Scenario: Racha negativa se rechaza

- **GIVEN** una racha -1
- **WHEN** se construye un `Streak` con ese valor
- **THEN** se lanza `IllegalArgumentException`

### Requirement: Cierre de una tanda sin frases

Cerrar una tanda sin frases MUST devolver una lista vacía de resultados.

#### Scenario: Tanda vacía devuelve lista vacía

- **GIVEN** una tanda sin frases
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** el resultado es una lista vacía

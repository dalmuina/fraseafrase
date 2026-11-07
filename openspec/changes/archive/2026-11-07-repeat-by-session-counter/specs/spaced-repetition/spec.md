# Spec Delta

## REMOVED Requirements

### Requirement: Racha de aciertos dentro de la tanda

**Reason**: la racha dejaba de "dentro de la tanda" a persistir entre sesiones generadas; una tanda ya no es la
unidad que la acota.
**Migration**: ver "Racha persistente entre sesiones" de este delta.

### Requirement: Certificación por racha al cerrar la tanda

**Reason**: la certificación ya no espera al cierre de una tanda: se resuelve al instante, en la misma
autoevaluación que hace que la racha llegue a 3, porque ahora cada frase solo se ofrece una vez por sesión
generada (una frase ya no puede fallar y acertar varias veces dentro de la misma tanda).
**Migration**: ver "Resolución de la autoevaluación" de este delta.

### Requirement: Resolución de frase certificada al cerrar la tanda

**Reason**: sustituida por la resolución instantánea en la autoevaluación.
**Migration**: ver "Resolución de la autoevaluación" de este delta.

### Requirement: Resolución de frase respondida no certificada al cerrar la tanda

**Reason**: sustituida por la resolución instantánea en la autoevaluación.
**Migration**: ver "Resolución de la autoevaluación" de este delta.

### Requirement: Resolución de frase nunca respondida al cerrar la tanda

**Reason**: ya no hay un paso de resolución al cerrar; una frase nunca respondida simplemente conserva su
`dueAtSession` y su nivel, sin que nada la toque.
**Migration**: ninguna; no hay un caso equivalente que especificar, porque no hay nada que "cerrar".

### Requirement: Estado de la frase tras cerrar la tanda

**Reason**: la racha ya no se pone a 0 al generar la siguiente sesión; persiste hasta un fallo real.
**Migration**: ver "Racha persistente entre sesiones" de este delta.

### Requirement: Próximo repaso según el nivel

**Reason**: el repaso se mide en sesiones generadas, no en días naturales.
**Migration**: ver "Enfriamiento en sesiones tras certificar" de este delta.

### Requirement: Frase pendiente de repaso

**Reason**: sustituida por el equivalente en sesiones, que además deja de excluir el nivel 0 (una frase de
nivel 0 recién fallada también debe poder estar pendiente).
**Migration**: ver "Frase pendiente de repaso por sesión" de este delta.

### Requirement: Cierre de una tanda sin frases

**Reason**: ya no existe una función de resolución de cierre que reciba la lista de la tanda y devuelva
resultados; cada frase se resuelve en su propia autoevaluación.
**Migration**: ninguna; no hay un caso equivalente que especificar.

### Requirement: Invariantes de PhraseProgress

**Reason**: `nextReview` se sustituye por `dueAtSession`, que ya no está ligado al nivel (una frase de nivel 0
también puede tener un `dueAtSession` concreto), así que las invariantes "nivel 0 sin próximo repaso" y
"nivel 1-5 con próximo repaso" dejan de aplicar; la invariante "racha > 0 exige respondida en la tanda"
también se retira, porque la racha ya no depende de la tanda activa.
**Migration**: ver "Invariantes de PhraseProgress con sesión de vencimiento" de este delta.

### Requirement: Bajada instantánea de nivel ante un fallo

**Reason**: el próximo repaso tras un fallo se mide en sesiones generadas, no en días naturales, y ahora se
fija igual en todos los niveles (sesión actual más 1), no según el intervalo del nivel resultante.
**Migration**: ver "Resolución de la autoevaluación" de este delta, que cubre la bajada de nivel y la fijación
de `dueAtSession` ante un fallo, junto con el resto de la resolución de cada autoevaluación.

## ADDED Requirements

### Requirement: Racha persistente entre sesiones

Un acierto MUST incrementar en 1 la racha de una frase; un fallo MUST ponerla a 0. La racha MUST NOT
reiniciarse por el solo hecho de que la frase entre en una tanda recién compuesta: persiste de una sesión
generada a la siguiente hasta que un fallo real la reinicie.

#### Scenario: Acierto suma uno a la racha

- **GIVEN** una frase con racha 1
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** su racha pasa a 2

#### Scenario: Fallo reinicia la racha

- **GIVEN** una frase con racha 2
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** su racha pasa a 0

#### Scenario: La racha sobrevive a una sesión generada de por medio

- **GIVEN** una frase con racha 1 que, tras responderla, queda fuera de la tanda activa al generarse la
  siguiente sesión
- **WHEN** la frase vuelve a ofrecerse en una sesión posterior y la pantalla la muestra
- **THEN** su racha sigue siendo 1, no 0

### Requirement: Resolución de la autoevaluación

Cada autoevaluación MUST resolverse al instante, sin esperar a generar la siguiente sesión. Un acierto que
hace que la racha llegue a 3 MUST certificar la frase en ese momento. Un fallo, en cualquier nivel, MUST
fijar su `dueAtSession` en la sesión siguiente, para que pueda volver a ofrecerse sin esperar a ningún
enfriamiento.

#### Scenario: Fallo en nivel 3 baja a nivel 2 y fija la sesión siguiente

- **GIVEN** una frase de nivel 3, sesión actual 10
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** la frase queda en nivel 2 con `dueAtSession` igual a 11

#### Scenario: Fallo en nivel 1 baja a nivel 0 y fija la sesión siguiente

- **GIVEN** una frase de nivel 1, sesión actual 10
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** la frase queda en nivel 0 con `dueAtSession` igual a 11

#### Scenario: Fallo en nivel 0 no cambia el nivel pero sí fija la sesión siguiente

- **GIVEN** una frase de nivel 0, sesión actual 10
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** la frase permanece en nivel 0 con `dueAtSession` igual a 11

#### Scenario: Acierto que certifica sube de nivel y entra en enfriamiento

- **GIVEN** una frase de nivel 1 con racha 2, sesión actual 10
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** la frase queda en nivel 2, racha 0, y `dueAtSession` igual a 12 (sesión actual 10 más el
  enfriamiento del nivel 2, que es 2 sesiones)

#### Scenario: Acierto que no certifica fija la sesión siguiente

- **GIVEN** una frase de nivel 1 con racha 0, sesión actual 10
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** la frase permanece en nivel 1, racha 1, y `dueAtSession` igual a 11

#### Scenario: Una frase nueva certificada por primera vez pasa a nivel 1

- **GIVEN** una frase de nivel 0 con racha 2, sesión actual 10
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** la frase queda en nivel 1, racha 0, y `dueAtSession` igual a 11 (sesión 10 + enfriamiento de
  nivel 1, que es 1 sesión)

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

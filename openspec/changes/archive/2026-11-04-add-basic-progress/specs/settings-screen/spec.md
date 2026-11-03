# Spec Delta

## ADDED Requirements

### Requirement: Edición descartada al salir sin guardar

Si el usuario sale de la pantalla de ajustes sin confirmar el guardado, la edición en curso MUST
descartarse: una lectura posterior de los ajustes MUST NOT reflejarla.

#### Scenario: Salir sin guardar no persiste la edición

- **GIVEN** la pantalla de ajustes abierta con nivel CEFR A1 y N igual a 20 guardados, y el usuario
  edita el tamaño de tanda a 15 sin confirmar el guardado
- **WHEN** el usuario sale de la pantalla de ajustes
- **THEN** una lectura posterior de los ajustes devuelve N igual a 20

## REMOVED Requirements

### Requirement: Acceso a la pantalla de ajustes desde la práctica

**Reason**: La navegación entre práctica y ajustes deja de ser una acción de apertura con *back
stack*; ambas pantallas pasan a ser pestañas de la barra de navegación inferior (`app-navigation`).

**Migration**: Usar el requisito "Cambio de pestaña" de `app-navigation` para llegar a los ajustes
desde cualquier otro destino.

### Requirement: Vuelta a la práctica desde los ajustes

**Reason**: Volver ya no es una acción de retroceso propia de la pantalla de ajustes; es
seleccionar la pestaña de práctica en la barra de navegación inferior (`app-navigation`), igual
que para cualquier otro destino.

**Migration**: Usar el requisito "Volver a la práctica desde cualquier destino" de `app-navigation`
para el cambio de pantalla. El comportamiento de no perder ediciones sin guardar se conserva en el
nuevo requisito "Edición descartada al salir sin guardar" de esta misma especificación.

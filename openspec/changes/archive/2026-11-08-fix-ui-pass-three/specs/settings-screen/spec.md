# Spec Delta

## ADDED Requirements

### Requirement: Alineación visual del stepper del tamaño de tanda

Los controles de disminuir y aumentar el tamaño de tanda N MUST tener su borde superior alineado con
el borde superior del campo de texto editable adyacente, de modo que ambos arranquen a la misma
altura visual. Esta alineación MUST mantenerse aunque el campo de texto reserve espacio adicional
por debajo de su línea de entrada, por ejemplo para mostrar el texto de error del tamaño de tanda
no válido.

#### Scenario: Los controles se alinean con el campo de texto sin error visible

- **GIVEN** la pantalla de ajustes abierta con un tamaño de tanda válido, sin texto de error visible
- **WHEN** la pantalla se muestra
- **THEN** el borde superior de los controles de disminuir y aumentar coincide con el borde superior
  del campo de texto del tamaño de tanda

#### Scenario: Los controles se mantienen alineados cuando el campo muestra el texto de error

- **GIVEN** la pantalla de ajustes abierta con un tamaño de tanda no válido, con su texto de error
  visible bajo el campo
- **WHEN** la pantalla se muestra
- **THEN** el borde superior de los controles de disminuir y aumentar sigue coincidiendo con el
  borde superior del campo de texto del tamaño de tanda

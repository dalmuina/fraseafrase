# Spec Delta

## ADDED Requirements

### Requirement: Barra de navegación visible sobre las barras del sistema

La barra de navegación inferior MUST permanecer completamente visible por encima de las barras del
sistema (barra de gestos o barra de navegación de 3 botones), sea cual sea su altura o la del inset
del sistema en ese dispositivo. Ningún icono ni etiqueta de sus tres destinos MUST quedar oculto o
recortado por esas barras.

#### Scenario: La barra no se recorta con barra de gestos del sistema

- **GIVEN** la aplicación abierta en un dispositivo cuya barra de navegación del sistema es una barra
  de gestos
- **WHEN** el usuario mira la barra de navegación inferior de la aplicación
- **THEN** los tres destinos, con su icono y su etiqueta de texto completos, se muestran por encima de
  la barra de gestos del sistema, sin ninguna parte oculta tras ella

#### Scenario: La barra no se recorta con barra de navegación de 3 botones

- **GIVEN** la aplicación abierta en un dispositivo cuya barra de navegación del sistema son 3 botones
- **WHEN** el usuario mira la barra de navegación inferior de la aplicación
- **THEN** los tres destinos, con su icono y su etiqueta de texto completos, se muestran por encima de
  esa barra de 3 botones, sin ninguna parte oculta tras ella

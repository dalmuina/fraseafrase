# app-navigation Specification

## Purpose

Define la barra de navegación inferior que permite moverse entre las pantallas principales de la
aplicación — práctica, progreso y ajustes — y qué pestaña se muestra al abrir la app.

## Requirements

### Requirement: Barra de navegación inferior con tres destinos

La aplicación MUST mostrar una barra de navegación inferior con tres destinos: práctica, progreso y
ajustes. Cada destino MUST mostrar un icono y una etiqueta de texto, y cuál está seleccionado.

#### Scenario: Los tres destinos están disponibles

- **GIVEN** la aplicación abierta
- **WHEN** el usuario mira la barra de navegación inferior
- **THEN** la barra ofrece los destinos práctica, progreso y ajustes, cada uno con su icono y su
  etiqueta de texto

### Requirement: Cambio de pestaña

Al seleccionar un destino de la barra de navegación inferior, la aplicación MUST mostrar esa
pantalla y MUST marcar ese destino como seleccionado.

#### Scenario: Seleccionar progreso desde la práctica

- **GIVEN** la aplicación muestra la pantalla de práctica
- **WHEN** el usuario selecciona el destino de progreso
- **THEN** la aplicación muestra la pantalla de progreso y la marca como seleccionada

#### Scenario: Seleccionar ajustes desde el progreso

- **GIVEN** la aplicación muestra la pantalla de progreso
- **WHEN** el usuario selecciona el destino de ajustes
- **THEN** la aplicación muestra la pantalla de ajustes y la marca como seleccionada

#### Scenario: Volver a la práctica desde cualquier destino

- **GIVEN** la aplicación muestra la pantalla de progreso o la de ajustes
- **WHEN** el usuario selecciona el destino de práctica
- **THEN** la aplicación muestra la pantalla de práctica y la marca como seleccionada

### Requirement: Pestaña de práctica como inicio

La aplicación MUST abrir con el destino de práctica seleccionado.

#### Scenario: Arranque muestra la práctica seleccionada

- **GIVEN** la aplicación se abre
- **WHEN** termina el arranque
- **THEN** la aplicación muestra la pantalla de práctica y la pestaña de práctica marcada como
  seleccionada

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

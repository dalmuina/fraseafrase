# ADR-0009: Alcance del MVP

- **Estado:** Aceptado
- **Fecha:** 2026-10-29

## Contexto
El método de estudio consiste en ver y repetir cada día entre 15 y 20 frases con contexto claro,
comprobar la traducción y volver a practicar las falladas. Hay que fijar qué entra en la primera versión
para evitar que la IA amplíe el alcance.

## Decisión

**Dentro del MVP:**

1. **Catálogo de frases** precargado, en `pt-PT` con traducción al español, clasificado por nivel (A1–C1) y tema.
2. **Sesión diaria** de N frases (por defecto 20, configurable entre 10 y 30), combinando frases pendientes de repaso y frases nuevas del nivel elegido.
3. **Tarjeta de frase:** se muestra la frase en español; el usuario la dice o piensa en portugués; revela la respuesta y se autoevalúa (acierto / fallo).
4. **Repetición espaciada:** las frases falladas vuelven antes; las acertadas se espacian.
5. **Audio (TTS):** escuchar la frase en portugués.
6. **Ajustes:** nivel, número de frases diarias.
7. **Progreso básico:** frases vistas hoy y racha de días.

**Fuera del MVP** (cada uno será un *change* de OpenSpec posterior):

- Corrección automática por voz (reconocimiento de voz y comparación). Es el **primer cambio tras el MVP**.
- Dirección inversa (portugués → español) y otros modos de práctica.
- Variante `pt-BR` u otros idiomas.
- Cuentas, sincronización en la nube, IA generativa, notificaciones, estadísticas avanzadas, monetización.

## Consecuencias
- **Positivas:** alcance acotado y verificable; cualquier propuesta fuera de esta lista se rechaza o se convierte en un *change* posterior explícito.
- **Negativas:** la experiencia del MVP depende de la autoevaluación honesta del usuario.

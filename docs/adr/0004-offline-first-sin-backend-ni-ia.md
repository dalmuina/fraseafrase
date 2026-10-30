# ADR-0004: Offline-first, sin backend ni IA en el MVP

- **Estado:** Aceptado
- **Fecha:** 2026-10-29

## Contexto
El método de estudio solo necesita frases con traducción, una selección diaria y repaso de las falladas.
Generar frases con IA en tiempo de ejecución exigiría un backend (para no exponer claves), costes por usuario
y conexión, y no garantiza la calidad lingüística.

## Decisión
- La app funciona **completamente sin conexión**.
- El catálogo de frases se distribuye **empaquetado en la app** (base de datos precargada, generada a partir de un fichero fuente versionado en el repo).
- El catálogo se elabora **fuera de la app** (corpus abiertos y/o generación asistida por IA) y se **revisa manualmente** antes de incluirse.
- El progreso del usuario se guarda en local con **Room**; los ajustes, con **DataStore**.
- No hay backend, cuentas de usuario, analítica remota ni llamadas a modelos de IA en el MVP.
- La selección diaria y el repaso se resuelven con un **algoritmo de repetición espaciada** determinista (ver especificación `spaced-repetition`).

## Consecuencias
- **Positivas:** coste operativo cero, privacidad total, sin dependencia de red, comportamiento determinista y testeable.
- **Negativas:** el catálogo solo crece con nuevas versiones de la app; el progreso no se sincroniza entre dispositivos.
- **Abierto a futuro:** sincronización en la nube o funciones con IA como cambios posteriores, cada uno con su propio ADR.

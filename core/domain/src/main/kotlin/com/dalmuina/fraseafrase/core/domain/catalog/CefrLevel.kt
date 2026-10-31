package com.dalmuina.fraseafrase.core.domain.catalog

/**
 * Nivel CEFR de una frase del catálogo y nivel elegido por el usuario para las frases nuevas (ADR-0012).
 * No se llama `Level` porque ese nombre ya designa el nivel de consolidación 0–5 de `spacedrepetition`.
 */
enum class CefrLevel {
    A1,
    A2,
    B1,
    B2,
    C1,
}

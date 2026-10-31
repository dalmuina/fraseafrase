package com.dalmuina.fraseafrase.core.domain.catalog

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CefrLevelTest {

    // Scenario: Niveles CEFR válidos (spec: Frases nuevas por nivel CEFR)
    @Test
    fun `given the CEFR level type when listing its values then they are A1 to C1 in order`() {
        // When
        val names = CefrLevel.entries.map { it.name }

        // Then
        names shouldBe listOf("A1", "A2", "B1", "B2", "C1")
    }
}

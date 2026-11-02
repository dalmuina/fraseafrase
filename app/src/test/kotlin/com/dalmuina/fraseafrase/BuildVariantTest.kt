package com.dalmuina.fraseafrase

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

// Escenarios de specs/build-variants (cambio add-debug-application-id): "Variante debug" e "Identificadores distintos".
// AGP ejecuta los tests unitarios solo contra la variante debug (testBuildType), por eso el test fija su identificador.
class BuildVariantTest {

    // Scenario: Variante debug (spec: build-variants) e Identificadores distintos (spec: build-variants)
    @Test
    fun `given the debug variant when reading application id then it has the debug suffix`() {
        // Given
        // Local comparison value only. The release identifier is verified by configuration in app/build.gradle.kts (onVariants), not by this test.
        val releaseApplicationId = "com.dalmuina.fraseafrase"

        // When
        val debugApplicationId = BuildConfig.APPLICATION_ID

        // Then
        debugApplicationId shouldBe "com.dalmuina.fraseafrase.debug"
        debugApplicationId shouldNotBe releaseApplicationId
    }
}

package com.dalmuina.fraseafrase.core.designsystem.tokens

import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Tests unitarios de [Dimensions], el token de altura de controles primarios que expone
 * `AppTheme.dimensions` (fix-redesign-ui-mismatches, design D8). No requiere Compose runtime:
 * comprueba directamente el valor por defecto de [Dimensions].
 */
class DimensionsTest {

    // Scenario: AppTheme.dimensions.primaryActionHeight existe y vale 56.dp (design D8)
    @Test
    fun `given default dimensions when reading primary action height then it is 56dp`() {
        Dimensions().primaryActionHeight shouldBe 56.dp
    }
}

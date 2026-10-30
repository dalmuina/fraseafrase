package com.dalmuina.fraseafrase.core.domain.time

import com.dalmuina.fraseafrase.core.testing.FixedClock
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

// Andamiaje sin escenario de spec (ver design.md, Estrategia de tests):
// el fake de reloj es la única fuente de fechas en los tests.
class FixedClockTest {

    @Test
    fun `given a fixed date when asking today then returns that date`() {
        // Given
        val clock = FixedClock(LocalDate.of(2026, 3, 10))

        // When
        val today = clock.today()

        // Then
        today shouldBe LocalDate.of(2026, 3, 10)
    }
}

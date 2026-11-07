package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ReviewScheduleTest {

    // Scenario: Enfriamiento de cada nivel (spec: Enfriamiento en sesiones tras certificar)
    @ParameterizedTest(name = "given level {0} when computing its cooldown then returns {1} sessions")
    @CsvSource(
        "1,1",
        "2,2",
        "3,4",
        "4,8",
        "5,16",
    )
    fun `given a level when computing its cooldown then returns the expected sessions`(
        level: Int,
        expectedSessions: Int,
    ) {
        // When
        val cooldown = ReviewSchedule.cooldownSessionsFor(Level(level))

        // Then
        cooldown shouldBe expectedSessions
    }
}

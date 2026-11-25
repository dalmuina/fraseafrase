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

    // Scenario: Espera intra-nivel de cada nivel (spec: Espera intra-nivel de cada nivel)
    @ParameterizedTest(name = "given level {0} when computing its within level wait then returns {1} sessions")
    @CsvSource(
        "0,1",
        "1,1",
        "2,1",
        "3,2",
        "4,4",
        "5,8",
    )
    fun `given levels 0 to 5 when computing the within level wait then it is 1 1 1 2 4 and 8`(
        level: Int,
        expectedSessions: Int,
    ) {
        // When
        val wait = ReviewSchedule.withinLevelWaitFor(Level(level))

        // Then
        wait shouldBe expectedSessions
    }
}

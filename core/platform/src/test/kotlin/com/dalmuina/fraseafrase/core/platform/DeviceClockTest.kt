package com.dalmuina.fraseafrase.core.platform

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DeviceClockTest {

    // Scenario: fecha local del dispositivo (ADR-0012: los intervalos se miden en días naturales de la zona local)
    @Test
    fun `given an instant just after local midnight in a zone when asking today then returns that local date`() {
        // Given: 23:30 UTC del 10-03 es ya el 11-03 en Madrid (UTC+1)
        val instant = Instant.parse("2026-03-10T23:30:00Z")
        val clock = DeviceClock(zone = ZoneId.of("Europe/Madrid"), now = { instant })

        // When
        val today = clock.today()

        // Then
        today shouldBe LocalDate.of(2026, 3, 11)
    }
}

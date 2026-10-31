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
        val clock = DeviceClock(zone = { ZoneId.of("Europe/Madrid") }, now = { instant })

        // When
        val today = clock.today()

        // Then
        today shouldBe LocalDate.of(2026, 3, 11)
    }

    // Scenario: el usuario cambia de zona horaria con la app abierta (design D8)
    @Test
    fun `given the zone changes between calls when asking today then the new zone local date is returned`() {
        // Given: 23:30 UTC del 10-03 es el 11-03 en Madrid y el 10-03 en Nueva York (UTC-4 en esa fecha)
        val instant = Instant.parse("2026-03-10T23:30:00Z")
        var zone = ZoneId.of("Europe/Madrid")
        val clock = DeviceClock(zone = { zone }, now = { instant })
        val before = clock.today()

        // When: el dispositivo pasa a la zona de Nueva York
        zone = ZoneId.of("America/New_York")
        val after = clock.today()

        // Then
        before shouldBe LocalDate.of(2026, 3, 11)
        after shouldBe LocalDate.of(2026, 3, 10)
    }
}

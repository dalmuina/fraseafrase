package com.dalmuina.fraseafrase.core.platform

import com.dalmuina.fraseafrase.core.domain.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Reloj del dispositivo (ADR-0010, ADR-0012): la fecha local de la zona del sistema, que decide los repasos en días
 * naturales. Se llama así para no colisionar con `android.os.SystemClock`. Hilt usa el constructor sin argumentos;
 * el constructor interno permite fijar la zona y el instante en los tests.
 */
class DeviceClock internal constructor(
    private val zone: ZoneId,
    private val now: () -> Instant,
) : Clock {

    @Inject
    constructor() : this(ZoneId.systemDefault(), Instant::now)

    override fun today(): LocalDate = now().atZone(zone).toLocalDate()
}

package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.time.Clock
import java.time.LocalDate

/**
 * Fake de [Clock] con una fecha fija e inmutable. Un test que necesite otra fecha crea otra instancia.
 */
class FixedClock(private val date: LocalDate) : Clock {

    override fun today(): LocalDate = date
}

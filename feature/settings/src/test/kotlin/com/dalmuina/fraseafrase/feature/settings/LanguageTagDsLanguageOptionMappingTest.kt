package com.dalmuina.fraseafrase.feature.settings

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LanguageTagDsLanguageOptionMappingTest {

    // Scenario: Elegir un idioma destino de la lista (spec: settings-screen) - nombre visible de cada
    // idioma destino soportado (add-multilingual-sources)
    @Test
    fun `given a supported language tag when mapping it to a ds language option then it shows its display name`() {
        // Given/When/Then: cada código soportado se mapea a su nombre nativo visible
        LanguageTag("pt-PT").toDsLanguageOption().displayName shouldBe "Português (Portugal)"
        LanguageTag("pt-BR").toDsLanguageOption().displayName shouldBe "Português (Brasil)"
        LanguageTag("es-ES").toDsLanguageOption().displayName shouldBe "Español (España)"
        LanguageTag("it-IT").toDsLanguageOption().displayName shouldBe "Italiano (Italia)"
        LanguageTag("en-GB").toDsLanguageOption().displayName shouldBe "English (UK)"
        LanguageTag("de-DE").toDsLanguageOption().displayName shouldBe "Deutsch (Deutschland)"
        LanguageTag("fr-FR").toDsLanguageOption().displayName shouldBe "Français (France)"
        LanguageTag("ru-RU").toDsLanguageOption().displayName shouldBe "Русский (Россия)"
    }

    // Scenario: Elegir un idioma destino de la lista (spec: settings-screen) - un código no reconocido
    // conserva su valor como nombre, para no perder la opción
    @Test
    fun `given an unrecognized language tag when mapping it then its value is used as the display name`() {
        // Given/When: un código de idioma no reconocido
        val option = LanguageTag("xx-XX").toDsLanguageOption()

        // Then: su nombre visible es el propio código
        option.displayName shouldBe "xx-XX"
        option.code shouldBe "xx-XX"
    }
}

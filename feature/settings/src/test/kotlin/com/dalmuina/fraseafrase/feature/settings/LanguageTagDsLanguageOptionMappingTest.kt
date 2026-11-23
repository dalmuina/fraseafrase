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
        LanguageTag("es-ES").toDsLanguageOption().displayName shouldBe "Español (España)"
        LanguageTag("it-IT").toDsLanguageOption().displayName shouldBe "Italiano (Italia)"
        LanguageTag("en-GB").toDsLanguageOption().displayName shouldBe "English (UK)"
        LanguageTag("de-DE").toDsLanguageOption().displayName shouldBe "Deutsch (Deutschland)"
        LanguageTag("fr-FR").toDsLanguageOption().displayName shouldBe "Français (France)"
        LanguageTag("ru-RU").toDsLanguageOption().displayName shouldBe "Русский (Россия)"
    }

    // Scenario: El catálogo empaquetado solo tiene portugués europeo (spec: phrase-catalog) - pt-BR ya no
    // tiene etiqueta propia y cae al comportamiento de código no reconocido
    @Test
    fun `given the retired pt-BR tag when mapping it then its value is used as the display name`() {
        // Given/When: el código pt-BR, retirado del catálogo
        val option = LanguageTag("pt-BR").toDsLanguageOption()

        // Then: ya no tiene nombre propio, se muestra su código
        option.displayName shouldBe "pt-BR"
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

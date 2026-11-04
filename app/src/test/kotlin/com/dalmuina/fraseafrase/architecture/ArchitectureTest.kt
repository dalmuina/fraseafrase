package com.dalmuina.fraseafrase.architecture

import com.lemonappdev.konsist.api.Konsist
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

// Scenarios: ADR-0010 ("una única declaración de preferencesDataStore por nombre de fichero" and
// "los data sources con estado tienen ámbito @Singleton"). skip_specs: true (design.md, change
// clean-remaining-review-debt): these rules guard architecture already documented in the ADR, not new
// product behaviour.
class ArchitectureTest {

    // Scenario: "una única declaración de preferencesDataStore por nombre de fichero" (ADR-0010)
    @Test
    fun `given the project when declaring preferencesDataStore then each name is used only once`() {
        // Given
        val preferencesDataStoreNames = Konsist
            .scopeFromProject()
            .properties()
            .mapNotNull { it.delegateName }
            .filter { it.startsWith("preferencesDataStore") }
            .mapNotNull { delegateName ->
                delegateName
                    .substringAfter("name", missingDelimiterValue = "")
                    .substringAfter("\"")
                    .substringBefore("\"")
                    .takeIf { it.isNotBlank() }
            }

        // When
        val duplicatedNames = preferencesDataStoreNames
            .groupBy { it }
            .filterValues { it.size > 1 }
            .keys

        // Then
        duplicatedNames.shouldBeEmpty()
    }

    // Scenario: "los data sources con estado tienen ámbito @Singleton" (ADR-0010)
    @Test
    fun `given data sources with in-memory state when checking their scope then they are annotated as Singleton`() {
        // Given: audited by hand (design.md, Decision 3) - data sources with a mutable field or in-memory cache of
        // their own. Constructor properties do not count.
        val statefulDataSourceNames = setOf("AssetsCatalogDataSource")

        // When
        val dataSourceClasses = Konsist
            .scopeFromProject()
            .classes()
            .filter { it.name.endsWith("DataSource") }

        // Then
        dataSourceClasses.forEach { dataSourceClass ->
            val isStateful = dataSourceClass.name in statefulDataSourceNames
            val hasSingletonAnnotation = dataSourceClass.hasAnnotationOf(javax.inject.Singleton::class)
            hasSingletonAnnotation shouldBe isStateful
        }
    }
}

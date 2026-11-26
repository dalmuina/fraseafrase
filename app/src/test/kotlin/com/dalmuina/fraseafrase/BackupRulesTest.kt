package com.dalmuina.fraseafrase

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

// Scenarios from specs/data-backup (change configure-backup-rules), design D4.
// Unit tests run with the module directory (app/) as working directory, so paths are relative to it.
private const val DATA_EXTRACTION_RULES = "src/main/res/xml/data_extraction_rules.xml"
private const val BACKUP_RULES = "src/main/res/xml/backup_rules.xml"
private const val MANIFEST = "src/main/AndroidManifest.xml"
private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

private data class Exclusion(val domain: String, val path: String)

private val expectedCatalogExclusions = listOf(
    Exclusion("database", "catalog.db"),
    Exclusion("database", "catalog.db-wal"),
    Exclusion("database", "catalog.db-shm"),
    Exclusion("database", "catalog.db-journal"),
).sortedBy { it.path }

private fun parseXml(path: String): Document {
    val file = File(path)
    withClue("falta $path") { file.exists() shouldBe true }
    return DocumentBuilderFactory.newInstance()
        .apply { isNamespaceAware = true }
        .newDocumentBuilder()
        .parse(file)
}

private fun Element.childElements(name: String): List<Element> =
    (0 until childNodes.length)
        .map { childNodes.item(it) }
        .filterIsInstance<Element>()
        .filter { it.localName == name }

private fun Element.excludes(): List<Exclusion> =
    childElements("exclude").map { Exclusion(it.getAttribute("domain"), it.getAttribute("path")) }

private fun cloudBackupExclusions(): List<Exclusion> =
    parseXml(DATA_EXTRACTION_RULES).documentElement.childElements("cloud-backup").flatMap { it.excludes() }

private fun deviceTransferExclusions(): List<Exclusion> =
    parseXml(DATA_EXTRACTION_RULES).documentElement.childElements("device-transfer").flatMap { it.excludes() }

private fun fullBackupExclusions(): List<Exclusion> =
    parseXml(BACKUP_RULES).documentElement.excludes()

private fun allExclusions(): List<Exclusion> =
    cloudBackupExclusions() + deviceTransferExclusions() + fullBackupExclusions()

class BackupRulesTest {

    // Scenario: "Copia en la nube en Android 12 o superior" (spec: data-backup)
    @Test
    fun `given the data extraction rules when reading cloud backup then catalog database files are excluded`() {
        // Given / When
        val exclusions = cloudBackupExclusions()

        // Then
        exclusions.sortedBy { it.path } shouldBe expectedCatalogExclusions
    }

    // Scenario: "Transferencia entre dispositivos en Android 12 o superior" (spec: data-backup)
    @Test
    fun `given the data extraction rules when reading device transfer then catalog database files are excluded`() {
        // Given / When
        val exclusions = deviceTransferExclusions()

        // Then
        exclusions.sortedBy { it.path } shouldBe expectedCatalogExclusions
    }

    // Scenario: "Copia completa en Android 8 a 11" (spec: data-backup)
    @Test
    fun `given the backup rules when reading full backup then catalog database files are excluded`() {
        // Given / When
        val exclusions = fullBackupExclusions()

        // Then
        exclusions.sortedBy { it.path } shouldBe expectedCatalogExclusions
    }

    // Scenario: "El manifiesto declara las reglas" (spec: data-backup)
    @Test
    fun `given the manifest when reading application then backup is allowed and both rule files are referenced`() {
        // Given
        val application = parseXml(MANIFEST).getElementsByTagName("application").item(0) as Element

        // When / Then
        application.getAttributeNS(ANDROID_NS, "allowBackup") shouldBe "true"
        application.getAttributeNS(ANDROID_NS, "dataExtractionRules") shouldBe "@xml/data_extraction_rules"
        application.getAttributeNS(ANDROID_NS, "fullBackupContent") shouldBe "@xml/backup_rules"
    }

    // Scenario: "Ninguna regla excluye el progreso" (spec: data-backup)
    @Test
    fun `given the backup rules when searching exclusions of practice database then there are none`() {
        // Given / When
        val progressExclusions = allExclusions().filter { it.path.startsWith("practice.db") }

        // Then
        progressExclusions shouldBe emptyList()
    }

    // Scenario: "Ninguna regla excluye los ajustes" (spec: data-backup)
    @Test
    fun `given the backup rules when searching exclusions of datastore then there are none`() {
        // Given / When
        val settingsExclusions = allExclusions().filter { it.path.startsWith("datastore") }

        // Then
        settingsExclusions shouldBe emptyList()
    }

    // Scenario: "Las reglas no restringen la copia a una lista de inclusión" (spec: data-backup)
    @Test
    fun `given the backup rules when reading them then none contains include rules`() {
        // Given / When
        val includeCount = listOf(DATA_EXTRACTION_RULES, BACKUP_RULES)
            .sumOf { parseXml(it).getElementsByTagName("include").length }

        // Then
        includeCount shouldBe 0
    }
}

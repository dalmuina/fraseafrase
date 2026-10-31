import com.dalmuina.fraseafrase.buildlogic.libs
import com.dalmuina.fraseafrase.buildlogic.library
import com.dalmuina.fraseafrase.buildlogic.pluginId
import androidx.room3.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Persistencia local (ADR-0013): Room 3 con KSP y SQLite empaquetado, serialización de JSON
 * para el catálogo de assets y DataStore para los ajustes. El plugin de serialization viene con
 * kotlin-gradle-plugin, así que se aplica por id desde aquí y no tiene versión propia.
 */
class PersistenceConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("ksp"))
            pluginManager.apply(libs.pluginId("room3"))
            pluginManager.apply(libs.pluginId("kotlin-serialization"))

            // Esquema exportado y versionado en el repositorio (design D5).
            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                add("implementation", libs.library("room3-runtime"))
                add("implementation", libs.library("androidx-sqlite-bundled"))
                add("implementation", libs.library("androidx-datastore-preferences"))
                add("implementation", libs.library("kotlinx-serialization-json"))
                add("ksp", libs.library("room3-compiler"))
                // Los tests JVM de Room (DAO y base de datos en memoria) necesitan su propio procesado de KSP.
                add("kspTest", libs.library("room3-compiler"))
                // Tests JVM locales: la variante Android de sqlite-bundled no encuentra sqliteJni fuera del
                // dispositivo; la variante JVM trae la librería nativa de Windows y Linux (ADR-0013).
                add("testRuntimeOnly", libs.library("androidx-sqlite-bundled-jvm"))
            }

            // Las configuraciones de test de AGP se crean por variante (p. ej. testDebugRuntimeClasspath).
            configurations.matching { it.name.startsWith("test") && it.name.endsWith("RuntimeClasspath") }
                .configureEach {
                    exclude(mapOf("group" to "androidx.sqlite", "module" to "sqlite-bundled-android"))
                }
        }
    }
}

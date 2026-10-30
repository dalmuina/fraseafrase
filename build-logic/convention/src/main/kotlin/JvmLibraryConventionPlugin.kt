import com.dalmuina.fraseafrase.buildlogic.configureKotlinJvm
import com.dalmuina.fraseafrase.buildlogic.configureQuality
import com.dalmuina.fraseafrase.buildlogic.libs
import com.dalmuina.fraseafrase.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Módulo Kotlin puro, sin dependencias de Android (ADR-0010). */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("kotlin-jvm"))
            configureKotlinJvm()
            configureQuality()
        }
    }
}

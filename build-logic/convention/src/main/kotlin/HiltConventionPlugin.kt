import com.dalmuina.fraseafrase.buildlogic.libs
import com.dalmuina.fraseafrase.buildlogic.library
import com.dalmuina.fraseafrase.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Hilt con KSP para módulos Android (ADR-0006). */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("ksp"))
            pluginManager.apply(libs.pluginId("hilt"))

            dependencies {
                add("implementation", libs.library("hilt-android"))
                add("ksp", libs.library("hilt-compiler"))
            }
        }
    }
}

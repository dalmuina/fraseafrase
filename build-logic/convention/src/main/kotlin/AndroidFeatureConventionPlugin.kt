import com.dalmuina.fraseafrase.buildlogic.libs
import com.dalmuina.fraseafrase.buildlogic.library
import com.dalmuina.fraseafrase.buildlogic.requireCoverage
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Módulo de feature (ADR-0010): librería Android con Compose y Hilt que solo
 * puede depender de :core:domain y :core:designsystem.
 *
 * Las dependencias de proyecto las añade este plugin, de modo que un módulo
 * de feature no necesita (ni debe) declarar dependencias hacia otros módulos.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("fraseafrase.android.library.compose")
            pluginManager.apply("fraseafrase.hilt")

            dependencies {
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:designsystem"))

                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))

                add("testImplementation", project(":core:testing"))
            }

            // ADR-0007: 80 % en la lógica de presentación (las funciones @Composable
            // y el código generado quedan excluidos por configureQuality()).
            requireCoverage(ruleName = "Presentación", minPercent = 80)
        }
    }
}

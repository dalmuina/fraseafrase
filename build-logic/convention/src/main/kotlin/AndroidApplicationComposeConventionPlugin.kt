import com.android.build.api.dsl.ApplicationExtension
import com.dalmuina.fraseafrase.buildlogic.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("fraseafrase.android.application")
            extensions.configure<ApplicationExtension> {
                configureAndroidCompose(this)
            }
        }
    }
}

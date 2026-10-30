import com.android.build.api.dsl.LibraryExtension
import com.dalmuina.fraseafrase.buildlogic.configureKotlinAndroid
import com.dalmuina.fraseafrase.buildlogic.configureQuality
import com.dalmuina.fraseafrase.buildlogic.libs
import com.dalmuina.fraseafrase.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("android-library"))

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
            }
            configureQuality()
        }
    }
}

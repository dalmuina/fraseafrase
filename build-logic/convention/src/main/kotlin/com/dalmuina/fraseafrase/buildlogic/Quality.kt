package com.dalmuina.fraseafrase.buildlogic

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * Análisis estático y cobertura comunes a todos los módulos (ADR-0007).
 * - detekt, con ktlint integrado para el formato.
 * - Kover, excluyendo código generado y funciones @Composable
 *   (la UI se verifica con tests de UI, no con cobertura de líneas).
 */
internal fun Project.configureQuality() {
    pluginManager.apply(libs.pluginId("detekt"))
    pluginManager.apply(libs.pluginId("kover"))

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        parallel = true
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    }

    // detekt 1.23 analiza el bytecode como JVM 17 (el proyecto compila a 17).
    tasks.withType<Detekt>().configureEach {
        jvmTarget = PROJECT_JAVA_VERSION.toString()
    }

    dependencies {
        add("detektPlugins", libs.library("detekt-formatting"))
    }

    extensions.configure<KoverProjectExtension> {
        reports {
            filters {
                excludes {
                    annotatedBy(*GENERATED_OR_UI_ANNOTATIONS)
                    classes(*GENERATED_CLASSES)
                    packages(*GENERATED_PACKAGES)
                }
            }
        }
    }
}

/**
 * Fija un mínimo de cobertura de líneas para el módulo actual.
 * Es pública para poder usarla desde los build.gradle.kts de los módulos.
 */
fun Project.requireCoverage(ruleName: String, minPercent: Int) {
    extensions.configure<KoverProjectExtension> {
        reports {
            verify {
                rule(ruleName) {
                    minBound(minPercent)
                }
            }
        }
    }
}

private val GENERATED_OR_UI_ANNOTATIONS = arrayOf(
    "androidx.compose.runtime.Composable",
    "androidx.compose.ui.tooling.preview.Preview",
    "dagger.Module",
    "dagger.internal.DaggerGenerated",
    "javax.annotation.processing.Generated",
)

private val GENERATED_CLASSES = arrayOf(
    "*_Factory",
    "*_Factory\$*",
    "*_MembersInjector",
    "*_HiltModules*",
    "Hilt_*",
    "*ComposableSingletons*",
    "*.BuildConfig",
    // Puntos de entrada de Android: solo enlazan el framework con la app.
    "*.MainActivity",
    "*.FraseAFraseApplication",
)

private val GENERATED_PACKAGES = arrayOf(
    "hilt_aggregated_deps",
    "dagger.hilt.internal.aggregatedroot.codegen",
)

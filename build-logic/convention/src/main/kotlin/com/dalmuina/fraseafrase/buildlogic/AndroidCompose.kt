package com.dalmuina.fraseafrase.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Activa Compose y añade las dependencias base de UI, previsualización y test (ADR-0006). */
internal fun Project.configureAndroidCompose(commonExtension: CommonExtension) {
    pluginManager.apply(libs.pluginId("kotlin-compose"))

    commonExtension.apply {
        buildFeatures.apply {
            compose = true
        }
    }

    dependencies {
        val bom = platform(libs.library("androidx-compose-bom"))
        add("implementation", bom)
        add("androidTestImplementation", bom)

        add("implementation", libs.library("androidx-compose-ui"))
        add("implementation", libs.library("androidx-compose-material3"))
        add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
        add("debugImplementation", libs.library("androidx-compose-ui-tooling"))

        add("androidTestImplementation", libs.library("androidx-compose-ui-test-junit4"))
        add("androidTestImplementation", libs.library("androidx-test-ext-junit"))
        add("androidTestImplementation", libs.library("androidx-test-runner"))
        add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
    }
}

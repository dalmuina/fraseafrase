package com.dalmuina.fraseafrase.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/** Nivel de bytecode de todo el proyecto (ADR-0008). */
internal val PROJECT_JAVA_VERSION = JavaVersion.VERSION_17

/**
 * Configuración común a módulos de aplicación y librería Android.
 *
 * Con AGP 9 Kotlin está integrado en el plugin de Android: no se aplica
 * `org.jetbrains.kotlin.android` y el jvmTarget de Kotlin se deriva de `compileOptions`.
 */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk {
            version = release(libs.version("compileSdk").toInt())
        }
        defaultConfig.apply {
            minSdk {
                version = release(libs.version("minSdk").toInt())
            }
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compileOptions.apply {
            sourceCompatibility = PROJECT_JAVA_VERSION
            targetCompatibility = PROJECT_JAVA_VERSION
        }
    }
    // Los tests unitarios locales de Android son tareas Test de Gradle: se ejecutan con JUnit Platform.
    tasks.withType<Test>().configureEach { useJUnitPlatform() }
    configureKotlinCompilerOptions()
    addUnitTestDependencies()
    addInstrumentationRunnerDependency()
}

/** Configuración común a módulos Kotlin puros (sin Android). */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = PROJECT_JAVA_VERSION
        targetCompatibility = PROJECT_JAVA_VERSION
    }
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.fromTarget(PROJECT_JAVA_VERSION.toString()))
    }
    tasks.withType<Test>().configureEach { useJUnitPlatform() }
    configureKotlinCompilerOptions()
    addUnitTestDependencies()
}

private fun Project.configureKotlinCompilerOptions() {
    val warningsAsErrors = providers.gradleProperty("warningsAsErrors").map(String::toBoolean).orElse(false)
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            allWarningsAsErrors.set(warningsAsErrors)
        }
    }
}

/**
 * El runner se fija en todos los módulos Android (`defaultConfig`), así que su librería
 * debe estar en todos ellos; si no, `connectedDebugAndroidTest` falla al arrancar aunque
 * el módulo no tenga tests instrumentados.
 */
private fun Project.addInstrumentationRunnerDependency() {
    dependencies {
        add("androidTestImplementation", libs.library("androidx-test-runner"))
    }
}

/**
 * Pila de tests unitarios común (ADR-0007). Gradle 9 exige declarar el launcher
 * de JUnit Platform de forma explícita.
 */
private fun Project.addUnitTestDependencies() {
    dependencies {
        add("testImplementation", platform(libs.library("junit-bom")))
        add("testImplementation", libs.library("junit-jupiter"))
        add("testImplementation", libs.library("kotest-assertions-core"))
        add("testImplementation", libs.library("mockk"))
        add("testImplementation", libs.library("turbine"))
        add("testImplementation", libs.library("kotlinx-coroutines-test"))
        add("testRuntimeOnly", libs.library("junit-platform-launcher"))
    }
}

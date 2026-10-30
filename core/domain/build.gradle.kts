import com.dalmuina.fraseafrase.buildlogic.requireCoverage

plugins {
    id("fraseafrase.jvm.library")
}

// ADR-0010: Kotlin puro. Ninguna dependencia de Android ni de otros módulos del proyecto.
// javax.inject (JSR-330) permite anotar casos de uso con @Inject sin acoplarse a Hilt.
dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)

    testImplementation(projects.core.testing)
}

// ADR-0007: 90 % de cobertura de líneas en el dominio.
requireCoverage(ruleName = "Dominio", minPercent = 90)

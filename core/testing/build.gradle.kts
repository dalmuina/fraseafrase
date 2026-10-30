plugins {
    id("fraseafrase.jvm.library")
}

// Utilidades de test compartidas: fakes de los puertos del dominio, builders de
// entidades y extensiones de JUnit. Solo se usa como testImplementation.
dependencies {
    api(projects.core.domain)

    api(platform(libs.junit.bom))
    api(libs.junit.jupiter)
    api(libs.kotest.assertions.core)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}

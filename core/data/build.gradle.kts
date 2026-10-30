plugins {
    id("fraseafrase.android.library")
    id("fraseafrase.hilt")
}

android {
    namespace = "com.dalmuina.fraseafrase.core.data"
}

// ADR-0010: data sources y repositorios que implementan los puertos de :core:domain.
// Las librerías de persistencia (Room, DataStore) se añaden en el cambio de OpenSpec
// que introduzca el catálogo y el progreso.
dependencies {
    implementation(projects.core.domain)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(projects.core.testing)
}

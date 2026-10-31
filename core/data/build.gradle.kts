plugins {
    id("fraseafrase.android.library")
    id("fraseafrase.hilt")
    id("fraseafrase.persistence")
}

android {
    namespace = "com.dalmuina.fraseafrase.core.data"
}

// ADR-0010: data sources y repositorios que implementan los puertos de :core:domain.
// Room 3, DataStore, SQLite empaquetado y serialización se aplican en fraseafrase.persistence (ADR-0013).
dependencies {
    implementation(projects.core.domain)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(projects.core.testing)
}

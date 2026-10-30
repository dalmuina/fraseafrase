plugins {
    id("fraseafrase.android.library")
    id("fraseafrase.hilt")
}

android {
    namespace = "com.dalmuina.fraseafrase.core.platform"
}

// ADR-0010: adaptadores de Android (TTS, reconocimiento de voz, reloj) que
// implementan los puertos de :core:domain.
dependencies {
    implementation(projects.core.domain)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(projects.core.testing)
}

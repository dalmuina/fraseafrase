plugins {
    id("fraseafrase.android.feature")
}

android {
    namespace = "com.dalmuina.fraseafrase.feature.session"
}

dependencies {
    // Espresso 3.7.0 en lugar del 3.5.0 del BOM de Compose, que falla en Android 17 (design.md D10).
    androidTestImplementation(libs.androidx.test.espresso.core)
}

// Las dependencias de proyecto (:core:domain, :core:designsystem) las añade
// el convention plugin. Una feature no depende de otras features ni de :core:data.

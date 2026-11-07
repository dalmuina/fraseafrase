plugins {
    id("fraseafrase.android.feature")
}

android {
    namespace = "com.dalmuina.fraseafrase.feature.settings"
}

dependencies {
    // Espresso 3.7.0 en lugar del 3.5.0 del BOM de Compose, que falla en Android 17 (design.md D10
    // de add-practice-batch-indicator; mismo fix ya aplicado en :feature:session, :feature:progress
    // y :core:designsystem).
    androidTestImplementation(libs.androidx.test.espresso.core)
}

// Las dependencias de proyecto (:core:domain, :core:designsystem) las añade
// el convention plugin. Una feature no depende de otras features ni de :core:data.

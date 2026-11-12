plugins {
    id("fraseafrase.android.feature")
}

android {
    namespace = "com.dalmuina.fraseafrase.feature.session"
}

dependencies {
    // Específica de este módulo (design D6 de fix-redesign-ui-mismatches): icono Undo para el botón de
    // deshacer, que material-icons-core (añadido globalmente) no incluye.
    implementation(libs.androidx.compose.material.icons.extended)

    // Espresso 3.7.0 en lugar del 3.5.0 del BOM de Compose, que falla en Android 17 (design.md D10).
    androidTestImplementation(libs.androidx.test.espresso.core)
}

// Las dependencias de proyecto (:core:domain, :core:designsystem) las añade
// el convention plugin. Una feature no depende de otras features ni de :core:data.

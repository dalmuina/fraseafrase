plugins {
    id("fraseafrase.android.library.compose")
}

android {
    namespace = "com.dalmuina.fraseafrase.core.designsystem"
}

dependencies {
    // Específica de este módulo (design D2): iconos military_tech, workspace_premium y diamond
    // que material-icons-core (añadido globalmente) no incluye.
    implementation(libs.androidx.compose.material.icons.extended)

    // Espresso 3.7.0 en lugar del 3.5.0 del BOM de Compose, que falla en Android 17
    // (mismo ajuste que :feature:session).
    androidTestImplementation(libs.androidx.test.espresso.core)
}

// ADR-0010: el design system no depende de ningún otro módulo del proyecto.

plugins {
    id("fraseafrase.android.library.compose")
}

android {
    namespace = "com.dalmuina.fraseafrase.core.designsystem"
}

// ADR-0010: el design system no depende de ningún otro módulo del proyecto.

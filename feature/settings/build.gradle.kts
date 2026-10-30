plugins {
    id("fraseafrase.android.feature")
}

android {
    namespace = "com.dalmuina.fraseafrase.feature.settings"
}

// Las dependencias de proyecto (:core:domain, :core:designsystem) las añade
// el convention plugin. Una feature no depende de otras features ni de :core:data.

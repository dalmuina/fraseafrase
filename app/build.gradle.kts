plugins {
    id("fraseafrase.android.application.compose")
    id("fraseafrase.hilt")
}

android {
    namespace = "com.dalmuina.fraseafrase"

    defaultConfig {
        applicationId = "com.dalmuina.fraseafrase"
        versionCode = 1
        versionName = "0.1.0"
        // Hilt exige un runner que devuelva HiltTestApplication (design.md D2). Solo :app lo sobrescribe.
        testInstrumentationRunner = "com.dalmuina.fraseafrase.HiltTestRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

// :app es el único módulo que conoce todas las implementaciones (ADR-0010).
dependencies {
    implementation(projects.feature.session)
    implementation(projects.feature.settings)
    implementation(projects.core.designsystem)
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.platform)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    // El proveedor de la base de datos (PersistenceModule) expone su tipo de Room a Hilt.
    implementation(libs.room3.runtime)

    // Tests de cableado de Hilt (design.md D1 a D3): grafo de producción real, sin módulos de prueba.
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}

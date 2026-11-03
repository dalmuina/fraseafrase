plugins {
    id("fraseafrase.android.application.compose")
    id("fraseafrase.hilt")
}

// Identificador de la build de publicación (design.md D1 y D5). Es val y no const val: forma segura en un script .gradle.kts.
val PRODUCTION_APP_ID = "com.dalmuina.fraseafrase"

android {
    namespace = "com.dalmuina.fraseafrase"

    defaultConfig {
        applicationId = PRODUCTION_APP_ID
        versionCode = 1
        versionName = "0.1.0"
        // Hilt exige un runner que devuelva HiltTestApplication (design.md D2). Solo :app lo sobrescribe.
        testInstrumentationRunner = "com.dalmuina.fraseafrase.HiltTestRunner"
    }

    buildFeatures {
        // Expone BuildConfig.APPLICATION_ID a los tests JVM (BuildVariantTest, design.md D4).
        buildConfig = true
        // Habilita resValue(...) para el app_name por variante (design.md - Decisions).
        resValues = true
    }

    buildTypes {
        debug {
            // Identificador propio para instalar junto a la build de publicación (design.md D1).
            applicationIdSuffix = ".debug"
            // Nombre visible distinto al de release, para distinguir ambas variantes instaladas (design.md - Decisions).
            resValue("string", "app_name", "Frase a Frase (Debug)")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Nombre de publicación (design.md - Decisions).
            resValue("string", "app_name", "Frase a Frase")
        }
    }
}

// Comprobación de configuración de la variante release (design.md D5): su identificador no cambia por este cambio.
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        val actual = variant.applicationId.get()
        if (actual != PRODUCTION_APP_ID) {
            throw GradleException("applicationId de release: esperado '$PRODUCTION_APP_ID', obtenido '$actual'")
        }
    }

    // Comprobación de configuración del nombre visible por variante (design.md - Decisions).
    // El MapProperty de resValues no se puede leer con .get() dentro de onVariants (se lanza
    // "configuration of project has not completed yet"): se difiere a cuando el grafo de tareas
    // está listo, momento en el que la configuración de todos los proyectos ya ha terminado.
    onVariants { variant ->
        project.gradle.taskGraph.whenReady {
            val expected = if (variant.buildType == "debug") "Frase a Frase (Debug)" else "Frase a Frase"
            val actual = variant.resValues.get()[variant.makeResValueKey("string", "app_name")]?.value
            if (actual != expected) {
                throw GradleException(
                    "app_name de la variante '${variant.name}': esperado '$expected', obtenido '$actual'",
                )
            }
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
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    // El proveedor de la base de datos (PersistenceModule) expone su tipo de Room a Hilt.
    implementation(libs.room3.runtime)

    // Tests de cableado de Hilt (design.md D1 a D3): grafo de producción real, sin módulos de prueba.
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    // Espresso 3.7.0 en lugar del 3.5.0 del BOM de Compose, que falla en Android 17 (ver
    // feature/session/build.gradle.kts design.md D10).
    androidTestImplementation(libs.androidx.test.espresso.core)
}

import java.util.Properties

plugins {
    id("fraseafrase.android.application.compose")
    id("fraseafrase.hilt")
}

// Identificador de la build de publicación (design.md D1 y D5). Es val y no const val: forma segura en un script .gradle.kts.
val PRODUCTION_APP_ID = "com.dalmuina.fraseafrase"

// Credenciales de firma de `release` (design.md - Decisions). Lectura perezosa: el fichero local
// `keystore.properties` (no versionado) es la vía principal; las variables de entorno son la
// alternativa para un futuro CI. No se exige nada aquí: solo se comprueba más abajo, al configurar
// una tarea de la variante release, para no afectar a `debug` ni a `check`.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile.exists()) {
            keystorePropertiesFile.inputStream().use { load(it) }
        }
    }

fun releaseSigningCredential(propertyKey: String, environmentVariable: String): String? =
    (keystoreProperties.getProperty(propertyKey) ?: System.getenv(environmentVariable))?.takeIf { it.isNotBlank() }

val releaseStoreFile = releaseSigningCredential("storeFile", "RELEASE_KEYSTORE_PATH")
val releaseStorePassword = releaseSigningCredential("storePassword", "RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias = releaseSigningCredential("keyAlias", "RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseSigningCredential("keyPassword", "RELEASE_KEY_PASSWORD")

val hasReleaseSigningCredentials =
    releaseStoreFile != null &&
        releaseStorePassword != null &&
        releaseKeyAlias != null &&
        releaseKeyPassword != null

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

    signingConfigs {
        // Solo se declara cuando las cuatro credenciales están disponibles (design.md - Decisions);
        // si faltan, release se queda sin signingConfig propio y la comprobación de más abajo falla
        // la build al configurar una tarea de esa variante, en vez de firmar con la clave de depuración.
        if (hasReleaseSigningCredentials) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
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
            if (hasReleaseSigningCredentials) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

// Nombres de las tareas de empaquetado/firma de la variante release (AGP): son las únicas que
// necesitan `signingConfigs.release`. Otras tareas de release (compilación, lint, kover) también
// contienen "Release" en el nombre pero no firman nada, y `check` las incluye en su grafo; si se
// comprobaran todas, `./gradlew check` fallaría sin motivo (tasks.md 4.1).
val releasePackagingTaskNames = setOf("assembleRelease", "bundleRelease", "packageRelease")

// Falla pronto si se pide una tarea de empaquetado de la variante release sin credenciales de
// firma, en vez de dejar que AGP la firme en silencio con la clave de depuración (design.md -
// Decisions). Perezoso: se comprueba solo cuando el grafo de tareas incluye una de esas tareas de
// este módulo, así que `./gradlew :app:assembleDebug` y `./gradlew check` no se ven afectados
// (tasks.md 2.5, 4.1).
gradle.taskGraph.whenReady {
    val releaseTaskRequested =
        allTasks.any { it.path.startsWith(project.path) && it.name in releasePackagingTaskNames }
    if (releaseTaskRequested && !hasReleaseSigningCredentials) {
        throw GradleException(
            "Faltan credenciales de firma para la variante release. Copia " +
                "'keystore.properties.example' a 'keystore.properties' en la raíz del proyecto y " +
                "rellena storeFile, storePassword, keyAlias y keyPassword, o define las variables " +
                "de entorno RELEASE_KEYSTORE_PATH, RELEASE_KEYSTORE_PASSWORD, RELEASE_KEY_ALIAS y " +
                "RELEASE_KEY_PASSWORD.",
        )
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
    implementation(projects.feature.progress)
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

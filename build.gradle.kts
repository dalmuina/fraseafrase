// Los plugins se declaran aquí con `apply false` para cargarlos una sola vez en el classpath
// de la build. Cada módulo los aplica a través de los convention plugins de build-logic.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover)
}

// Informe de cobertura agregado de los módulos con lógica (ADR-0007: mínimo global del 70 %).
// :app (enlace con el framework) y :core:designsystem (verificado con tests de UI) no se
// incluyen: su código es visual o de integración y distorsionaría la métrica.
// Ejecutar con `./gradlew :koverHtmlReport` o `./gradlew :koverVerify`.
dependencies {
    kover(projects.feature.session)
    kover(projects.feature.settings)
    kover(projects.core.data)
    kover(projects.core.platform)
    kover(projects.core.domain)
}

kover {
    reports {
        filters {
            excludes {
                annotatedBy(
                    "androidx.compose.runtime.Composable",
                    "androidx.compose.ui.tooling.preview.Preview",
                    "dagger.Module",
                    "dagger.internal.DaggerGenerated",
                    "javax.annotation.processing.Generated",
                )
                classes(
                    "*_Factory",
                    "*_Factory\$*",
                    "*_MembersInjector",
                    "*_HiltModules*",
                    "Hilt_*",
                    "*ComposableSingletons*",
                    "*.BuildConfig",
                )
                packages("hilt_aggregated_deps", "dagger.hilt.internal.aggregatedroot.codegen")
            }
        }
        verify {
            rule("Cobertura global") {
                minBound(70)
            }
        }
    }
}

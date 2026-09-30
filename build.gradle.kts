// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.spotless) apply false
    // Milestone static-analysis scan (roadmap 2.10/3.8/4.19; owner decision 2026-09-28).
    // Applied at the root (the `sonar` task analyses the whole multi-module build as one
    // project) and per subproject (so each module can declare its own coverage report path).
    // Host URL and token are passed on the command line, never in this file.
    alias(libs.plugins.sonar) apply false
    alias(libs.plugins.kover) apply false
}

apply(plugin = "org.sonarqube")

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "com.diffplug.spotless")
}

// The root project is not a subproject, so the block above never reaches it. 2.0.5 gates the
// two root Kotlin scripts (`build.gradle.kts`, `settings.gradle.kts`) as well; detekt and
// spotless stay subproject-only.
apply(plugin = "org.jlleitschuh.gradle.ktlint")

// ============================================================================
// Unit-test coverage for the SonarQube milestone scans (roadmap 2.6).
// Kover instruments the classes that sit on the test runtime classpath, so the classes
// Robolectric's sandbox classloader defines are measured too — the exact blind spot that
// the JaCoCo wiring could not see (core-ui, core-database, the feature screens).
// Kover emits a JaCoCo-format XML, so the scanner property name is unchanged.
// The coverage gate itself is 4.8.
// ============================================================================

/** Kover's XML report for a module, at its default location. */
fun Project.koverXmlFile(): File = layout.buildDirectory.file("reports/kover/report.xml").get().asFile

/** Generated code and tests never deserve coverage — same intent as the old JaCoCo excludes. */
fun Project.configureKoverFilters() {
    extensions.configure<kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension> {
        reports {
            filters {
                excludes {
                    classes(
                        "*.R",
                        "*.R\$*",
                        "*BuildConfig",
                        "*BuildConfig.*",
                        "*_Factory",
                        "*_Factory*",
                        "*_MembersInjector",
                        "*_MembersInjector*",
                        "*Dagger*",
                        "*hilt_aggregated_deps*",
                        "*Manifest*",
                        "*Test",
                        "*Test\$*",
                    )
                }
            }
        }
    }
}

/** Every module with unit tests declares its own Kover XML on its own Sonar extension. */
fun Project.pointSonarAtKoverReport() {
    extensions.configure<org.sonarqube.gradle.SonarExtension> {
        properties {
            property("sonar.coverage.jacoco.xmlReportPaths", koverXmlFile())
        }
    }
}

subprojects {
    plugins.withId("com.android.application") {
        // The app module has no unit tests; its coverage would come from instrumented runs,
        // which AGP 9.4 cannot report (documented in 2.6's validation.md).
    }
    plugins.withId("com.android.library") {
        apply(plugin = "org.jetbrains.kotlinx.kover")
        apply(plugin = "org.sonarqube")
        configureKoverFilters()
        pointSonarAtKoverReport()
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        apply(plugin = "org.jetbrains.kotlinx.kover")
        apply(plugin = "org.sonarqube")
        configureKoverFilters()
        pointSonarAtKoverReport()
    }
}

gradle.projectsEvaluated {
    tasks.named("sonar") {
        // Kover's report tasks carry no test dependencies on AGP 9 (kotlinx-kover #785), so the
        // scan drives tests explicitly, then the reports.
        val unitTestTasks =
            allprojects.mapNotNull { project ->
                project.tasks.findByName("testDebugUnitTest") ?: project.tasks.findByName("test")
            }
        val reportTasks = allprojects.mapNotNull { it.tasks.findByName("koverXmlReport") }
        dependsOn(unitTestTasks + reportTasks)
    }
}

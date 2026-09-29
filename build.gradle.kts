// Top-level build file where you can add configuration options common to all sub-projects/modules.
import com.android.build.api.dsl.LibraryExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

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
// Unit-test coverage for the SonarQube milestone scans (roadmap 2.6, 2026-09-28).
// The coverage gate itself is 4.8; this wires the measurement. JaCoCo ships with the
// Android Gradle Plugin, so no new dependency enters the stack (tech-stack section 6).
// ============================================================================

// Files that never deserve coverage: generated R/BuildConfig, DI factories, tests.
val coverageExcludes =
    listOf(
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "**/*_Factory*.*",
        "**/*_MembersInjector*.*",
        "**/di/**",
        "**/Dagger*.*",
        "**/hilt_aggregated_deps/**",
    )

subprojects {
    plugins.withId("com.android.library") {
        apply(plugin = "jacoco")
        // The scanner only auto-integrates JaCoCo for JVM projects; Android modules must
        // declare their own report path on their own Sonar extension.
        apply(plugin = "org.sonarqube")
        extensions.configure<org.sonarqube.gradle.SonarExtension> {
            properties {
                property(
                    "sonar.coverage.jacoco.xmlReportPaths",
                    layout.buildDirectory.file("reports/jacoco/jacocoTestReport/jacocoTestReport.xml").get().asFile,
                )
            }
        }
        extensions.configure<LibraryExtension> {
            testCoverage {
                jacocoVersion = "0.8.13"
            }
        }
        tasks.register<JacocoReport>("jacocoTestReport") {
            dependsOn("testDebugUnitTest")
            reports {
                xml.required.set(true)
                html.required.set(false)
            }
            val buildDirFile = layout.buildDirectory.get().asFile
            val kotlinClasses =
                fileTree("$buildDirFile/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes") {
                    exclude(coverageExcludes)
                }
            // Robolectric-based tests record the exec against the classes the test runtime
            // actually loads (the ASM-transformed and runtime-library copies), so the report
            // analyzes those too; duplicate names resolve to the first match. The runtime
            // copies go first because the offline instrumenter rewrites them in place.
            val asmTransformedClasses =
                fileTree("$buildDirFile/intermediates/classes/debug/transformDebugClassesWithAsm/dirs") {
                    exclude(coverageExcludes)
                }
            val runtimeLibraryClasses =
                fileTree("$buildDirFile/intermediates/runtime_library_classes_dir/debug/bundleLibRuntimeToDirDebug") {
                    exclude(coverageExcludes)
                }
            classDirectories.setFrom(kotlinClasses, asmTransformedClasses)
            sourceDirectories.setFrom(files(listOf("src/main/java", "src/main/kotlin")))
            executionData.setFrom(files("$buildDirFile/jacoco/testDebugUnitTest.exec"))
        }
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        apply(plugin = "jacoco")
        tasks.named<JacocoReport>("jacocoTestReport") {
            dependsOn("test")
            reports {
                xml.required.set(true)
                xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/jacocoTestReport/jacocoTestReport.xml"))
                html.required.set(false)
            }
            val buildDirFile = layout.buildDirectory.get().asFile
            val kotlinClasses = fileTree("$buildDirFile/classes/kotlin/main") { exclude(coverageExcludes) }
            classDirectories.setFrom(kotlinClasses)
            sourceDirectories.setFrom(files(listOf("src/main/java", "src/main/kotlin")))
            executionData.setFrom(files("$buildDirFile/jacoco/test.exec"))
        }
    }
}

gradle.projectsEvaluated {
    tasks.named("sonar") {
        dependsOn(allprojects.mapNotNull { it.tasks.findByName("jacocoTestReport") })
    }
}

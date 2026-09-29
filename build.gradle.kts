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
    plugins.withId("com.android.application") {
        apply(plugin = "jacoco")
        apply(plugin = "org.sonarqube")
        extensions.configure<org.sonarqube.gradle.SonarExtension> {
            properties {
                property(
                    "sonar.coverage.jacoco.xmlReportPaths",
                    layout.buildDirectory.file("reports/jacoco/jacocoAndroidTestReport/jacocoAndroidTestReport.xml").get().asFile,
                )
            }
        }
        extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
            testCoverage {
                jacocoVersion = "0.8.13"
            }
        }
        // Roadmap 2.6 coverage: the instrumented (device) run covers the app and every library
        // module's classes it loads, so this report analyzes the whole build's class trees and
        // merges with the per-module unit reports server-side.
        tasks.register<JacocoReport>("jacocoAndroidTestReport") {
            dependsOn("connectedDebugAndroidTest")
            reports {
                xml.required.set(true)
                xml.outputLocation.set(
                    layout.buildDirectory.file("reports/jacoco/jacocoAndroidTestReport/jacocoAndroidTestReport.xml"),
                )
                html.required.set(false)
            }
            val buildDirFile = layout.buildDirectory.get().asFile
            val classTrees = mutableListOf(
                fileTree("$buildDirFile/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes") {
                    exclude(coverageExcludes)
                },
            )
            val sourceRoots = mutableListOf<java.io.File>()
            rootProject.subprojects.forEach { lib ->
                if (lib.path == path) return@forEach
                val classesDir =
                    java.io.File(lib.layout.buildDirectory.get().asFile, "intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")
                if (classesDir.exists()) {
                    classTrees += fileTree(classesDir) { exclude(coverageExcludes) }
                }
                listOf("src/main/java", "src/main/kotlin").forEach { src ->
                    val dir = java.io.File(lib.projectDir, src)
                    if (dir.exists()) sourceRoots += dir
                }
            }
            classDirectories.setFrom(classTrees)
            sourceDirectories.setFrom(
                files(
                    listOf(
                        java.io.File(projectDir, "src/main/java"),
                        java.io.File(projectDir, "src/main/kotlin"),
                    ) + sourceRoots,
                ),
            )
            executionData.setFrom(fileTree("$buildDirFile/outputs") { include("**/*.ec") })
        }
    }
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
        // Only the JVM unit reports run as part of a scan; the device report
        // (jacocoAndroidTestReport) is run explicitly beforehand when the device is available,
        // because it wipes the on-device wallet via the E2E test's documented precondition.
        dependsOn(allprojects.mapNotNull { it.tasks.findByName("jacocoTestReport") })
    }
}

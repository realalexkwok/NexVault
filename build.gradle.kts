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
    // Applied at the root on purpose: the `sonar` task analyses the whole multi-module build
    // as one project. Host URL and token are passed on the command line, never in this file.
    alias(libs.plugins.sonar)
}

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
            classDirectories.setFrom(kotlinClasses)
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
    val reportPaths =
        allprojects.mapNotNull { project ->
            project.tasks.findByName("jacocoTestReport")?.let {
                "${project.layout.buildDirectory.get().asFile}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
            }
        }
    // The scanner reads `sonar.*` gradle project properties; extra properties are the way to feed
    // them without the plugin's extension object.
    extensions.extraProperties["sonar.coverage.jacoco.xmlReportPaths"] = reportPaths.joinToString(",")
    tasks.named("sonar") {
        dependsOn(allprojects.mapNotNull { it.tasks.findByName("jacocoTestReport") })
    }
}

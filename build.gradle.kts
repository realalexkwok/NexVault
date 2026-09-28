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

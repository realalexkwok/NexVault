import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.compose.compiler)
}

//configurations.configureEach {
//    resolutionStrategy.eachDependency {
//        if (requested.group == "io.netty") {
//            useVersion("4.1.115.Final")
//            because("Force all Netty modules to a single patched version")
//        }
//    }
//}

// API keys live in local.properties (git-ignored). Gradle project properties do NOT include that
// file, so it is loaded explicitly here; a -P property (CI) still overrides it. Values are never
// printed — only injected into BuildConfig.
val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}

fun apiKey(name: String): String {
    val fromFile: String? = localProperties.getProperty(name)
    if (!fromFile.isNullOrBlank()) return fromFile
    val fromProject = project.findProperty(name)
    return if (fromProject is String && fromProject.isNotBlank()) fromProject else ""
}

android {
    namespace = "com.nexvault.wallet"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "com.nexvault.wallet"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // BuildConfig fields for API keys
        buildConfigField("String", "INFURA_API_KEY", "\"${apiKey("INFURA_API_KEY")}\"")
        buildConfigField("String", "ALCHEMY_API_KEY", "\"${apiKey("ALCHEMY_API_KEY")}\"")
        buildConfigField("String", "COINGECKO_API_KEY", "\"${apiKey("COINGECKO_API_KEY")}\"")
        buildConfigField("String", "ETHERSCAN_API_KEY", "\"${apiKey("ETHERSCAN_API_KEY")}\"")
        buildConfigField("String", "WALLETCONNECT_PROJECT_ID", "\"${apiKey("WALLETCONNECT_PROJECT_ID")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/FastDoubleParser-LICENSE"
            excludes += "META-INF/FastDoubleParser-NOTICE"
            excludes += "/META-INF/versions/9/io/netty/**"
            excludes += "META-INF/io.netty.versions.properties"
            // web3j 6 uses Jackson 3 (tools.jackson.*) while its tuweni -> vertx-core
            // chain still pulls Jackson 2 (com.fasterxml.jackson.*). Both jars carry a
            // META-INF/thirdparty-LICENSE, which collides at packaging time. The two
            // Jackson lines live in different packages and coexist at runtime; only the
            // duplicate license file needs dropping.
            excludes += "META-INF/thirdparty-LICENSE"
        }
    }
}

dependencies {
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.material)

    // Navigation
    implementation(libs.bundles.compose.navigation)

    // Core Modules
    implementation(project(":core:core-common"))
    implementation(project(":core:core-ui"))
    implementation(project(":core:core-network"))
    implementation(project(":core:core-database"))
    implementation(project(":core:core-datastore"))
    implementation(project(":core:core-security"))

    // Domain & Data
    implementation(project(":domain"))
    implementation(project(":data"))

    // Feature Modules
    implementation(project(":feature:feature-onboarding"))
    implementation(project(":feature:feature-home"))
    implementation(project(":feature:feature-tokens"))
    implementation(project(":feature:feature-send"))
    implementation(project(":feature:feature-receive"))
    implementation(project(":feature:feature-history"))
    implementation(project(":feature:feature-dapp"))
    implementation(project(":feature:feature-nft"))
    implementation(project(":feature:feature-swap"))
    implementation(project(":feature:feature-settings"))
    implementation(project(":feature:feature-auth"))

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.compose.testing)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
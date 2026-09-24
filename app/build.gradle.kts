plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// Every commit bumps versionCode so each install on the phone is an upgrade.
val commitCount: Int = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim().toIntOrNull() ?: 1 }.getOrElse(1)

// Release signing: credentials live in ~/.gradle/gradle.properties (never in the repo).
val releaseStoreFile = providers.gradleProperty("EMBER_STORE_FILE").orNull
val hasReleaseKey = releaseStoreFile != null && file(releaseStoreFile).exists()

android {
    namespace = "com.nhowe.ember"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.nhowe.ember"
        minSdk = 31
        targetSdk = 37
        versionCode = commitCount
        versionName = "0.1.$commitCount"
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = providers.gradleProperty("EMBER_STORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("EMBER_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("EMBER_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-dev"
        }
        release {
            isMinifyEnabled = false
            signingConfig = if (hasReleaseKey) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)

    implementation(libs.room3.runtime)
    ksp(libs.room3.compiler)
    implementation(libs.sqlite.bundled)
    implementation(libs.datastore.preferences)

    implementation(libs.coroutines.android)
    implementation(libs.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.coroutines.test)
}

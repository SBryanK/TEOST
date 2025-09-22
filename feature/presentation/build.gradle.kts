plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.teost.feature.presentation"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    buildFeatures { compose = true }

    sourceSets {
        getByName("main") {
            java.srcDir("screens")
            java.srcDir("navigation")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

// Workaround Windows KSP path flakiness for CI/local builds
ksp {
    arg("ksp.incremental", "false")
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)
    implementation(libs.gson)
    implementation(libs.work.runtime.ktx)
    implementation(libs.appcompat)
}



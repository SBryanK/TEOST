plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.teost"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.teost"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "BASE_URL", "\"https://api.edgeone.tencent.com/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Use Compose plugin managed compiler extension (remove hardcoded version)
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":feature:main"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:search"))
    implementation(project(":feature:presentation"))
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.splashscreen)
    
    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    
    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    
    // Material Components for XML themes
    implementation(libs.material.components)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
    
    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.12.0")
    
    // Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)
    
    // DataStore
    implementation(libs.datastore.preferences)
    
    // Image Loading
    implementation(libs.coil.compose)
    
    // Paging
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    // Firebase App Check
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    
    // Accompanist (remove deprecated SystemUiController)
    // implementation(libs.accompanist.permissions) // Uncomment if still needed
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // WorkManager
    implementation(libs.work.runtime.ktx)
    
    // Testing
    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.squareup.okhttp3:okhttp-tls:4.12.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
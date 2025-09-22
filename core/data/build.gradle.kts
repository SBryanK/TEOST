plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.teost.core.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Workaround Windows path issues with KSP incremental I/O on some environments
ksp {
    arg("ksp.incremental", "false")
    arg("room.incremental", "false")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(project(":core:domain"))

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.room.paging)

    // DataStore
    implementation(libs.datastore.preferences)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Gson for Room converters
    implementation("com.google.code.gson:gson:2.10.1")

    // Paging API for DAO return types
    implementation(libs.paging.runtime)

    // Hilt and Firebase for repositories
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // OkHttp for ConnectionTestRepository
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Retrofit for HttpTestService
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // Coroutines Task await (Firebase)
    implementation(libs.kotlinx.coroutines.play.services)
}



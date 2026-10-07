plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// OneDrive may lock generated files while syncing; build outputs are disposable.
layout.buildDirectory.set(file("${System.getProperty("java.io.tmpdir")}/magle-github-build/app"))

android {
    namespace = "com.tai.oeviewer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tai.oeviewer"
        minSdk = 29
        targetSdk = 36
        versionCode = 82
        versionName = "0.8.58"
        // MAGLE's public client registration; existing OE Link tokens remain isolated.
        val oneDriveId = providers.gradleProperty("magleOneDriveClientId")
            .orElse("7d995065-a18c-4b0d-9b3a-0917e773dc62").get()
        require(Regex("[0-9a-fA-F-]{36}").matches(oneDriveId))
        buildConfigField("String", "ONEDRIVE_CLIENT_ID", "\"$oneDriveId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.12.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.documentfile:documentfile:1.0.0")
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation("com.google.android.gms:play-services-auth:22.0.0")
    implementation("com.dropbox.core:dropbox-core-sdk:7.0.0")
    implementation("eu.agno3.jcifs:jcifs-ng:2.1.10")
    implementation("io.minio:minio:8.6.0")
    // Android lacks StAX; retain MinIO's DTD/external-entity protections with a real provider.
    implementation("javax.xml.stream:stax-api:1.0-2")
    implementation("com.fasterxml.woodstox:woodstox-core:6.5.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250107")
}

plugins {
    id("com.android.application") version "8.5.2"
}

android {
    namespace = "com.example.glowring"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.glowring"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.connecthub"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.connecthub"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)


    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:34.2.0"))

    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth")

    // Firestore
    implementation("com.google.firebase:firebase-firestore")


    // Firebase Cloud Messaging (version managed by the BOM)
    implementation("com.google.firebase:firebase-messaging")

    // Image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Voice message waveform
    implementation("com.github.massoudss:waveformSeekBar:5.0.2")


    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
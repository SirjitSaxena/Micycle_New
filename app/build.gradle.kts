plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.micycle"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.micycle"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth) {
        exclude(group = "com.google.firebase", module = "firebase-common")
    }
    implementation(libs.googleid) {
        exclude(group = "com.google.firebase", module = "firebase-common")
    }
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.squareup.picasso:picasso:2.8")
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.sendinblue:sib-api-v3-sdk:6.0.0")
}

// Add this configurations block to force a specific version of firebase-common
configurations.all {
    resolutionStrategy {
        force("com.google.firebase:firebase-common:20.4.3")
    }
}

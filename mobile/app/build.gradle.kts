plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.explicitapp3"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.explicitapp3"
        minSdk = 35
        targetSdk = 35
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
    androidResources {
        noCompress += "tflite"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.play.services.vision)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)


//    runtimeOnly("com.google.ai.edge.litert:litert:1.4.0")
//    runtimeOnly("com.google.ai.edge.litert:litert-support:1.4.0")
//    implementation("org.tensorflow:tensorflow-lite-task-text:0.4.4")
//    implementation("com.google.ai.edge.litert:litert-metadata:1.4.0")
//    implementation("com.google.ai.edge.litert:litert-gpu:1.4.0")
//    implementation("com.google.ai.edge.litert:litert-gpu-api:1.4.0")
//    runtimeOnly("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.4")

//    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")
//    implementation(libs.tensorflow.lite)
//    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-task-text:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu-api:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-api:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.16.1")
    implementation ("eu.bolt:screenshotty:1.0.4")

    // ui
//    implementation ("androidx.compose.material3:material3:1.3.2")
//    implementation ("androidx.compose.material3:material3-window-size-class:1.3.2")
//    implementation ("androidx.compose.material3:material3-adaptive-navigation-suite:1.5.0-alpha04")
    implementation ("com.google.android.flexbox:flexbox:3.0.0")

    implementation("ai.djl:api:0.34.0")
    implementation("ai.djl.huggingface:tokenizers:0.34.0")
    implementation("ai.djl.tensorflow:tensorflow-engine:0.34.0")
//    runtimeOnly("ai.djl.tensorflow:tensorflow-native-auto:0.25.0")

}
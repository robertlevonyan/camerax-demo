plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("androidx.navigation.safeargs")
}

android {
    compileSdkVersion(31)
    defaultConfig {
        applicationId = "com.robertlevonyan.demo.camerax"
        minSdkVersion(21)
        targetSdkVersion(31)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.0")

    implementation("com.google.android.material:material:1.5.0-alpha01")

    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("androidx.core:core-ktx:1.7.0-alpha01")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("androidx.camera:camera-core:1.1.0-alpha06")
    implementation("androidx.camera:camera-camera2:1.1.0-alpha06")
    implementation("androidx.camera:camera-lifecycle:1.1.0-alpha06")
    implementation("androidx.camera:camera-extensions:1.0.0-alpha26")
    implementation("androidx.camera:camera-view:1.0.0-alpha26")
    implementation("androidx.fragment:fragment:1.3.5")
    implementation("androidx.fragment:fragment-ktx:1.3.5")
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    implementation("io.coil-kt:coil:1.3.0")
    implementation("io.coil-kt:coil-video:1.1.1")
}

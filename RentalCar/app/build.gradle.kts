plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.fakeuber"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.fakeuber"
        minSdk = 24
        targetSdk = 37
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
    buildFeatures {
        // Sinh ra một class binding cho mỗi layout (activity_main.xml -> ActivityMainBinding),
        // nhờ đó viết binding.btnNext thay cho findViewById(R.id.btnNext).
        viewBinding = true
    }
    testOptions {
        // Espresso dễ lỗi khi có animation; Gradle tự tắt animation lúc chạy test.
        animationsDisabled = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestImplementation(libs.androidx.junit)
}
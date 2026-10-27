// 카릴레이 차량 앱 (Pleos Connect / Android Automotive OS)
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// gradle.properties 의 carrelay.pleosSdk=true 이면 Pleos Vehicle SDK 를 연동합니다.
val usePleosSdk: Boolean = providers.gradleProperty("carrelay.pleosSdk")
    .map { it.toBoolean() }
    .getOrElse(false)

android {
    namespace = "com.carrelay.vehicle"
    compileSdk = 35

    // AAOS 표준 Car API (CarPropertyManager, CarUxRestrictionsManager)
    useLibrary("android.car")

    defaultConfig {
        applicationId = "com.carrelay.vehicle"
        minSdk = 28 // Pleos Playground 배포 조건 (서명 키 교체 지원)
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("boolean", "USE_PLEOS_SDK", usePleosSdk.toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 공모전 제출용 release APK 서명 설정은 docs/01_개발환경_세팅.md 참고
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDir(if (usePleosSdk) "src/pleos/kotlin" else "src/simulated/kotlin")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    if (usePleosSdk) {
        implementation(libs.pleos.vehicle)
    }

    testImplementation(libs.junit)
}

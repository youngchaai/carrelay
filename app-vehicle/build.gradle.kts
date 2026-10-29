import java.util.Properties

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

// 제출용 release 서명: 서명 키와 비밀번호는 저장소 밖(../carrelay-keys/)에 둡니다. 없으면 서명하지 않고 빌드합니다.
// 다른 위치를 쓰려면 -Pcarrelay.signing=경로/keystore.properties
val signingFile = rootProject.file(
    providers.gradleProperty("carrelay.signing").getOrElse("../carrelay-keys/keystore.properties"),
)
val signingProps: Properties? = signingFile.takeIf { it.isFile }?.let { f -> Properties().apply { f.inputStream().use { load(it) } } }

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
        versionName = "1.0.0" // 공모전 제출판
        // 상수로 두면 Kotlin 이 값을 코드에 복사해, 빌드 옵션을 바꿔도 증분 빌드에서 옛 값이 남을 수 있습니다.
        // 메서드 호출로 초기화해 상수가 아니게 합니다.
        buildConfigField("boolean", "USE_PLEOS_SDK", "Boolean.parseBoolean(\"$usePleosSdk\")")
        // 차량(arm64)과 에뮬레이터(x86_64, Apple Silicon 은 arm64)에서 쓰는 것만 넣어 APK 를 줄입니다.
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }

    // 제출 APK 크기를 줄이려고 코드·네이티브 라이브러리를 압축해 넣습니다(동작은 같음).
    packaging {
        dex { useLegacyPackaging = true }
        jniLibs { useLegacyPackaging = true }
    }

    signingConfigs {
        if (signingProps != null) {
            create("release") {
                storeFile = signingFile.parentFile.resolve(signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Pleos SDK 는 AIDL·리플렉션을 쓰므로 코드 축소를 끄고 그대로 넣습니다.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 만드는 방법: docs/01_개발환경_세팅.md 8번
            signingConfig = signingConfigs.findByName("release")
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
        implementation(libs.pleos.navi)
        implementation(libs.pleos.location)
        implementation(libs.pleos.stt)
    }

    testImplementation(libs.junit)
}

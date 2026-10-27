import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 차량·안드로이드에 의존하지 않는 순수 Kotlin 로직 모듈.
// 인수인계 브리핑, 메모 상태, 운전자 세션, 교대 권유, 케어 모드 판단을 담고
// PC에서 바로 단위 테스트할 수 있습니다:  gradlew :core:test
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

package com.carrelay.vehicle.feature

/** 말 한 번 듣기의 결과 */
sealed interface VoiceResult {
    data class Heard(val text: String) : VoiceResult
    data class Failed(val message: String) : VoiceResult
    data object Cancelled : VoiceResult
}

/**
 * 말로 남기는 메모의 음성 인식.
 * - Pleos 빌드: Pleos SpeechToText SDK (차량 내장 인식 엔진, ON_DEVICE)
 * - 가상 빌드: 마이크 없이 예시 문장을 골라 말한 것처럼 처리합니다.
 */
interface VoiceInput {
    /** 실제 마이크로 들을 수 있으면 true. false 면 화면에서 예시 문장을 고르게 합니다. */
    val isAvailable: Boolean

    /** 한 번 듣고 최종 문장을 돌려줍니다. 듣는 동안의 중간 결과는 [onPartial] 로 알려 줍니다. */
    suspend fun listen(onPartial: (String) -> Unit): VoiceResult

    /** 듣기를 멈춥니다(진행 중인 [listen] 은 Cancelled 로 끝납니다). */
    fun cancel()
}

/** 가상 빌드용: 마이크를 쓰지 않습니다. */
class ExampleVoiceInput : VoiceInput {
    override val isAvailable: Boolean = false
    override suspend fun listen(onPartial: (String) -> Unit): VoiceResult =
        VoiceResult.Failed("이 빌드에서는 예시 문장을 골라 주세요")
    override fun cancel() = Unit
}

/** 마이크가 없는 환경에서 고를 수 있는 예시 문장 (말로 하는 방법도 함께 알려 줍니다) */
val VOICE_MEMO_EXAMPLES = listOf(
    "다음 사람한테 주유하라고 전해 줘",
    "엄마한테 타이어 바람 넣으라고 전해 줘",
    "와이퍼에서 소리 난다고 알려 줘",
    "내일 장거리 간다고 남겨 줘",
)

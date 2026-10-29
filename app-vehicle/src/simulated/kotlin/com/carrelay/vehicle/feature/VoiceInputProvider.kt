package com.carrelay.vehicle.feature

import android.content.Context

/** carrelay.pleosSdk=false (기본): 마이크 대신 예시 문장을 골라 말한 것처럼 처리합니다. */
object VoiceInputProvider {
    @Suppress("UNUSED_PARAMETER")
    fun create(context: Context): VoiceInput = ExampleVoiceInput()
}

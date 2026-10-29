package com.carrelay.vehicle.feature

import android.content.Context

/** carrelay.pleosSdk=true: Pleos SpeechToText SDK 로 차량 마이크를 씁니다. */
object VoiceInputProvider {
    fun create(context: Context): VoiceInput = PleosVoiceInput(context)
}

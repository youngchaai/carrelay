package com.carrelay.vehicle.feature

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * 음성 브리핑·교대 안내·안부 확인.
 *
 * Pleos TextToSpeech SDK 는 사용 신청이 필요해서, 승인 전에는 Android 기본 TTS 를 씁니다.
 * 한국어 음성이 없는 에뮬레이터에서는 조용히 넘어가고 화면 표시로 대신합니다.
 */
class Speaker(context: Context) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var pending: String? = null

    /** 한국어 음성을 쓸 수 있으면 true */
    var available: Boolean = false
        private set

    /** 설정에서 끌 수 있습니다. */
    var enabled: Boolean = true

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "TTS 초기화 실패: $status")
            return
        }
        val result = tts.setLanguage(Locale.KOREAN)
        available = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        ready = true
        pending?.let { speak(it) }
        pending = null
    }

    fun speak(text: String) {
        if (!enabled) return
        if (!ready) {
            pending = text
            return
        }
        if (!available) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "carrelay-${System.nanoTime()}")
    }

    fun stop() {
        if (ready) tts.stop()
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    private companion object {
        const val TAG = "Speaker"
    }
}

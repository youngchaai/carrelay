package com.carrelay.vehicle.feature

// ────────────────────────────────────────────────────────────────────────────
// Pleos SpeechToText SDK 3.3.1 연동 (carrelay.pleosSdk=true 일 때만 컴파일)
// 함수 이름은 SDK 아카이브의 실제 클래스와 공식 문서에서 확인했습니다 (2026-10-29).
//   SpeechToText(context, Mode.ON_DEVICE) → initialize(listener) → addListener() → setLocale(KOREA)
//   → request() (차량 마이크) → ResultListener.onUpdated(text, completed) → stop()
// ON_DEVICE 모드는 차량 안의 인식 엔진을 써서 네트워크·앱 등록(Client ID/Secret)이 필요 없습니다.
// 권한: pleos.car.permission.STT_SERVICE (매니페스트)
// ────────────────────────────────────────────────────────────────────────────
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import android.content.Context
import android.util.Log
import com.carrelay.vehicle.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale

class PleosVoiceInput(context: Context) : VoiceInput {

    private val appContext = context.applicationContext
    private val stt = SpeechToText(appContext, Mode.ON_DEVICE)

    @Volatile private var ready = false
    @Volatile private var initError: ErrorType? = null
    @Volatile private var session: CompletableDeferred<VoiceResult>? = null
    @Volatile private var lastText = ""
    @Volatile private var onPartial: (String) -> Unit = {}

    override val isAvailable: Boolean get() = initError == null

    private val listener = object : ResultListener {
        override fun onReady() {
            Log.i(TAG, "인식 준비됨")
        }

        override fun onStartedRecognition() {
            Log.i(TAG, "인식 시작")
        }

        override fun onUpdated(stt: String, completed: Boolean) {
            Log.i(TAG, "인식 결과: '$stt' (최종=$completed)")
            if (stt.isNotBlank()) {
                lastText = stt
                onPartial(stt)
            }
            if (completed) session?.complete(heardOrMissed())
        }

        override fun onUpdatedEpdData(on: Long, off: Long) = Unit

        override fun onUpdatedRms(rms: Double) = Unit

        override fun onEndedRecognition() {
            Log.i(TAG, "인식 끝")
            session?.complete(heardOrMissed())
        }

        override fun onError(errorType: ErrorType) {
            Log.w(TAG, "인식 오류: $errorType")
            session?.complete(if (lastText.isNotBlank()) VoiceResult.Heard(lastText) else VoiceResult.Failed(message(errorType)))
        }
    }

    init {
        try {
            stt.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    try {
                        stt.addListener(listener)
                        stt.setLocale(Locale.KOREA)
                        ready = true
                        Log.i(TAG, "SpeechToText 초기화 성공 (ON_DEVICE, ko-KR)")
                    } catch (t: Throwable) {
                        initError = ErrorType.UNKNOWN
                        Log.w(TAG, "리스너 등록 실패", t)
                    }
                }

                override fun onFailure(errorType: ErrorType) {
                    initError = errorType
                    Log.w(TAG, "SpeechToText 초기화 실패: $errorType")
                }
            })
        } catch (t: Throwable) {
            initError = ErrorType.UNKNOWN
            Log.w(TAG, "SpeechToText 연결 실패", t)
        }
    }

    override suspend fun listen(onPartial: (String) -> Unit): VoiceResult {
        // 서비스 연결은 비동기라 잠깐 기다립니다.
        withTimeoutOrNull(3_000L) { while (!ready && initError == null) delay(100) }
        if (!ready) return VoiceResult.Failed(initError?.let(::message) ?: "음성 인식을 준비하지 못했어요")

        this.onPartial = onPartial
        lastText = ""
        val done = CompletableDeferred<VoiceResult>().also { session = it }
        return try {
            val testAudio = debugTestAudio()
            if (testAudio != null) {
                sendTestAudio(testAudio)
            } else {
                stt.request()
            }
            // 말이 끝나면 엔진이 스스로 끝을 알려 줍니다. 너무 길면 들은 데까지 씁니다.
            withTimeoutOrNull(LISTEN_LIMIT_MILLIS) { done.await() } ?: run {
                Log.w(TAG, "듣기 시간 초과")
                runCatching { stt.stop() }
                heardOrMissed()
            }
        } catch (e: CancellationException) {
            runCatching { stt.stop() }
            throw e
        } catch (t: Throwable) {
            Log.w(TAG, "음성 인식 요청 실패", t)
            VoiceResult.Failed("음성 인식을 시작하지 못했어요")
        } finally {
            session = null
            this.onPartial = {}
        }
    }

    override fun cancel() {
        session?.complete(VoiceResult.Cancelled)
        runCatching { stt.stop() }
    }

    private fun heardOrMissed(): VoiceResult =
        if (lastText.isNotBlank()) VoiceResult.Heard(lastText) else VoiceResult.Failed("말씀을 듣지 못했어요. 다시 말해 주세요")

    /**
     * 개발용(디버그 빌드만): 앱 폴더에 voice_test.pcm(16kHz·16bit·mono)이 있으면 마이크 대신 그 소리로 인식합니다.
     * 에뮬레이터에서 실제로 말하지 않고 인식 엔진을 확인하려고 둡니다. 한 번 쓰면 지웁니다.
     */
    private fun debugTestAudio(): File? {
        if (!BuildConfig.DEBUG) return null
        return File(appContext.filesDir, "voice_test.pcm").takeIf { it.isFile && it.length() > 0 }
    }

    private suspend fun sendTestAudio(file: File) = withContext(Dispatchers.IO) {
        val pcm = file.readBytes()
        file.delete()
        Log.i(TAG, "테스트 음성 파일로 인식 (${pcm.size} bytes)")
        // 실제 말하는 속도처럼 100ms(3,200바이트)씩 보냅니다.
        var offset = 0
        while (offset < pcm.size) {
            val end = minOf(offset + BYTES_PER_100_MS, pcm.size)
            stt.sendAudio(pcm.copyOfRange(offset, end))
            offset = end
            delay(100)
        }
        stt.completeAudioSend()
    }

    private fun message(error: ErrorType): String = when (error) {
        ErrorType.MIC_ERROR -> "마이크를 쓸 수 없어요"
        ErrorType.PERMISSION_DENIED -> "음성 인식 권한이 없어요"
        ErrorType.UNAVAILABLE_LANGUAGE -> "한국어 음성 인식을 쓸 수 없어요"
        ErrorType.ON_DEVICE_ENGINE_ERROR -> "차량 음성 인식 엔진에 문제가 있어요"
        ErrorType.TIMEOUT -> "말씀을 듣지 못했어요. 다시 말해 주세요"
        else -> "음성 인식에 실패했어요 ($error)"
    }

    private companion object {
        const val TAG = "PleosVoice"
        const val LISTEN_LIMIT_MILLIS = 15_000L
        const val BYTES_PER_100_MS = 3_200
    }
}

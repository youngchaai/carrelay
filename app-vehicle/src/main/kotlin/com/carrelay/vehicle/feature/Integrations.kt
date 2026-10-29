package com.carrelay.vehicle.feature

import android.content.Context
import android.util.Log
import com.carrelay.core.reststop.RestStop
import com.carrelay.core.reststop.RestStopFinder
import com.carrelay.core.text.KoreanText
import com.carrelay.vehicle.data.GuardianAlert
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

/** 쉼터 경유지 추가 결과 */
data class RestStopResult(
    val added: Boolean,
    val stopName: String?,
    val distanceKm: Double?,
    /** 화면 토스트용 문장 */
    val message: String,
)

/**
 * 휴게소·졸음쉼터 경유지 추가와 보호자 알림용 현재 위치.
 * - 가상 차량 빌드: [SimulatedRestStopNavigator] (고정 위치에서 실제 쉼터 목록으로 고름)
 * - Pleos 빌드: PleosRestStopNavigator (FusedLocation 으로 위치·방향, NaviHelper 로 경유지 추가·주소)
 */
interface RestStopNavigator {
    suspend fun addNearestRestStop(): RestStopResult

    /** 보호자 알림에 넣을 현재 위치 설명. 위치는 케어 모드 3단계에서만 사용합니다. */
    suspend fun currentLocationText(): String
}

/**
 * 전국 휴게소·졸음쉼터 목록 (assets/rest_stops.json, © OpenStreetMap contributors, ODbL).
 * 처음 쓸 때 한 번 읽습니다.
 */
class RestStopCatalog(private val context: Context) {
    @Serializable
    private data class Row(val n: String, val a: Double, val o: Double, val k: String)

    val stops: List<RestStop> by lazy {
        try {
            val text = context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
            Json.decodeFromString<List<Row>>(text).map {
                RestStop(it.n, it.a, it.o, if (it.k == "r") RestStop.Kind.SHELTER else RestStop.Kind.SERVICE)
            }
        } catch (e: Exception) {
            Log.w("RestStopCatalog", "쉼터 목록을 읽지 못했어요", e)
            emptyList()
        }
    }

    private companion object {
        const val ASSET = "rest_stops.json"
    }
}

/** "6km" (1km 미만도 1km 로) */
internal fun kmText(distanceKm: Double): String = String.format(Locale.KOREA, "%.0fkm", distanceKm.coerceAtLeast(1.0))

internal fun objectParticle(name: String): String = KoreanText.obj(name)

/**
 * 가상 차량 빌드용: 경부고속도로 수원~기흥 사이를 남쪽으로 달리는 것으로 보고, 실제 쉼터 목록에서 앞쪽 가장 가까운 곳을 고릅니다.
 * 내비게이션 연동은 하지 않으므로 문장 끝에 (시뮬레이션)을 붙입니다.
 */
class SimulatedRestStopNavigator(private val catalog: RestStopCatalog) : RestStopNavigator {
    override suspend fun addNearestRestStop(): RestStopResult {
        val pick = RestStopFinder.nearest(catalog.stops, SIM_LAT, SIM_LON, SIM_HEADING)
            ?: return RestStopResult(false, null, null, "가까운 쉼터를 찾지 못했어요 (시뮬레이션)")
        return RestStopResult(
            added = true,
            stopName = pick.stop.name,
            distanceKm = pick.distanceKm,
            message = "${objectParticle(pick.stop.name)} 경유지로 추가했어요 · ${kmText(pick.distanceKm)} (시뮬레이션)",
        )
    }

    override suspend fun currentLocationText(): String = "경부고속도로 수원 부근 (시뮬레이션)"

    private companion object {
        const val SIM_LAT = 37.285
        const val SIM_LON = 127.091
        const val SIM_HEADING = 175.0
    }
}

/**
 * 보호자 폰 알림.
 * TODO(확장): 동기화 서버 → 보호자 폰 푸시. Pleos Connect 는 Google 서비스가 없는 AAOS 이므로
 * 차량 앱은 자체 서버 API 를 호출하고, 서버가 보호자 폰으로 푸시를 보냅니다.
 */
interface GuardianNotifier {
    suspend fun send(alert: GuardianAlert)
}

class LocalGuardianNotifier : GuardianNotifier {
    override suspend fun send(alert: GuardianAlert) {
        Log.i("GuardianNotifier", "보호자 알림(서버 연동 전, 로컬 기록만): ${alert.message}")
    }
}

package com.carrelay.vehicle.feature

// ────────────────────────────────────────────────────────────────────────────
// Pleos NaviHelper SDK 3.0.5 + FusedLocation SDK 1.1.3 연동 (carrelay.pleosSdk=true 일 때만 컴파일)
// 함수 이름은 SDK 아카이브의 실제 클래스에서 확인했습니다 (2026-10-29).
//   - NaviHelper: initialize() → addListener() → getDestinationInfo() / addWaypoint() / requestRoute()
//                 / getCurrentLocationInfo() → 결과는 NaviHelperEventListener 콜백
//   - FusedLocation: initialize() → getLastKnownLocation() / registerFusedLocationCallback()
// NaviHelper 에는 주변 검색이 없어, 쉼터 좌표는 앱에 넣은 목록(OpenStreetMap)에서 고릅니다.
// 위치는 필요할 때(쉼터 고르기·보호자 알림)만 읽고, 저장하지 않습니다.
// ────────────────────────────────────────────────────────────────────────────
import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.WaypointChangedInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import com.carrelay.core.reststop.RestStopFinder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

class PleosRestStopNavigator(
    context: Context,
    private val catalog: RestStopCatalog,
) : RestStopNavigator {

    /** 위치 한 점: 어디서 왔는지 함께 남깁니다(로그·문서용). */
    private data class Position(val latitude: Double, val longitude: Double, val headingDeg: Double?, val source: String)

    private val navi = NaviHelper(context)
    private val location = FusedLocation(context)
    private var locationReady = false

    // 콜백으로 오는 결과를 기다리는 자리 (요청할 때마다 새로 만듭니다)
    @Volatile private var pendingDestination: CompletableDeferred<DestinationInfo?>? = null
    @Volatile private var pendingNaviLocation: CompletableDeferred<CurrentLocationInfo?>? = null
    @Volatile private var pendingRoute: CompletableDeferred<Boolean>? = null
    /** 내비가 알려 준 실제 도로 거리·시간 (안내 시작 때) */
    @Volatile private var lastRouteStart: RouteStartInfo? = null

    private val naviListener = object : NaviHelperEventListener {
        override fun onDestinationInfo(info: DestinationInfo) {
            pendingDestination?.complete(info)
        }

        override fun onCurrentLocationInfo(info: CurrentLocationInfo) {
            pendingNaviLocation?.complete(info)
        }

        override fun onWaypointChanged(info: WaypointChangedInfo) {
            Log.i(TAG, "경유지 바뀜: $info")
            pendingRoute?.complete(true)
        }

        override fun onRouteStarted(info: RouteStartInfo) {
            Log.i(TAG, "경로 안내 시작: $info")
            lastRouteStart = info
            pendingRoute?.complete(true)
        }

        override fun onRoutingCompleted() {
            Log.i(TAG, "경로 계산 끝")
            pendingRoute?.complete(true)
        }

        override fun onError(code: NaviErrorCode) {
            Log.w(TAG, "내비 오류: $code")
            pendingRoute?.complete(false)
        }
    }

    init {
        // 서비스 연결은 비동기라 미리 붙여 둡니다(필요한 순간에 연결을 기다리지 않게).
        try {
            navi.initialize()
            navi.addListener(naviListener)
        } catch (t: Throwable) {
            Log.w(TAG, "NaviHelper 연결 실패", t)
        }
        try {
            if (location.isSupported()) {
                location.initialize()
                locationReady = true
            } else {
                Log.w(TAG, "이 환경은 FusedLocation 을 지원하지 않아요. 내비 현재 위치를 씁니다")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "FusedLocation 연결 실패", t)
        }
    }

    override suspend fun addNearestRestStop(): RestStopResult {
        val here = currentPosition()
            ?: return RestStopResult(false, null, null, "현재 위치를 알 수 없어 쉼터를 찾지 못했어요")
        val pick = RestStopFinder.nearest(catalog.stops, here.latitude, here.longitude, here.headingDeg)
            ?: return RestStopResult(false, null, null, "${RestStopFinder.DEFAULT_MAX_KM.toInt()}km 안에 쉼터가 없어요").also { Log.w(TAG, "쉼터 목록 ${catalog.stops.size}곳 중 반경 안에 없음") }
        val stop = pick.stop
        val km = kmText(pick.distanceKm)
        Log.i(TAG, "위치(${here.source}) ${here.latitude},${here.longitude} 방향 ${here.headingDeg} → ${stop.name} $km (앞쪽=${pick.ahead})")

        // 목적지가 있으면 첫 번째 경유지로, 없으면 쉼터까지 바로 안내합니다.
        val hasDestination = destination() != null
        lastRouteStart = null
        val done = CompletableDeferred<Boolean>().also { pendingRoute = it }
        try {
            if (hasDestination) {
                Log.i(TAG, "목적지 있음 → 경유지 추가 요청")
                navi.addWaypoint(
                    RequestWaypointInfo(
                        latitude = stop.latitude,
                        longitude = stop.longitude,
                        poiName = stop.name,
                        waypointIndex = WaypointIndex.FIRST,
                        poiId = "",
                        address = "",
                        poiSubId = "0",
                    ),
                )
            } else {
                Log.i(TAG, "목적지 없음 → 쉼터까지 안내 요청")
                navi.requestRoute(
                    RouteInfo(
                        latitude = stop.latitude,
                        longitude = stop.longitude,
                        displayLatitude = null,
                        displayLongitude = null,
                        poiName = stop.name,
                        poiId = "",
                        poiSubId = "0",
                        address = "",
                    ),
                )
            }
        } catch (t: Throwable) {
            Log.w(TAG, "내비 요청 실패", t)
            pendingRoute = null
            return RestStopResult(false, stop.name, pick.distanceKm, "내비게이션에 연결하지 못했어요. 가까운 쉼터: ${stop.name} · $km")
        }
        // 내비가 결과를 알려 주면 확정, 시간 안에 소식이 없으면 요청은 보냈다고 알립니다.
        // 경로 계산이 5초 넘게 걸리기도 해서(에뮬레이터 확인) 10초까지 기다립니다.
        val ok = withTimeoutOrNull(10_000L) { done.await() }
        pendingRoute = null
        Log.i(TAG, "내비 응답: ${ok ?: "시간 초과(요청은 보냄)"}")
        return if (ok == false) {
            RestStopResult(false, stop.name, pick.distanceKm, "내비게이션이 경로를 찾지 못했어요. 가까운 쉼터: ${stop.name} · $km")
        } else {
            RestStopResult(
                added = true,
                stopName = stop.name,
                distanceKm = pick.distanceKm,
                message = if (hasDestination) "${objectParticle(stop.name)} 경유지로 추가했어요 · $km" else "${stop.name}까지 안내를 시작했어요 · ${routeText() ?: km}",
            )
        }
    }

    override suspend fun currentLocationText(): String {
        val info = naviCurrentLocation()
        info?.address?.takeIf { it.isNotBlank() }?.let { return it }
        val here = currentPosition() ?: return "위치를 알 수 없어요"
        return String.format(Locale.KOREA, "위도 %.4f, 경도 %.4f", here.latitude, here.longitude)
    }

    /**
     * 위치: 내비 현재 위치 → FusedLocation 마지막 위치 → 잠깐 실시간 위치 순서로.
     * 경유지는 내비 경로에 더하는 것이라 내비가 보는 위치를 먼저 씁니다.
     * (에뮬레이터에서는 FusedLocation 이 안드로이드 기본 GPS 위치를 돌려줘 내비 위치와 다를 수 있습니다)
     */
    private suspend fun currentPosition(): Position? {
        val navi = naviCurrentLocation()
        val pos = when {
            navi != null && (navi.latitude != 0.0 || navi.longitude != 0.0) ->
                Position(navi.latitude, navi.longitude, navi.heading.takeIf { !it.isNaN() }, "NaviHelper")
            locationReady -> fusedOnce(useCallback = false) ?: fusedOnce(useCallback = true)
            else -> null
        }
        Log.i(TAG, if (pos == null) "위치 없음 (내비·FusedLocation 모두 응답 없음)" else "위치(${pos.source}) ${pos.latitude},${pos.longitude} 방향 ${pos.headingDeg}")
        return pos
    }

    private suspend fun fusedOnce(useCallback: Boolean): Position? {
        val got = CompletableDeferred<LocationData?>()
        val listener = object : FusedLocationListener {
            override fun onChanged(data: LocationData?) {
                got.complete(data)
            }

            override fun onError(e: Exception) {
                Log.w(TAG, "FusedLocation 오류: ${e.message}")
                got.complete(null)
            }
        }
        return try {
            if (useCallback) location.registerFusedLocationCallback(listener) else location.getLastKnownLocation(listener)
            val data = withTimeoutOrNull(if (useCallback) 3_000L else 1_500L) { got.await() }
            if (data == null) Log.w(TAG, "FusedLocation ${if (useCallback) "실시간" else "마지막"} 위치 없음")
            data?.let { Position(it.latitude, it.longitude, it.heading.toDouble().takeIf { _ -> it.confidence > 0f }, "FusedLocation") }
        } catch (t: Throwable) {
            Log.w(TAG, "FusedLocation 호출 실패: ${t.message}")
            null
        } finally {
            if (useCallback) runCatching { location.unregisterFusedLocationCallback(listener) }
        }
    }

    private suspend fun naviCurrentLocation(): CurrentLocationInfo? {
        val wait = CompletableDeferred<CurrentLocationInfo?>().also { pendingNaviLocation = it }
        return try {
            navi.getCurrentLocationInfo()
            withTimeoutOrNull(2_000L) { wait.await() }
        } catch (t: Throwable) {
            Log.w(TAG, "내비 현재 위치 요청 실패: ${t.message}")
            null
        } finally {
            pendingNaviLocation = null
        }
    }

    private suspend fun destination(): DestinationInfo? {
        val wait = CompletableDeferred<DestinationInfo?>().also { pendingDestination = it }
        return try {
            navi.getDestinationInfo()
            withTimeoutOrNull(1_500L) { wait.await() }?.takeIf { it.latitude != 0.0 || it.longitude != 0.0 }
        } catch (t: Throwable) {
            Log.w(TAG, "목적지 확인 실패: ${t.message}")
            null
        } finally {
            pendingDestination = null
        }
    }

    /** "6.3km, 약 14분" (내비가 알려 준 도로 기준) */
    private fun routeText(): String? = lastRouteStart?.let { r ->
        if (r.distance <= 0) null else String.format(Locale.KOREA, "%.1fkm, 약 %d분", r.distance / 1000.0, (r.duration + 59) / 60)
    }

    private companion object {
        const val TAG = "PleosRestStop"
    }
}

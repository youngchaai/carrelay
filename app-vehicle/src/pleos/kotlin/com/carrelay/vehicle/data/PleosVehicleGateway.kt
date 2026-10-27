package com.carrelay.vehicle.data

// ────────────────────────────────────────────────────────────────────────────
// Pleos Vehicle SDK 2.3.2 연동부 (gradle.properties: carrelay.pleosSdk=true 일 때만 컴파일)
//
// 확인된 것 (document.pleos.ai, 2026-10-27 기준)
//   - 의존성 ai.pleos.playground:Vehicle:2.3.2, 패키지 루트 ai.pleos.playground.vehicle
//   - Vehicle(context) → initialize() → getDoor() 등 도메인 객체 → release()
//   - 결과는 onComplete / onFailed 비동기 콜백, 구독은 registerXxx / unregisterXxx
//   - Door: checkDoorPositionCapabilities, getCurrentDoorPosition(DoorArea(DoorValue.ROW_1_LEFT)),
//           registerDoorPosition(doorPositionListener = DoorPositionListener)
//
// 아직 확인할 것 (10/25 API 스파이크 체크리스트 → docs/03_개발일정_체크리스트.md)
//   - 아래 import 의 세부 패키지 경로. 빨간 줄이 뜨면 Android Studio 에서 Alt+Enter 로 고칩니다.
//   - Fuel / EvBattery / Safety / Odometer / Brake 의 정확한 함수 이름과 반환 타입
// ────────────────────────────────────────────────────────────────────────────
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.Door
import ai.pleos.playground.vehicle.api.listener.DoorPositionListener
import ai.pleos.playground.vehicle.area.DoorArea
import ai.pleos.playground.vehicle.value.DoorValue
import android.content.Context
import android.util.Log
import com.carrelay.core.model.DoorEvent
import com.carrelay.core.model.DrivingSample
import com.carrelay.core.model.Gear
import com.carrelay.core.model.Signal
import com.carrelay.core.model.SignalStatus
import com.carrelay.core.model.VehicleSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class PleosVehicleGateway(private val context: Context) : VehicleGateway {

    override val sourceName: String = "Pleos Vehicle SDK"
    override val isSimulated: Boolean = false

    private val vehicle = Vehicle(context)
    private val aaos = AaosCarSignals(context)
    private var scope: CoroutineScope? = null

    private val _driving = MutableStateFlow(DrivingSample(System.currentTimeMillis(), Gear.UNKNOWN))
    override val driving: StateFlow<DrivingSample> = _driving.asStateFlow()

    private val _door = MutableSharedFlow<DoorEvent>(extraBufferCapacity = 8)
    override val driverDoorEvents: Flow<DoorEvent> = _door.asSharedFlow()

    private val brakePressed = MutableStateFlow<Boolean?>(null)
    private var lastDoorPosition: Int? = null
    private var door: Door? = null

    /** 운전석 문 위치: 0 = 닫힘, 0 초과 = 열림 (에뮬레이터 범위 0..1) */
    private val doorListener = object : DoorPositionListener {
        override fun onPositionUpdated(pos: Int?) {
            val p = pos ?: return
            val prev = lastDoorPosition
            lastDoorPosition = p
            if (prev == null) return
            if (prev == 0 && p > 0) _door.tryEmit(DoorEvent.OPENED)
            if (prev > 0 && p == 0) _door.tryEmit(DoorEvent.CLOSED)
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "문 상태 구독 실패", e)
        }
    }

    override fun start() {
        if (scope != null) return
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = s

        vehicle.initialize()
        aaos.start()

        // 기어·속도: AAOS 표준 API, 브레이크 페달: Pleos Brake (TODO)
        s.launch {
            combine(aaos.gear, aaos.speedKmh, brakePressed) { gear, speed, brake ->
                DrivingSample(
                    timestampMillis = System.currentTimeMillis(),
                    gear = gear ?: Gear.UNKNOWN,
                    speedKmh = speed,
                    brakePedalPressed = brake,
                )
            }.collect { _driving.value = it }
        }

        val d = vehicle.getDoor()
        door = d
        d.checkDoorPositionCapabilities(
            onComplete = { _, _ ->
                d.registerDoorPosition(doorPositionListener = doorListener)
            },
            onFailed = { e -> Log.w(TAG, "문 상태 지원 확인 실패: 이 차량에서는 교대 자동 재확인을 끕니다", e) },
        )

        // TODO(스파이크): Brake 도메인으로 페달 상태 구독
        // vehicle.getBrake().registerXxx(...) { pressed -> brakePressed.value = pressed }
    }

    override fun stop() {
        try {
            door?.unregisterDoorPosition(doorPositionListener = doorListener)
        } catch (t: Throwable) {
            Log.w(TAG, "문 구독 해제 실패", t)
        }
        aaos.stop()
        scope?.cancel()
        scope = null
        vehicle.release()
    }

    override fun onPermissionsChanged() {
        aaos.registerSpeedIfPermitted()
    }

    override suspend fun readStatus(): VehicleSnapshot {
        val unavailable = mutableMapOf<Signal, SignalStatus>()

        // TODO(스파이크): API Reference 에서 함수 이름을 확인해 아래 주석을 실제 호출로 바꿉니다.
        //   항상 checkXxxCapabilities → getCurrentXxx 순서로 호출하고, 실패 이유를 unavailable 에 넣습니다.
        //
        // val fuel: Int? = awaitGet { ok, fail -> vehicle.getFuel().getCurrentFuelLevel(onComplete = ok, onFailed = fail) }
        // val soc: Int? = awaitGet { ok, fail -> vehicle.getEvBattery().getCurrentEvBatteryStateOfCharge(onComplete = ok, onFailed = fail) }
        // val odo: Float? = awaitGet { ok, fail -> vehicle.getOdometer().getCurrentDistanceDriven(onComplete = ok, onFailed = fail) }
        // val telltales = awaitGet { ok, fail -> vehicle.getSafety().getCurrentTelltales(onComplete = ok, onFailed = fail) }
        unavailable[Signal.FUEL] = SignalStatus.UNSUPPORTED
        unavailable[Signal.WARNINGS] = SignalStatus.UNSUPPORTED

        return VehicleSnapshot(
            capturedAtMillis = System.currentTimeMillis(),
            unavailable = unavailable,
        )
    }

    /**
     * onComplete / onFailed 콜백 API 를 suspend 함수로 바꿉니다. 실패·시간 초과면 null.
     * (Vehicle 통신은 IPC 라서 결과가 비동기로 옵니다.)
     */
    @Suppress("unused")
    private suspend fun <T> awaitGet(
        timeoutMillis: Long = 3_000L,
        call: (onComplete: (T) -> Unit, onFailed: (Exception) -> Unit) -> Unit,
    ): T? = withTimeoutOrNull(timeoutMillis) {
        suspendCancellableCoroutine<T?> { cont ->
            call(
                { value -> if (cont.isActive) cont.resume(value) },
                { e ->
                    Log.w(TAG, "Vehicle API 실패", e)
                    if (cont.isActive) cont.resume(null)
                },
            )
        }
    }

    @Suppress("unused")
    private fun driverDoorArea() = DoorArea(DoorValue.ROW_1_LEFT)

    private companion object {
        const val TAG = "PleosVehicleGateway"
    }
}

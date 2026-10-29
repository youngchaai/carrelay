package com.carrelay.vehicle.data

// ────────────────────────────────────────────────────────────────────────────
// Pleos Vehicle SDK 2.3.2 연동부 (gradle.properties: carrelay.pleosSdk=true 일 때만 컴파일)
//
// 함수·패키지 이름은 SDK 아카이브(Vehicle-2.3.2.aar)의 실제 클래스에서 확인했습니다 (2026-10-29).
//   - Vehicle(context) → initialize() → getGear() / getBrake() / getDoor() / getFuel() / getEvBattery()
//     / getOdometer() / getSafety() → release()
//   - 조회: getXxx(onComplete, onFailed) 비동기 콜백 / 구독: registerXxx(listener) · unregisterXxx(listener)
//   - 리스너는 ai.pleos.playground.vehicle.listener.*, 문 위치는 api.model.area.DoorArea(DoorArea.ROW_1_LEFT)
//
// 값 조작: Pleos Connect Virtual Console (adb forward tcp:8088 tcp:8088 → http://127.0.0.1:8088/)
// 속도만 AAOS 표준 PERF_VEHICLE_SPEED 로 받습니다(Vehicle SDK 에 속도 API 없음). 기어는 Pleos 를 먼저 씁니다.
// ────────────────────────────────────────────────────────────────────────────
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.telltale.Telltales
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.control.OilLevelState
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.telltale.TelltaleState
import ai.pleos.playground.vehicle.constant.telltale.TelltaleType
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.FuelPercentListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.LowWasherFluidWaringListener
import ai.pleos.playground.vehicle.listener.OilLevelStateListener
import ai.pleos.playground.vehicle.listener.RangeRemainingListener
import ai.pleos.playground.vehicle.listener.TelltaleInfoListener
import android.content.Context
import android.util.Log
import com.carrelay.core.model.DoorEvent
import com.carrelay.core.model.DrivingSample
import com.carrelay.core.model.Gear
import com.carrelay.core.model.Signal
import com.carrelay.core.model.SignalStatus
import com.carrelay.core.model.VehicleSnapshot
import com.carrelay.core.model.WarningLight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
import kotlin.math.roundToInt

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

    private val _status = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val statusChanges: Flow<Unit> = _status.asSharedFlow()

    // 실시간 값 (리스너가 채우고, readStatus 가 모아서 돌려줍니다)
    private val pleosGear = MutableStateFlow<Gear?>(null)
    private val brakePressed = MutableStateFlow<Boolean?>(null)
    @Volatile private var fuelPercent: Int? = null
    @Volatile private var socPercent: Int? = null
    @Volatile private var rangeKm: Int? = null
    @Volatile private var odometerKm: Double? = null
    /** 계기판 경고등(telltale). 서드파티 앱은 PRIVATE_CAR_SAFETY 가 없어 보통 못 받습니다. */
    @Volatile private var warnings: Set<WarningLight>? = null
    @Volatile private var telltaleDenied = false
    // CAR_SAFETY 로 받을 수 있는 소모품 상태
    @Volatile private var engineOilLow: Boolean? = null
    @Volatile private var brakeFluidLow: Boolean? = null
    @Volatile private var washerFluidLow: Boolean? = null
    private var lastDoorPosition: Int? = null
    private val driverDoor = DoorArea(DoorValue.ROW_1_LEFT)

    // ── 리스너 ─────────────────────────────────────────────

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) { pleosGear.value = mapGear(gear) }
        override fun onFailed(e: Exception) = logFail("기어", e)
    }

    private val brakeListener = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(pressed: Boolean?) { brakePressed.value = pressed }
        override fun onFailed(e: Exception) = logFail("브레이크 페달", e)
    }

    /** 운전석 문 위치: 0 = 닫힘, 0 초과 = 열림 */
    private val doorListener = object : DoorPositionListener {
        override fun onPositionUpdated(pos: Int?) {
            val p = pos ?: return
            val prev = lastDoorPosition
            lastDoorPosition = p
            if (prev == null) return
            if (prev == 0 && p > 0) _door.tryEmit(DoorEvent.OPENED)
            if (prev > 0 && p == 0) _door.tryEmit(DoorEvent.CLOSED)
        }
        override fun onFailed(e: Exception) = logFail("운전석 문", e)
    }

    private val fuelListener = object : FuelPercentListener {
        override fun onFuelPercentUpdated(percent: Int?) { fuelPercent = percent; changed() }
        override fun onFailed(e: Exception) = logFail("연료", e)
    }

    private val socListener = object : EvBatteryStateOfChargeListener {
        override fun onEvBatteryStateOfChargeUpdated(percent: Int?) { socPercent = percent; changed() }
        override fun onFailed(e: Exception) = logFail("배터리", e)
    }

    private val rangeListener = object : RangeRemainingListener {
        override fun onRangeRemainingChanged(range: Float?) { rangeKm = range?.let(::toKm); changed() }
        override fun onFailed(e: Exception) = logFail("주행 가능 거리", e)
    }

    private val odometerListener = object : DistanceDrivenListener {
        override fun onDistanceDrivenUpdated(distance: Float?) { odometerKm = distance?.toDouble(); changed() }
        override fun onFailed(e: Exception) = logFail("누적 주행거리", e)
    }

    private val engineOilListener = object : OilLevelStateListener {
        override fun onOilLevelUpdated(state: OilLevelState?) { engineOilLow = state?.let(::isLow); changed() }
        override fun onFailed(e: Exception) = logFail("엔진오일", e)
    }

    private val brakeFluidListener = object : OilLevelStateListener {
        override fun onOilLevelUpdated(state: OilLevelState?) { brakeFluidLow = state?.let(::isLow); changed() }
        override fun onFailed(e: Exception) = logFail("브레이크액", e)
    }

    private val washerListener = object : LowWasherFluidWaringListener {
        override fun onLowWasherFluidWaringUpdated(low: Boolean?) { washerFluidLow = low; changed() }
        override fun onFailed(e: Exception) = logFail("워셔액", e)
    }

    private val telltaleListener = object : TelltaleInfoListener {
        override fun onTelltaleInfoUpdated(info: Telltales?) { warnings = info?.let(::mapTelltales); changed() }
        override fun onFailed(e: Exception) = logFail("경고등", e)
    }

    // ── 수명 주기 ──────────────────────────────────────────

    override fun start() {
        if (scope != null) return
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = s

        vehicle.initialize()
        aaos.start()

        // 기어는 Pleos, 없으면 AAOS 표준값. 속도는 AAOS. 페달은 Pleos.
        s.launch {
            combine(pleosGear, aaos.gear, aaos.speedKmh, brakePressed) { pGear, aGear, speed, brake ->
                DrivingSample(
                    timestampMillis = System.currentTimeMillis(),
                    gear = pGear ?: aGear ?: Gear.UNKNOWN,
                    speedKmh = speed,
                    brakePedalPressed = brake,
                )
            }.collect { _driving.value = it }
        }

        // 서비스 연결은 비동기라, 실패하면 잠시 뒤 다시 등록합니다.
        s.launch { registerWithRetry("기어") { vehicle.getGear().registerGearSelection(gearListener) } }
        s.launch { registerWithRetry("브레이크 페달") { vehicle.getBrake().registerBrakePedalStatus(brakeListener) } }
        s.launch { registerWithRetry("운전석 문") { vehicle.getDoor().registerDoorPosition(driverDoor, doorListener) } }
        s.launch { registerWithRetry("배터리") { vehicle.getEvBattery().registerEvBatteryStateOfCharge(socListener) } }
        s.launch { registerWithRetry("연료") { vehicle.getFuel().registerFuelPercent(fuelListener) } }
        s.launch { registerWithRetry("주행 가능 거리") { vehicle.getOdometer().registerRangeRemainingListener(rangeListener) } }
        s.launch { registerWithRetry("누적 주행거리") { vehicle.getOdometer().registerDistanceDriven(odometerListener) } }
        s.launch { registerWithRetry("엔진오일") { vehicle.getSafety().registerEngineOilLevelState(engineOilListener) } }
        s.launch { registerWithRetry("브레이크액") { vehicle.getSafety().registerBrakeFluidLevelState(brakeFluidListener) } }
        s.launch { registerWithRetry("워셔액") { vehicle.getSafety().registerLowWasherFluidWaringState(washerListener) } }
        // 경고등은 권한이 없으면 한 번만 시도하고 '볼 수 없음'으로 표시합니다.
        vehicleCall {
            try {
                vehicle.getSafety().registerTelltales(telltaleListener)
            } catch (t: Throwable) {
                telltaleDenied = t.message.orEmpty().contains("Permission", ignoreCase = true)
                Log.w(TAG, "경고등 구독 불가: ${t.message}")
            }
        }

        // 처음 값: 기어
        vehicleCall { vehicle.getGear().getCurrentGearSelectionState({ pleosGear.value = mapGear(it) }, { e -> logFail("기어 조회", e) }) }
    }

    override fun stop() {
        vehicleCall {
            vehicle.getGear().unregisterGearSelection(gearListener)
            vehicle.getBrake().unregisterBrakePedalStatus(brakeListener)
            vehicle.getDoor().unregisterDoorPosition(driverDoor, doorListener)
            vehicle.getEvBattery().unregisterEvBatteryStateOfCharge(socListener)
            vehicle.getFuel().unregisterFuelPercent(fuelListener)
            vehicle.getOdometer().unregisterRangeRemainingListener(rangeListener)
            vehicle.getOdometer().unregisterDistanceDriven(odometerListener)
            vehicle.getSafety().unregisterEngineOilLevelState(engineOilListener)
            vehicle.getSafety().unregisterBrakeFluidLevelState(brakeFluidListener)
            vehicle.getSafety().unregisterLowWasherFluidWaringState(washerListener)
            if (!telltaleDenied) vehicle.getSafety().unregisterTelltales(telltaleListener)
        }
        aaos.stop()
        scope?.cancel()
        scope = null
        vehicleCall { vehicle.release() }
    }

    override fun onPermissionsChanged() {
        aaos.registerSpeedIfPermitted()
    }

    /**
     * 인수인계용 상태. 구독으로 받은 최신 값을 쓰고, 아직 없으면 한 번 직접 조회합니다.
     * 끝내 못 받은 항목은 숨기지 않고 [VehicleSnapshot.unavailable] 에 이유를 남깁니다.
     */
    override suspend fun readStatus(): VehicleSnapshot {
        val unavailable = mutableMapOf<Signal, SignalStatus>()

        val soc = socPercent ?: awaitGet<Int?> { ok, fail -> vehicle.getEvBattery().getEvBatteryStateOfCharge(ok, fail) }?.also { socPercent = it }
        val fuel = fuelPercent ?: if (soc == null) {
            awaitGet<Int?> { ok, fail -> vehicle.getFuel().getFuelPercent(ok, fail) }?.also { fuelPercent = it }
        } else {
            null
        }
        val range = rangeKm ?: awaitGet<Float?> { ok, fail -> vehicle.getOdometer().getRangeRemainingDistance(ok, fail) }?.let(::toKm)?.also { rangeKm = it }
        val odo = odometerKm ?: awaitGet<Float?> { ok, fail -> vehicle.getOdometer().getCurrentDistanceDriven(ok, fail) }?.toDouble()?.also { odometerKm = it }
        val lights = if (telltaleDenied) null else warnings
        val consumables = buildSet {
            if (engineOilLow == true) add(WarningLight.ENGINE_OIL)
            if (brakeFluidLow == true) add(WarningLight.BRAKE)
            if (washerFluidLow == true) add(WarningLight.WASHER_FLUID)
        }

        if (soc == null && fuel == null) unavailable[if (isEv()) Signal.EV_BATTERY else Signal.FUEL] = SignalStatus.FAILED
        if (range == null) unavailable[Signal.RANGE] = SignalStatus.FAILED
        if (odo == null) unavailable[Signal.ODOMETER] = SignalStatus.FAILED
        // 계기판 경고등을 못 보면 '경고등 없음'처럼 보이지 않게 이유를 남깁니다.
        if (lights == null) unavailable[Signal.WARNINGS] = if (telltaleDenied) SignalStatus.NO_PERMISSION else SignalStatus.FAILED

        return VehicleSnapshot(
            capturedAtMillis = System.currentTimeMillis(),
            // 전기차면 배터리만 씁니다(연료 값이 0으로 오는 경우가 있어 둘 다 있으면 배터리 우선).
            fuelPercent = if (soc != null) null else fuel,
            evBatteryPercent = soc,
            rangeKm = range,
            odometerKm = odo,
            activeWarnings = lights.orEmpty() + consumables,
            unavailable = unavailable,
        )
    }

    // ── 도움 함수 ──────────────────────────────────────────

    private fun isEv(): Boolean = socPercent != null

    private fun isLow(state: OilLevelState): Boolean = state == OilLevelState.LOW || state == OilLevelState.CRITICALLY_LOW

    private fun changed() { _status.tryEmit(Unit) }

    private suspend fun registerWithRetry(what: String, register: () -> Unit) {
        repeat(5) { attempt ->
            try {
                register()
                Log.i(TAG, "$what 구독 시작")
                return
            } catch (t: Throwable) {
                Log.w(TAG, "$what 구독 실패 (${attempt + 1}/5): ${t.message}")
                delay(1_000L * (attempt + 1))
            }
        }
        Log.w(TAG, "$what: 이 차량에서는 받지 않습니다")
    }

    private inline fun vehicleCall(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            Log.w(TAG, "Vehicle SDK 호출 실패", t)
        }
    }

    /** onComplete / onFailed 콜백을 suspend 로 바꿉니다. 실패·시간 초과면 null. */
    private suspend fun <T> awaitGet(
        timeoutMillis: Long = 2_000L,
        call: (onComplete: (T) -> Unit, onFailed: (Exception) -> Unit) -> Unit,
    ): T? = withTimeoutOrNull(timeoutMillis) {
        suspendCancellableCoroutine<T?> { cont ->
            try {
                call(
                    { value -> if (cont.isActive) cont.resume(value) },
                    { e ->
                        Log.w(TAG, "Vehicle API 조회 실패: ${e.message}")
                        if (cont.isActive) cont.resume(null)
                    },
                )
            } catch (t: Throwable) {
                Log.w(TAG, "Vehicle API 호출 실패: ${t.message}")
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    private fun logFail(what: String, e: Exception) {
        Log.w(TAG, "$what 신호 실패: ${e.message}")
    }

    private fun mapGear(g: VehicleGear?): Gear = when (g) {
        VehicleGear.GEAR_PARK -> Gear.PARK
        VehicleGear.GEAR_REVERSE -> Gear.REVERSE
        VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
        null, VehicleGear.GEAR_UNKNOWN -> Gear.UNKNOWN
        else -> Gear.DRIVE // D 및 수동 1~9단
    }

    /** 주행 가능 거리: VHAL 은 미터 단위. 큰 값이면 미터로 보고 km 로 바꿉니다. */
    private fun toKm(value: Float): Int = if (value > 3_000f) (value / 1000f).roundToInt() else value.roundToInt()

    /** Pleos 경고등(telltale) → 브리핑 경고등. 켜짐·깜빡임만 셉니다. */
    private fun mapTelltales(info: Telltales): Set<WarningLight> =
        info.telltales
            .filter { it.state == TelltaleState.ON || it.state == TelltaleState.BLINK }
            .mapNotNull { t ->
                when (t.type) {
                    TelltaleType.ENGINE, TelltaleType.ENGINE_TEMPERATURE -> WarningLight.ENGINE_CHECK
                    TelltaleType.ABS, TelltaleType.RBS -> WarningLight.BRAKE
                    TelltaleType.LOW_VOLT_BATTERY, TelltaleType.EV_SYSTEM -> WarningLight.BATTERY_CHARGE
                    TelltaleType.TPMS -> WarningLight.TIRE_PRESSURE
                    TelltaleType.ENGINE_OIL_PRESSURE -> WarningLight.ENGINE_OIL
                    else -> null
                }
            }
            .toSet()

    private companion object {
        const val TAG = "PleosVehicleGateway"
    }
}

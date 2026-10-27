package com.carrelay.vehicle.data

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/** 가상 차량의 현재 상태. '가상 차량' 패널에서 바꿉니다. */
data class SimulatedCar(
    val fuelPercent: Int = 62,
    val isEv: Boolean = false,
    val odometerKm: Double = 12_345.0,
    val warnings: Set<WarningLight> = emptySet(),
    val gear: Gear = Gear.PARK,
    val speedKmh: Double = 0.0,
    /** 목표 속도. 실제 속도는 현실적인 가감속으로 따라갑니다. */
    val targetSpeedKmh: Double = 0.0,
    /** true 면 급감속(약 7.5m/s²)으로 목표 속도까지 줄입니다. */
    val hardBraking: Boolean = false,
    val brakePressed: Boolean = true,
    val acceleratorPressed: Boolean = false,
    /** false 로 두면 '연료 미지원 차량'을 흉내 냅니다(미지원 처리 시연용). */
    val energySupported: Boolean = true,
) {
    /** 연료 100% = 600km 로 단순 계산 */
    val rangeKm: Int get() = fuelPercent * 6
}

/**
 * 가상 차량. SDK 없이도 앱의 모든 흐름(인수인계, 교대, 케어 모드)을 재현합니다.
 * 0.2초마다 속도를 목표값으로 움직이고, 주행거리와 연료를 갱신합니다.
 */
class FakeVehicleGateway(
    private val now: () -> Long = System::currentTimeMillis,
) : VehicleGateway {

    override val sourceName: String = "가상 차량"
    override val isSimulated: Boolean = true

    private val _car = MutableStateFlow(SimulatedCar())
    val car: StateFlow<SimulatedCar> = _car.asStateFlow()

    private val _driving = MutableStateFlow(sampleOf(_car.value))
    override val driving: StateFlow<DrivingSample> = _driving.asStateFlow()

    private val _door = MutableSharedFlow<DoorEvent>(extraBufferCapacity = 8)
    override val driverDoorEvents: Flow<DoorEvent> = _door.asSharedFlow()

    private var scope: CoroutineScope? = null
    private var fuelKmAccumulator = 0.0

    // ── 패널 조작 ───────────────────────────────────────────

    fun setGear(gear: Gear) = update { c ->
        if (gear.isDriveGear) {
            c.copy(gear = gear)
        } else {
            // 시뮬레이션 단순화: P·N 으로 바꾸면 바로 정지
            c.copy(gear = gear, speedKmh = 0.0, targetSpeedKmh = 0.0, hardBraking = false, acceleratorPressed = false)
        }
    }

    fun setTargetSpeed(kmh: Double) = update { c ->
        if (!c.gear.isDriveGear) c else c.copy(targetSpeedKmh = kmh.coerceIn(0.0, 180.0), hardBraking = false)
    }

    /** 현재 속도에서 40km/h 를 급하게 줄입니다. */
    fun hardBrake() = update { c ->
        if (!c.gear.isDriveGear) c else c.copy(targetSpeedKmh = (c.speedKmh - 40.0).coerceAtLeast(0.0), hardBraking = true)
    }

    /** 정차 중 페달을 한 번 떼었다 밟습니다(운전자 조작이 있었음을 흉내). */
    fun tapPedal() {
        update { it.copy(brakePressed = !it.brakePressed) }
        update { it.copy(brakePressed = !it.brakePressed) }
    }

    fun setFuel(percent: Int) = update { it.copy(fuelPercent = percent.coerceIn(0, 100)) }

    fun setEnergySupported(supported: Boolean) = update { it.copy(energySupported = supported) }

    fun toggleWarning(warning: WarningLight) = update { c ->
        c.copy(warnings = if (warning in c.warnings) c.warnings - warning else c.warnings + warning)
    }

    /** 운전석 문을 열었다 닫습니다(교대 감지 시연). */
    fun cycleDriverDoor() {
        _door.tryEmit(DoorEvent.OPENED)
        _door.tryEmit(DoorEvent.CLOSED)
    }

    private fun update(block: (SimulatedCar) -> SimulatedCar) {
        _car.update(block)
        _driving.value = sampleOf(_car.value)
    }

    // ── VehicleGateway ─────────────────────────────────────

    override suspend fun readStatus(): VehicleSnapshot {
        val c = _car.value
        val unavailable = buildMap {
            if (!c.energySupported) put(if (c.isEv) Signal.EV_BATTERY else Signal.FUEL, SignalStatus.UNSUPPORTED)
        }
        return VehicleSnapshot(
            capturedAtMillis = now(),
            fuelPercent = c.fuelPercent.takeIf { c.energySupported && !c.isEv },
            evBatteryPercent = c.fuelPercent.takeIf { c.energySupported && c.isEv },
            rangeKm = c.rangeKm.takeIf { c.energySupported },
            odometerKm = c.odometerKm,
            activeWarnings = c.warnings,
            unavailable = unavailable,
        )
    }

    override fun start() {
        if (scope != null) return
        // 화면 조작과 시뮬레이션이 같은(메인) 스레드에서 상태를 바꾸도록 해 경쟁 상태를 막습니다.
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = s
        s.launch { simulate() }
    }

    override fun stop() {
        scope?.cancel()
        scope = null
    }

    // ── 물리 시뮬레이션 ────────────────────────────────────

    private suspend fun simulate() {
        var last = now()
        while (true) {
            delay(TICK_MILLIS) // 취소되면 여기서 멈춥니다
            val t = now()
            val dtSec = (t - last).coerceIn(0L, 1_000L) / 1000.0
            last = t
            val before = _car.value
            val after = step(before, dtSec)
            _car.value = after
            if (after.speedKmh != before.speedKmh ||
                after.brakePressed != before.brakePressed ||
                after.acceleratorPressed != before.acceleratorPressed
            ) {
                _driving.value = sampleOf(after)
            }
        }
    }

    private fun step(c: SimulatedCar, dtSec: Double): SimulatedCar {
        if (!c.gear.isDriveGear) return c
        val diff = c.targetSpeedKmh - c.speedKmh
        val rate = when {
            diff > 0 -> ACCEL_KMH_PER_SEC
            c.hardBraking -> HARD_DECEL_KMH_PER_SEC
            else -> DECEL_KMH_PER_SEC
        }
        val delta = (rate * dtSec).coerceAtMost(abs(diff))
        val speed = c.speedKmh + sign(diff) * delta
        val reached = abs(c.targetSpeedKmh - speed) < 0.01

        val km = (c.speedKmh + speed) / 2.0 / 3600.0 * dtSec
        fuelKmAccumulator += km
        val usedPercent = (fuelKmAccumulator / KM_PER_PERCENT).toInt()
        fuelKmAccumulator -= usedPercent * KM_PER_PERCENT

        val stopped = speed < 1.0
        val brake = when {
            diff < 0 && !reached -> true // 감속 중
            diff > 0 -> false            // 가속 중
            stopped -> c.brakePressed    // D 로 서 있을 때는 그대로 유지
            else -> false                // 정속 주행
        }
        return c.copy(
            speedKmh = if (reached) c.targetSpeedKmh else speed,
            hardBraking = c.hardBraking && !reached,
            odometerKm = c.odometerKm + km,
            fuelPercent = (c.fuelPercent - usedPercent).coerceAtLeast(0),
            brakePressed = brake,
            acceleratorPressed = diff > 0 && !reached,
        )
    }

    private fun sampleOf(c: SimulatedCar) = DrivingSample(
        timestampMillis = now(),
        gear = c.gear,
        speedKmh = c.speedKmh,
        brakePedalPressed = c.brakePressed,
        acceleratorPressed = c.acceleratorPressed,
    )

    private companion object {
        const val TICK_MILLIS = 200L
        const val ACCEL_KMH_PER_SEC = 9.0       // 2.5 m/s²
        const val DECEL_KMH_PER_SEC = 9.0       // 2.5 m/s² (급감속 기준 3.5 m/s² 미만)
        const val HARD_DECEL_KMH_PER_SEC = 27.0 // 7.5 m/s²
        const val KM_PER_PERCENT = 6.0
    }
}

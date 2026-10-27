package com.carrelay.vehicle.data

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.carrelay.core.model.Gear
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * AAOS 표준 CarPropertyManager 로 기어(GEAR_SELECTION)와 속도(PERF_VEHICLE_SPEED)를 받습니다.
 * Pleos Connect 는 AAOS 기반이므로 신청 없이 쓸 수 있는 표준 API 입니다.
 *
 * Android Automotive 가 아닌 기기에서는 아무것도 하지 않습니다(값은 null 유지).
 */
class AaosCarSignals(private val context: Context) {

    private val _gear = MutableStateFlow<Gear?>(null)
    val gear: StateFlow<Gear?> = _gear.asStateFlow()

    private val _speedKmh = MutableStateFlow<Double?>(null)
    val speedKmh: StateFlow<Double?> = _speedKmh.asStateFlow()

    private var car: Car? = null
    private var propertyManager: CarPropertyManager? = null
    private var speedRegistered = false

    // Car API 가 없는 기기에서 클래스 로딩 오류가 나지 않도록 start() 에서 처음 쓸 때 만듭니다.
    private val callback by lazy { createCallback() }

    private fun createCallback() = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            when (value.propertyId) {
                VehiclePropertyIds.GEAR_SELECTION -> _gear.value = mapGear(value.value as? Int)
                // VHAL 속도 단위는 m/s
                VehiclePropertyIds.PERF_VEHICLE_SPEED ->
                    _speedKmh.value = (value.value as? Float)?.let { abs(it) * 3.6 }
            }
        }

        override fun onErrorEvent(propertyId: Int, zone: Int) {
            Log.w(TAG, "property error id=$propertyId zone=$zone")
        }
    }

    fun start(): Boolean {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) return false
        return try {
            val c = Car.createCar(context) ?: return false
            car = c
            val pm = c.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager ?: return false
            propertyManager = pm
            pm.registerCallback(callback, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE)
            registerSpeedIfPermitted()
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Car API 연결 실패", t)
            false
        }
    }

    /** 속도는 런타임 권한(CAR_SPEED)이 필요합니다. 허용 후 다시 호출하세요. */
    fun registerSpeedIfPermitted() {
        val pm = propertyManager ?: return
        if (speedRegistered) return
        if (context.checkSelfPermission(PERMISSION_SPEED) != PackageManager.PERMISSION_GRANTED) return
        try {
            pm.registerCallback(callback, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_NORMAL)
            speedRegistered = true
        } catch (t: Throwable) {
            Log.w(TAG, "속도 구독 실패", t)
        }
    }

    fun stop() {
        try {
            propertyManager?.unregisterCallback(callback)
            car?.disconnect()
        } catch (t: Throwable) {
            Log.w(TAG, "Car API 해제 실패", t)
        }
        propertyManager = null
        car = null
        speedRegistered = false
    }

    private fun mapGear(raw: Int?): Gear = when (raw) {
        // android.car.VehicleGear 상수값
        4 -> Gear.PARK
        2 -> Gear.REVERSE
        1 -> Gear.NEUTRAL
        null, 0 -> Gear.UNKNOWN
        else -> Gear.DRIVE // GEAR_DRIVE(8) 및 수동 1~9단
    }

    companion object {
        private const val TAG = "AaosCarSignals"
        const val PERMISSION_SPEED = "android.car.permission.CAR_SPEED"
    }
}

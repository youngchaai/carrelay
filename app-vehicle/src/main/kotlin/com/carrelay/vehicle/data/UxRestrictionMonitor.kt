package com.carrelay.vehicle.data

import android.car.Car
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AAOS 주행 규제(CarUxRestrictions) 구독.
 * requiresDistractionOptimization = true 이면 주행 중 화면(음성 + 큰 버튼)만 보여야 합니다.
 * AAOS 가 아니면 null 로 남고, 앱은 기어 상태로 대신 판단합니다.
 * https://document.pleos.ai (Driving Restriction Development Guide)
 */
class UxRestrictionMonitor(private val context: Context) {

    private val _requiresDistractionOptimization = MutableStateFlow<Boolean?>(null)
    val requiresDistractionOptimization: StateFlow<Boolean?> = _requiresDistractionOptimization.asStateFlow()

    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    fun start() {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) return
        try {
            val c = Car.createCar(context) ?: return
            car = c
            val m = c.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager ?: return
            manager = m
            _requiresDistractionOptimization.value = m.currentCarUxRestrictions?.isRequiresDistractionOptimization
            m.registerListener { restrictions ->
                _requiresDistractionOptimization.value = restrictions.isRequiresDistractionOptimization
            }
        } catch (t: Throwable) {
            Log.w(TAG, "CarUxRestrictions 연결 실패", t)
        }
    }

    fun stop() {
        try {
            manager?.unregisterListener()
            car?.disconnect()
        } catch (t: Throwable) {
            Log.w(TAG, "CarUxRestrictions 해제 실패", t)
        }
        manager = null
        car = null
    }

    private companion object {
        const val TAG = "UxRestrictionMonitor"
    }
}

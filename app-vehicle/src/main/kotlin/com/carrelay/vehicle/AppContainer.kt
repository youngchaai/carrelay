package com.carrelay.vehicle

import android.content.Context
import com.carrelay.vehicle.data.LocalStore
import com.carrelay.vehicle.data.UxRestrictionMonitor
import com.carrelay.vehicle.data.VehicleGateway
import com.carrelay.vehicle.data.VehicleGatewayProvider
import com.carrelay.vehicle.feature.GuardianNotifier
import com.carrelay.vehicle.feature.LocalGuardianNotifier
import com.carrelay.vehicle.feature.RestStopNavigator
import com.carrelay.vehicle.feature.SimulatedRestStopNavigator
import com.carrelay.vehicle.feature.Speaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** 앱 전역 객체 모음(간단한 수동 DI). */
class AppContainer(context: Context) {
    /** 화면이 닫혀도 끝까지 마쳐야 하는 작업(저장 등)용. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val gateway: VehicleGateway = VehicleGatewayProvider.create(context)
    val store = LocalStore(context)
    val speaker = Speaker(context)
    val uxMonitor = UxRestrictionMonitor(context)
    val navigator: RestStopNavigator = SimulatedRestStopNavigator()
    val guardianNotifier: GuardianNotifier = LocalGuardianNotifier()
}

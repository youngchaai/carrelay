package com.carrelay.vehicle.data

import android.content.Context

/** carrelay.pleosSdk=false (기본): 가상 차량으로 실행합니다. */
object VehicleGatewayProvider {
    @Suppress("UNUSED_PARAMETER")
    fun create(context: Context): VehicleGateway = FakeVehicleGateway()
}

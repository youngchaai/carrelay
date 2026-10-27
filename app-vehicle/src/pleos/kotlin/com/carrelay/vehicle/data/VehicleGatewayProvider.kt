package com.carrelay.vehicle.data

import android.content.Context

/** carrelay.pleosSdk=true: Pleos Vehicle SDK 로 실제(에뮬레이터 VHAL) 차량 데이터를 받습니다. */
object VehicleGatewayProvider {
    fun create(context: Context): VehicleGateway = PleosVehicleGateway(context.applicationContext)
}

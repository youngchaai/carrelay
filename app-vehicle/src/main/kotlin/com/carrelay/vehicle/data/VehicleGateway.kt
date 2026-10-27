package com.carrelay.vehicle.data

import com.carrelay.core.model.DoorEvent
import com.carrelay.core.model.DrivingSample
import com.carrelay.core.model.VehicleSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 앱이 차량 데이터를 받는 유일한 통로.
 *
 * 구현체
 * - [FakeVehicleGateway] : 가상 차량. 화면 오른쪽 '가상 차량' 패널로 값을 바꿉니다. (기본값)
 * - PleosVehicleGateway  : Pleos Vehicle SDK + AAOS Car API. (gradle.properties 에서 carrelay.pleosSdk=true)
 *
 * 어떤 구현을 쓸지는 빌드 옵션에 따라 VehicleGatewayProvider 가 정합니다.
 */
interface VehicleGateway {
    /** 화면에 표시할 데이터 출처 이름. */
    val sourceName: String

    /** 가상 차량이면 true (시연 화면에서 실제 차량 데이터와 구분해 표시). */
    val isSimulated: Boolean

    /** 주행 판단용 실시간 신호(기어·속도·페달). 값이 바뀔 때마다 갱신됩니다. */
    val driving: StateFlow<DrivingSample>

    /** 운전석 문 열림/닫힘. 주차 중 교대 감지에 씁니다. */
    val driverDoorEvents: Flow<DoorEvent>

    /**
     * 인수인계용 차량 상태를 읽습니다.
     * 지원하지 않거나 읽지 못한 항목은 값 대신 [VehicleSnapshot.unavailable] 에 이유를 넣습니다.
     */
    suspend fun readStatus(): VehicleSnapshot

    fun start()
    fun stop()

    /** 런타임 권한(예: 속도) 허용 결과가 바뀌면 호출합니다. */
    fun onPermissionsChanged() {}
}

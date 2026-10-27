package com.carrelay.vehicle.feature

import android.util.Log
import com.carrelay.vehicle.data.GuardianAlert

/**
 * 휴게소·졸음쉼터 경유지 추가.
 * TODO(확장): Pleos NaviHelper SDK 의 경유지 추가 API 와 onWayPointArrived 로 교체합니다.
 */
interface RestStopNavigator {
    /** 결과를 화면·음성으로 알려 줄 문장을 돌려줍니다. */
    suspend fun addNearestRestStop(): String

    /**
     * 보호자 알림에 넣을 현재 위치 설명. 위치는 케어 모드 3단계에서만 사용합니다.
     * TODO(확장): Pleos FusedLocation SDK 로 현재 위치를 받아 주소/좌표로 바꿉니다.
     */
    suspend fun currentLocationText(): String
}

class SimulatedRestStopNavigator : RestStopNavigator {
    override suspend fun addNearestRestStop(): String = "가까운 졸음쉼터를 경유지로 추가했어요 (시뮬레이션)"
    override suspend fun currentLocationText(): String = "위치 정보(시뮬레이션)"
}

/**
 * 보호자 폰 알림.
 * TODO(확장): 동기화 서버 → 보호자 폰 푸시. Pleos Connect 는 Google 서비스가 없는 AAOS 이므로
 * 차량 앱은 자체 서버 API 를 호출하고, 서버가 보호자 폰으로 푸시를 보냅니다.
 */
interface GuardianNotifier {
    suspend fun send(alert: GuardianAlert)
}

class LocalGuardianNotifier : GuardianNotifier {
    override suspend fun send(alert: GuardianAlert) {
        Log.i("GuardianNotifier", "보호자 알림(서버 연동 전, 로컬 기록만): ${alert.message}")
    }
}

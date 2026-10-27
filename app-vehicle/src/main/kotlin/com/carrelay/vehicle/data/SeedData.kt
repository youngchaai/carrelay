package com.carrelay.vehicle.data

import com.carrelay.core.model.Driver
import com.carrelay.core.model.DriverCondition

/**
 * 처음 실행할 때 쓰는 시연용 파티. (참가신청서 시나리오 A·B)
 * P0 에서는 차량 안의 로컬 프로필로 시작하고, 이후 폰 앱의 파티 생성·초대로 바꿉니다.
 * 컨디션 값은 워치 실데이터 대신 쓰는 샘플입니다.
 */
object SeedData {

    fun initial(): AppData {
        val now = System.currentTimeMillis()
        return AppData(
            drivers = listOf(
                Driver(id = "dad", name = "아빠"),
                Driver(id = "mom", name = "엄마"),
                Driver(id = "daughter", name = "딸"),
                // 시나리오 B: 본인 동의로 케어 모드를 켠 고령 가족(샘플)
                Driver(id = "grandpa", name = "할아버지", careModeEnabled = true, guardianName = "딸"),
            ),
            conditions = mapOf(
                "dad" to DriverCondition("dad", sleepHours = 4.0, updatedAtMillis = now),
                "mom" to DriverCondition("mom", sleepHours = 7.0, updatedAtMillis = now),
                "daughter" to DriverCondition("daughter", sleepHours = 6.0, updatedAtMillis = now),
                "grandpa" to DriverCondition("grandpa", sleepHours = 5.0, restingHeartRateDelta = 3, updatedAtMillis = now),
            ),
        )
    }
}

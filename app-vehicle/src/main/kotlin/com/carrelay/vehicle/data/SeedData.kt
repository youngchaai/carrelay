package com.carrelay.vehicle.data

import com.carrelay.core.model.Driver
import com.carrelay.core.model.DriverCondition
import com.carrelay.core.model.LicenseClass
import java.time.LocalDate

/**
 * 처음 실행할 때 쓰는 시연용 파티. (참가신청서 시나리오 A·B)
 * P0 에서는 차량 안의 로컬 프로필로 시작하고, 이후 폰 앱의 파티 생성·초대로 바꿉니다.
 * 컨디션·면허 값은 시연용 샘플입니다. 면허 번호는 저장하지 않습니다(고유식별정보).
 */
object SeedData {

    fun initial(): AppData {
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        val year = today.year
        return AppData(
            drivers = listOf(
                Driver(
                    id = "dad", name = "아빠",
                    licenseClass = LicenseClass.CLASS1_NORMAL,
                    licenseExpiryEpochDay = today.plusYears(5).toEpochDay(), licensedSinceYear = 2005,
                ),
                Driver(
                    id = "mom", name = "엄마",
                    licenseClass = LicenseClass.CLASS2_NORMAL,
                    licenseExpiryEpochDay = today.plusYears(3).toEpochDay(), licensedSinceYear = 2010,
                ),
                Driver(
                    id = "daughter", name = "딸",
                    licenseClass = LicenseClass.CLASS2_NORMAL,
                    licenseExpiryEpochDay = today.plusYears(9).toEpochDay(), licensedSinceYear = year - 1, // 초보 운전
                ),
                // 시나리오 B: 본인 동의로 케어 모드를 켠 고령 가족(샘플). 면허 갱신(적성검사) 임박. 큰 글씨 모드 사용.
                Driver(
                    id = "grandpa", name = "할아버지", careModeEnabled = true, guardianName = "딸",
                    licenseClass = LicenseClass.CLASS1_NORMAL,
                    licenseExpiryEpochDay = today.plusDays(20).toEpochDay(), licensedSinceYear = 1978,
                    largeText = true,
                ),
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

package com.carrelay.vehicle.data

import com.carrelay.core.memo.MemoBoard
import com.carrelay.core.model.CarUsage
import com.carrelay.core.model.DriveSession
import com.carrelay.core.model.Driver
import com.carrelay.core.model.DriverCondition
import com.carrelay.core.model.LicenseClass
import com.carrelay.core.model.TripPurpose
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * 처음 실행할 때 쓰는 시연용 파티. (참가신청서 시나리오 A·B)
 * P0 에서는 차량 안의 로컬 프로필로 시작하고, 이후 폰 앱의 파티 생성·초대로 바꿉니다.
 * 컨디션·면허 값은 시연용 샘플입니다. 면허 번호는 저장하지 않습니다(고유식별정보).
 */
object SeedData {

    fun forUsage(usage: CarUsage, odometerNowKm: Double? = null): AppData = when (usage) {
        CarUsage.FAMILY -> initial()
        CarUsage.COMPANY -> company(odometerNowKm)
    }

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

    /**
     * 회사 차(소규모 사업장 공용차) 시연용 파티. 이름은 가상의 직함 호칭입니다.
     * 지난 이틀의 운행 4건을 지금 계기판 값에 이어지게 미리 넣어 운행기록부가 비어 보이지 않게 합니다.
     * 계기판 값을 모르거나 그보다 작으면(새 가상 차량) 지난 운행은 넣지 않습니다. 음수 거리를 만들지 않습니다.
     * 컨디션(워치 수면) 정보와 케어 모드는 회사 차에서 쓰지 않습니다.
     */
    fun company(odometerNowKm: Double?): AppData {
        val today = LocalDate.now()
        val year = today.year
        val zone = ZoneId.systemDefault()
        fun at(daysAgo: Long, hour: Int, minute: Int) =
            today.minusDays(daysAgo).atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()

        // (운전자, 며칠 전, 시, 분, 거리 km, 목적) — 오래된 순
        val sample = listOf(
            Trip("kim", 2, 8, 10, 18.4, TripPurpose.COMMUTE),
            Trip("lee", 2, 13, 20, 42.7, TripPurpose.BUSINESS),
            Trip("park", 1, 10, 5, 27.3, TripPurpose.BUSINESS),
            Trip("park", 1, 19, 30, 6.1, TripPurpose.PERSONAL),
        )
        val trips = if (odometerNowKm != null && odometerNowKm >= sample.sumOf { it.km } + 1.0) sample else emptyList()
        var odo = (odometerNowKm ?: 0.0) - trips.sumOf { it.km }
        val sessions = trips.mapIndexed { i, t ->
            val start = at(t.daysAgo, t.hour, t.minute)
            val minutes = (t.km / 35.0 * 60).toLong().coerceAtLeast(10)
            val from = odo
            odo += t.km
            DriveSession(
                id = "seed-trip-$i", driverId = t.driverId, confirmed = true,
                startedAtMillis = start, startOdometerKm = round1(from),
                endedAtMillis = start + minutes * 60_000L, endOdometerKm = round1(odo),
                purpose = t.purpose,
            )
        }
        return AppData(
            usage = CarUsage.COMPANY,
            vehicleName = "영업팀 업무 1호차",
            drivers = listOf(
                Driver(
                    id = "kim", name = "김 과장", department = "영업1팀",
                    licenseClass = LicenseClass.CLASS1_NORMAL,
                    licenseExpiryEpochDay = today.plusYears(4).toEpochDay(), licensedSinceYear = 2008,
                ),
                Driver(
                    id = "lee", name = "이 대리", department = "영업1팀",
                    licenseClass = LicenseClass.CLASS2_NORMAL,
                    licenseExpiryEpochDay = today.plusYears(6).toEpochDay(), licensedSinceYear = 2016,
                ),
                Driver(
                    id = "park", name = "박 사원", department = "영업2팀",
                    licenseClass = LicenseClass.CLASS2_NORMAL,
                    licenseExpiryEpochDay = today.plusYears(9).toEpochDay(), licensedSinceYear = year - 1, // 초보 운전
                ),
                Driver(
                    id = "choi", name = "최 팀장", department = "총무팀",
                    licenseClass = LicenseClass.CLASS1_NORMAL,
                    licenseExpiryEpochDay = today.plusDays(25).toEpochDay(), licensedSinceYear = 1998, // 면허 갱신 임박
                ),
            ),
            sessions = sessions,
            lastDriverId = trips.lastOrNull()?.driverId,
            // 하이패스 카드는 총무팀이 충전하므로 최 팀장에게 남긴 메모(모두에게 보이지만 받는 사람 표시)
            memos = MemoBoard.add(
                emptyList(), "seed-memo-hipass", "lee", at(2, 14, 30),
                text = "하이패스 카드 잔액이 얼마 안 남았어요", toDriverId = "choi",
            ),
        )
    }

    private data class Trip(val driverId: String, val daysAgo: Long, val hour: Int, val minute: Int, val km: Double, val purpose: TripPurpose)

    private fun round1(v: Double) = Math.round(v * 10) / 10.0
}

package com.carrelay.core.triplog

import com.carrelay.core.model.DriveSession
import com.carrelay.core.model.Driver
import com.carrelay.core.model.TripPurpose

/**
 * 업무용승용차 운행기록부 한 줄 (국세청 고시 서식의 칸에 맞춤).
 *
 * 사용자 부서·성명은 운전자 선택에서, 주행 전·후 계기판 누적거리는 Pleos Vehicle SDK 의 누적 주행거리에서,
 * 업무용 사용거리(출퇴근용·일반 업무용)는 출발 전에 고른 운행 목적에서 채웁니다. 손으로 적을 칸이 없습니다.
 */
data class TripLogRow(
    val sessionId: String,
    val startedAtMillis: Long,
    val department: String?,
    val driverName: String,
    val startOdometerKm: Double?,
    val endOdometerKm: Double?,
    val distanceKm: Double?,
    val purpose: TripPurpose?,
    val commuteKm: Double,
    val businessKm: Double,
    val inProgress: Boolean,
    /** 확인이 필요한 점: 운전자 미확인, 목적 미입력, 계기판 값 없음 */
    val notes: List<String>,
)

data class TripLogSummary(
    val trips: Int,
    val totalKm: Double,
    val commuteKm: Double,
    val businessKm: Double,
    val personalKm: Double,
    /** 목적을 고르지 않은 운행 거리 (업무용으로 치지 않음) */
    val unknownKm: Double,
) {
    /** 업무용 사용거리 = 출퇴근용 + 일반 업무용 */
    val businessUseKm: Double get() = commuteKm + businessKm

    /** 업무사용비율 (총 주행거리가 0 이면 null) */
    val businessRatio: Double? get() = if (totalKm > 0.0) businessUseKm / totalKm else null
}

object TripLog {

    const val NOTE_UNCONFIRMED = "운전자 미확인"
    const val NOTE_NO_PURPOSE = "목적 미입력"
    const val NOTE_NO_ODOMETER = "계기판 값 없음"

    /**
     * 끝난 운전 구간과 진행 중인 구간으로 운행기록부를 만듭니다(오래된 순).
     * 진행 중인 구간은 지금 계기판 값을 '주행 후'로 씁니다.
     */
    fun rows(
        ended: List<DriveSession>,
        active: DriveSession?,
        currentOdometerKm: Double?,
        driverOf: (String) -> Driver?,
    ): List<TripLogRow> {
        val all = ended.map { it to false } + listOfNotNull(active?.let { it.copy(endOdometerKm = currentOdometerKm) to true })
        return all.sortedBy { it.first.startedAtMillis }.map { (s, inProgress) -> row(s, inProgress, driverOf(s.driverId)) }
    }

    fun summary(rows: List<TripLogRow>): TripLogSummary {
        var total = 0.0
        var commute = 0.0
        var business = 0.0
        var personal = 0.0
        var unknown = 0.0
        for (r in rows) {
            val d = r.distanceKm ?: continue
            total += d
            when (r.purpose) {
                TripPurpose.COMMUTE -> commute += d
                TripPurpose.BUSINESS -> business += d
                TripPurpose.PERSONAL -> personal += d
                null -> unknown += d
            }
        }
        return TripLogSummary(rows.size, total, commute, business, personal, unknown)
    }

    private fun row(s: DriveSession, inProgress: Boolean, driver: Driver?): TripLogRow {
        val distance = s.distanceKm
        val notes = buildList {
            if (!s.confirmed) add(NOTE_UNCONFIRMED)
            if (s.purpose == null) add(NOTE_NO_PURPOSE)
            if (distance == null) add(NOTE_NO_ODOMETER)
        }
        return TripLogRow(
            sessionId = s.id,
            startedAtMillis = s.startedAtMillis,
            department = driver?.department,
            driverName = driver?.name ?: "알 수 없음",
            startOdometerKm = s.startOdometerKm,
            endOdometerKm = s.endOdometerKm,
            distanceKm = distance,
            purpose = s.purpose,
            commuteKm = if (s.purpose == TripPurpose.COMMUTE) distance ?: 0.0 else 0.0,
            businessKm = if (s.purpose == TripPurpose.BUSINESS) distance ?: 0.0 else 0.0,
            inProgress = inProgress,
            notes = notes,
        )
    }
}

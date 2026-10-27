package com.carrelay.core.care

import com.carrelay.core.model.DriveSession
import com.carrelay.core.text.KoreanText

/**
 * 보호자에게 보내는 하루 요약. 실시간 위치 대신 운전 시간·야간 운전·급감속·안부 확인 기록만 담습니다.
 */
data class CareDailySummary(
    val driverName: String,
    val drivingMillis: Long,
    val nightDriving: Boolean,
    val harshDecelCount: Int,
    val checkInsAsked: Int,
    val checkInsAnswered: Int,
    val guardianAlerts: Int,
) {
    val message: String
        get() = buildString {
            append("${driverName}님 운전 ${KoreanText.duration(drivingMillis)}")
            append(", 급감속 ${harshDecelCount}회")
            append(", 야간 운전 ${if (nightDriving) "있음" else "없음"}")
            append(", 안부 확인 ${checkInsAsked}회")
            if (checkInsAsked > 0) {
                append(if (checkInsAnswered >= checkInsAsked) "(응답함)" else "(응답 ${checkInsAnswered}회)")
            }
            if (guardianAlerts > 0) append(", 보호자 알림 ${guardianAlerts}회")
        }

    companion object {
        /** 22시~05시 사이에 시작하거나 끝난 운전을 야간 운전으로 봅니다. */
        private val NIGHT_HOURS = setOf(22, 23, 0, 1, 2, 3, 4)

        fun from(
            driverId: String,
            driverName: String,
            sessions: List<DriveSession>,
            events: List<CareEvent>,
            nowMillis: Long,
            hourOf: (Long) -> Int,
        ): CareDailySummary {
            val mine = sessions.filter { it.driverId == driverId }
            val myEvents = events.filter { it.driverId == driverId }
            val driving = mine.sumOf { ((it.endedAtMillis ?: nowMillis) - it.startedAtMillis).coerceAtLeast(0L) }
            val night = mine.any { s ->
                hourOf(s.startedAtMillis) in NIGHT_HOURS || hourOf(s.endedAtMillis ?: nowMillis) in NIGHT_HOURS
            }
            return CareDailySummary(
                driverName = driverName,
                drivingMillis = driving,
                nightDriving = night,
                harshDecelCount = myEvents.count { it.type == CareEvent.Type.HARSH_DECEL },
                checkInsAsked = myEvents.count { it.type == CareEvent.Type.CHECK_IN_ASKED },
                checkInsAnswered = myEvents.count { it.type == CareEvent.Type.CHECK_IN_ANSWERED },
                guardianAlerts = myEvents.count { it.type == CareEvent.Type.GUARDIAN_NOTIFIED },
            )
        }
    }
}

package com.carrelay.core.incident

import com.carrelay.core.model.CarZone
import com.carrelay.core.model.DriveSession
import com.carrelay.core.model.Incident
import com.carrelay.core.model.IncidentSource
import com.carrelay.core.model.IncidentStatus
import com.carrelay.core.model.IncidentType

/** 운전자별 사고·신고 집계 */
data class IncidentStats(
    /** 본인이 운행 후 신고한 건수 */
    val selfReported: Int,
    /** 본인 운전 직후 다음 운전자가 발견한 건수 (확인 필요) */
    val foundAfterMyDrive: Int,
    /** 그중 아직 처리 안 된 건수 */
    val open: Int,
)

/** 사고·파손 기록을 다루는 순수 함수 모음 */
object IncidentLog {

    fun report(
        incidents: List<Incident>,
        id: String,
        reporterId: String,
        type: IncidentType,
        zones: Set<CarZone>,
        source: IncidentSource,
        nowMillis: Long,
        previousSession: DriveSession? = null,
    ): List<Incident> = incidents + Incident(
        id = id,
        reporterId = reporterId,
        type = type,
        zones = zones,
        source = source,
        createdAtMillis = nowMillis,
        // 인수 점검에서 발견했고 직전 운전자가 다른 사람이면, 확인 대상을 남깁니다.
        previousDriverId = previousSession?.driverId?.takeIf { source == IncidentSource.AT_PICKUP && it != reporterId },
        previousParkedAtMillis = previousSession?.endedAtMillis?.takeIf { source == IncidentSource.AT_PICKUP },
    )

    fun updateStatus(
        incidents: List<Incident>,
        id: String,
        status: IncidentStatus,
        note: String,
        byDriverId: String,
        nowMillis: Long,
    ): List<Incident> = incidents.map {
        if (it.id == id) it.copy(status = status, statusNote = note, statusUpdatedAtMillis = nowMillis, statusUpdatedBy = byDriverId) else it
    }

    /** 처리 안 된 기록, 최신순 */
    fun open(incidents: List<Incident>): List<Incident> =
        incidents.filter { it.isOpen }.sortedByDescending { it.createdAtMillis }

    fun statsFor(incidents: List<Incident>, driverId: String): IncidentStats {
        val self = incidents.filter { it.reporterId == driverId && it.source == IncidentSource.AFTER_DRIVE }
        val after = incidents.filter { it.previousDriverId == driverId }
        return IncidentStats(
            selfReported = self.size,
            foundAfterMyDrive = after.size,
            open = (self + after).distinctBy { it.id }.count { it.isOpen },
        )
    }
}

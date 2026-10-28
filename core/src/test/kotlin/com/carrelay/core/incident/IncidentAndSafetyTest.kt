package com.carrelay.core.incident

import com.carrelay.core.driving.DrivingEvent
import com.carrelay.core.driving.DrivingEventDetector
import com.carrelay.core.handover.BriefingItem
import com.carrelay.core.handover.HandoverBriefingBuilder
import com.carrelay.core.model.CarZone
import com.carrelay.core.model.DriveSession
import com.carrelay.core.model.Driver
import com.carrelay.core.model.DrivingSample
import com.carrelay.core.model.Gear
import com.carrelay.core.model.IncidentSource
import com.carrelay.core.model.IncidentStatus
import com.carrelay.core.model.IncidentType
import com.carrelay.core.model.LicenseClass
import com.carrelay.core.model.VehicleSnapshot
import com.carrelay.core.profile.LicenseCheck
import com.carrelay.core.profile.LicenseState
import com.carrelay.core.session.DriverSessionTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IncidentAndSafetyTest {

    private val prev = DriveSession("s1", "daughter", true, startedAtMillis = 0, endedAtMillis = 5_000)

    @Test
    fun `인수 점검에서 발견하면 직전 운전자와 주차 시각을 남긴다`() {
        val list = IncidentLog.report(
            emptyList(), "i1", reporterId = "mom", type = IncidentType.SCRATCH,
            zones = setOf(CarZone.FRONT, CarZone.LEFT), source = IncidentSource.AT_PICKUP, nowMillis = 9_000, previousSession = prev,
        )
        val inc = list.single()
        assertEquals("daughter", inc.previousDriverId)
        assertEquals(5_000L, inc.previousParkedAtMillis)
        assertEquals("앞·왼쪽 긁힘", inc.summary)
        assertTrue(inc.isOpen)
    }

    @Test
    fun `스스로 신고한 건은 직전 운전자를 남기지 않는다`() {
        val inc = IncidentLog.report(
            emptyList(), "i1", "daughter", IncidentType.COLLISION, setOf(CarZone.REAR),
            IncidentSource.AFTER_DRIVE, 9_000, previousSession = prev,
        ).single()
        assertNull(inc.previousDriverId)
    }

    @Test
    fun `처리 완료 전까지 열린 기록으로 남고 운전자별로 집계된다`() {
        var list = IncidentLog.report(emptyList(), "i1", "mom", IncidentType.DENT, setOf(CarZone.RIGHT), IncidentSource.AT_PICKUP, 1, prev)
        list = IncidentLog.report(list, "i2", "daughter", IncidentType.COLLISION, setOf(CarZone.REAR), IncidentSource.AFTER_DRIVE, 2)

        val daughter = IncidentLog.statsFor(list, "daughter")
        assertEquals(1, daughter.selfReported)
        assertEquals(1, daughter.foundAfterMyDrive)
        assertEquals(2, daughter.open)

        list = IncidentLog.updateStatus(list, "i1", IncidentStatus.RESOLVED, "수리 완료", "dad", 3)
        assertEquals(listOf("i2"), IncidentLog.open(list).map { it.id })
        assertEquals(1, IncidentLog.statsFor(list, "daughter").open)
    }

    @Test
    fun `처리 안 된 사고는 브리핑 맨 위에 나온다`() {
        val list = IncidentLog.report(emptyList(), "i1", "daughter", IncidentType.SCRATCH, setOf(CarZone.FRONT), IncidentSource.AFTER_DRIVE, 1)
        val b = HandoverBriefingBuilder.build(
            Driver("mom", "엄마"), VehicleSnapshot(10, fuelPercent = 10), null, emptyList(),
            { if (it == "daughter") "딸" else it }, 10, incidents = list,
        )
        val first = b.items.first()
        assertEquals(BriefingItem.Kind.OPEN_INCIDENT, first.kind)
        assertEquals("처리 필요: 앞 긁힘 (딸 신고)", first.message)
        assertEquals("i1", first.incidentId)
    }

    @Test
    fun `면허 갱신 기한과 초보 여부를 판단한다`() {
        val today = 20_000L
        val ok = LicenseCheck.of(Driver("a", "아빠", licenseClass = LicenseClass.CLASS1_NORMAL, licenseExpiryEpochDay = today + 400, licensedSinceYear = 2005), today, 2026)
        assertEquals(LicenseState.OK, ok.state)
        assertFalse(ok.isNovice)

        val soon = LicenseCheck.of(Driver("g", "할아버지", licenseClass = LicenseClass.CLASS1_NORMAL, licenseExpiryEpochDay = today + 20), today, 2026)
        assertEquals(LicenseState.RENEW_SOON, soon.state)
        assertEquals("갱신 D-20", soon.renewalLabel)

        val expired = LicenseCheck.of(Driver("x", "x", licenseClass = LicenseClass.CLASS2_NORMAL, licenseExpiryEpochDay = today - 3), today, 2026)
        assertEquals(LicenseState.EXPIRED, expired.state)
        assertEquals("갱신 기한 3일 지남", expired.renewalLabel)

        val novice = LicenseCheck.of(Driver("d", "딸", licenseClass = LicenseClass.CLASS2_NORMAL, licensedSinceYear = 2025), today, 2026)
        assertTrue(novice.isNovice)

        assertEquals(LicenseState.UNKNOWN, LicenseCheck.of(Driver("n", "n"), today, 2026).state)
    }

    @Test
    fun `급정거와 급가속을 따로 센다`() {
        val d = DrivingEventDetector()
        assertNull(d.onSample(DrivingSample(0, Gear.DRIVE, 60.0)))
        assertEquals(DrivingEvent.HARSH_DECEL, d.onSample(DrivingSample(1_000, Gear.DRIVE, 40.0))) // 5.6 m/s²
        assertNull(d.onSample(DrivingSample(2_000, Gear.DRIVE, 32.0))) // 2.2 m/s² 보통 감속
        assertEquals(DrivingEvent.HARSH_ACCEL, d.onSample(DrivingSample(3_000, Gear.DRIVE, 45.0))) // 3.6 m/s²
    }

    @Test
    fun `몇 ms 차이로 붙어 들어온 샘플 때문에 보통 가속을 급가속으로 세지 않는다`() {
        val d = DrivingEventDetector()
        assertNull(d.onSample(DrivingSample(0, Gear.DRIVE, 0.0)))
        assertNull(d.onSample(DrivingSample(20, Gear.DRIVE, 1.8))) // 두 점만 보면 25m/s² 처럼 보임
        var t = 20L
        var v = 1.8
        repeat(10) {
            t += 200
            v += 1.8 // 2.5 m/s² 보통 가속
            assertNull(d.onSample(DrivingSample(t, Gear.DRIVE, v)))
        }
    }

    @Test
    fun `200ms 간격으로 이어지는 급정거는 한 번으로 센다`() {
        val d = DrivingEventDetector()
        var t = 0L
        var v = 60.0
        var events = 0
        repeat(10) {
            if (d.onSample(DrivingSample(t, Gear.DRIVE, v)) == DrivingEvent.HARSH_DECEL) events++
            t += 200
            v -= 5.4 // 7.5 m/s²
        }
        assertEquals(1, events)
    }

    @Test
    fun `진행 중인 운전 구간에 급정거 횟수가 쌓이고 끝날 때 함께 기록된다`() {
        val t = DriverSessionTracker(null) { "s" }
        t.onGear(Gear.PARK, 0, null)
        t.selectDriver("mom", 1, null)
        t.addDrivingEvent(harshDecel = 1)
        t.addDrivingEvent(harshDecel = 1, harshAccel = 1)
        val ended = (t.endSession(10, null).single() as com.carrelay.core.session.SessionEffect.SessionEnded).session
        assertEquals(2, ended.harshDecelCount)
        assertEquals(1, ended.harshAccelCount)
    }
}

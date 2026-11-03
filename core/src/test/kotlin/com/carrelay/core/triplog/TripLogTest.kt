package com.carrelay.core.triplog

import com.carrelay.core.memo.VoiceMemoParser
import com.carrelay.core.model.DriveSession
import com.carrelay.core.model.Driver
import com.carrelay.core.model.Gear
import com.carrelay.core.model.TripPurpose
import com.carrelay.core.session.DriverSessionTracker
import com.carrelay.core.session.SessionEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripLogTest {

    private val drivers = listOf(
        Driver("kim", "김 과장", department = "영업1팀"),
        Driver("lee", "이 대리", department = "영업1팀"),
    ).associateBy { it.id }

    private fun session(
        id: String, driver: String, start: Long, from: Double?, to: Double?,
        purpose: TripPurpose?, confirmed: Boolean = true,
    ) = DriveSession(
        id = id, driverId = driver, confirmed = confirmed, startedAtMillis = start,
        startOdometerKm = from, endedAtMillis = start + 1, endOdometerKm = to, purpose = purpose,
    )

    @Test
    fun `운행기록부 칸을 부서·이름·계기판 거리·목적으로 채운다`() {
        val rows = TripLog.rows(
            ended = listOf(session("a", "kim", 100, 12000.0, 12018.4, TripPurpose.COMMUTE)),
            active = null, currentOdometerKm = null, driverOf = drivers::get,
        )

        val r = rows.single()
        assertEquals("영업1팀", r.department)
        assertEquals("김 과장", r.driverName)
        assertEquals(12000.0, r.startOdometerKm!!, 0.001)
        assertEquals(12018.4, r.endOdometerKm!!, 0.001)
        assertEquals(18.4, r.distanceKm!!, 0.001)
        assertEquals(18.4, r.commuteKm, 0.001)
        assertEquals(0.0, r.businessKm, 0.001)
        assertTrue(r.notes.isEmpty())
    }

    @Test
    fun `업무사용비율은 출퇴근과 일반 업무 거리를 총 주행거리로 나눈 값이다`() {
        val rows = TripLog.rows(
            ended = listOf(
                session("a", "kim", 100, 0.0, 10.0, TripPurpose.COMMUTE),
                session("b", "lee", 200, 10.0, 30.0, TripPurpose.BUSINESS),
                session("c", "lee", 300, 30.0, 40.0, TripPurpose.PERSONAL),
            ),
            active = null, currentOdometerKm = null, driverOf = drivers::get,
        )

        val sum = TripLog.summary(rows)
        assertEquals(40.0, sum.totalKm, 0.001)
        assertEquals(30.0, sum.businessUseKm, 0.001)
        assertEquals(10.0, sum.personalKm, 0.001)
        assertEquals(0.75, sum.businessRatio!!, 0.0001)
    }

    @Test
    fun `목적을 고르지 않았거나 운전자가 미확인이면 표시하고 업무용으로 치지 않는다`() {
        val rows = TripLog.rows(
            ended = listOf(session("a", "kim", 100, 0.0, 20.0, purpose = null, confirmed = false)),
            active = null, currentOdometerKm = null, driverOf = drivers::get,
        )

        assertEquals(listOf(TripLog.NOTE_UNCONFIRMED, TripLog.NOTE_NO_PURPOSE), rows.single().notes)
        val sum = TripLog.summary(rows)
        assertEquals(20.0, sum.unknownKm, 0.001)
        assertEquals(0.0, sum.businessRatio!!, 0.0001)
    }

    @Test
    fun `진행 중인 운행은 지금 계기판 값까지 거리로 넣고 시간순으로 정렬한다`() {
        val active = DriveSession("now", "lee", confirmed = true, startedAtMillis = 500, startOdometerKm = 40.0, purpose = TripPurpose.BUSINESS)
        val rows = TripLog.rows(
            ended = listOf(session("a", "kim", 100, 0.0, 40.0, TripPurpose.BUSINESS)),
            active = active, currentOdometerKm = 52.5, driverOf = drivers::get,
        )

        assertEquals(listOf("a", "now"), rows.map { it.sessionId })
        assertTrue(rows.last().inProgress)
        assertEquals(12.5, rows.last().distanceKm!!, 0.001)
    }

    @Test
    fun `계기판 값이 없으면 거리를 비우고 합계에서 뺀다`() {
        val rows = TripLog.rows(
            ended = listOf(session("a", "kim", 100, null, null, TripPurpose.BUSINESS)),
            active = null, currentOdometerKm = null, driverOf = drivers::get,
        )

        assertNull(rows.single().distanceKm)
        assertTrue(TripLog.NOTE_NO_ODOMETER in rows.single().notes)
        assertNull(TripLog.summary(rows).businessRatio)
    }

    @Test
    fun `운행 목적은 같은 운전자가 이어서 운전해도 유지되고 끝난 기록에 남는다`() {
        val t = DriverSessionTracker(null) { "s1" }
        t.onGear(Gear.PARK, 0, 100.0)
        t.selectDriver("kim", 1, 100.0)
        t.setPurpose(TripPurpose.BUSINESS)
        t.selectDriver("kim", 2, 100.0) // 문 열고 닫은 뒤 '그대로'

        assertEquals(TripPurpose.BUSINESS, t.currentSession?.purpose)
        val ended = (t.endSession(3, 130.0).single() as SessionEffect.SessionEnded).session
        assertEquals(TripPurpose.BUSINESS, ended.purpose)
        assertEquals(30.0, ended.distanceKm!!, 0.001)
    }

    @Test
    fun `띄어 쓴 이름도 붙여 말한 받는 사람으로 알아듣는다`() {
        val draft = VoiceMemoParser.parse(
            "김과장한테 하이패스 카드 충전하라고 전해 줘",
            listOf(VoiceMemoParser.Person("kim", "김 과장")),
        )!!

        assertEquals("kim", draft.toDriverId)
        assertEquals("하이패스 카드 충전해 주세요", draft.text)
    }
}

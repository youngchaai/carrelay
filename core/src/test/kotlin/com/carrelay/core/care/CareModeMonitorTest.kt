package com.carrelay.core.care

import com.carrelay.core.model.DriveSession
import com.carrelay.core.model.DrivingSample
import com.carrelay.core.model.Gear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CareModeMonitorTest {

    private val monitor = CareModeMonitor()
    private val effects = mutableListOf<Pair<Long, CareEffect>>()

    /** from..to 초 동안 1초 간격으로 같은 상태의 샘플을 넣습니다. */
    private fun feed(fromSec: Int, toSec: Int, gear: Gear, speed: Double, brake: Boolean? = true) {
        for (s in fromSec..toSec) {
            val t = s * 1000L
            monitor.onSample(DrivingSample(t, gear, speed, brakePedalPressed = brake))
                .forEach { effects += t to it }
        }
    }

    private inline fun <reified T : CareEffect> firstAt(): Long? =
        effects.firstOrNull { it.second is T }?.first

    @Test
    fun `D 기어로 정차해 조작이 없으면 기록 후 안부를 묻고, 무응답이면 쉼터 추가 후 보호자에게 알린다`() {
        feed(0, 300, Gear.DRIVE, 0.0)

        val recorded = effects.first { (it.second as? CareEffect.Record)?.type == CareEvent.Type.LONG_STOP_RECORDED }.first
        assertEquals(90_000L, recorded)
        assertEquals(120_000L, firstAt<CareEffect.AskAreYouOk>())
        assertEquals(150_000L, firstAt<CareEffect.AddRestStopWaypoint>())
        assertEquals(210_000L, firstAt<CareEffect.NotifyGuardian>())
        assertEquals(CareModeMonitor.Stage.GUARDIAN_NOTIFIED, monitor.stage)
    }

    @Test
    fun `응답하면 기록만 남기고 정상으로 돌아간다`() {
        feed(0, 125, Gear.DRIVE, 0.0)
        assertEquals(CareModeMonitor.Stage.ASKING, monitor.stage)

        val answered = monitor.onDriverAnswered(126_000L)

        assertTrue(answered.contains(CareEffect.Record(CareEvent.Type.CHECK_IN_ANSWERED, CareReason.LONG_STOP_NO_INPUT)))
        assertTrue(answered.contains(CareEffect.CloseCheckIn))
        assertEquals(CareModeMonitor.Stage.NORMAL, monitor.stage)

        // 응답 직후 5분 동안은 같은 상황으로 다시 묻지 않음
        effects.clear()
        feed(127, 400, Gear.DRIVE, 0.0)
        assertEquals(null, firstAt<CareEffect.AskAreYouOk>())
    }

    @Test
    fun `페달을 밟았다 떼면 정차 타이머가 다시 시작된다`() {
        feed(0, 80, Gear.DRIVE, 0.0, brake = true)
        feed(81, 81, Gear.DRIVE, 0.0, brake = false)
        feed(82, 160, Gear.DRIVE, 0.0, brake = false)

        assertEquals(null, firstAt<CareEffect.AskAreYouOk>())
    }

    @Test
    fun `신호 대기처럼 짧은 정차는 묻지 않는다`() {
        feed(0, 60, Gear.DRIVE, 0.0)
        feed(61, 120, Gear.DRIVE, 40.0)
        feed(121, 180, Gear.DRIVE, 0.0)

        assertEquals(null, firstAt<CareEffect.AskAreYouOk>())
        assertTrue(effects.none { (it.second as? CareEffect.Record)?.type == CareEvent.Type.LONG_STOP_RECORDED })
    }

    @Test
    fun `P 기어 주차는 정차로 보지 않는다`() {
        feed(0, 400, Gear.PARK, 0.0)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `짧은 시간에 급감속이 3번 반복되면 안부를 묻는다`() {
        val samples = listOf(0L to 60.0, 1000L to 40.0, 5000L to 60.0, 6000L to 40.0, 10000L to 60.0, 11000L to 40.0)
        val out = samples.flatMap { (t, v) -> monitor.onSample(DrivingSample(t, Gear.DRIVE, v)) }

        assertEquals(3, out.count { it == CareEffect.Record(CareEvent.Type.HARSH_DECEL) })
        assertTrue(out.contains(CareEffect.AskAreYouOk(CareReason.REPEATED_HARSH_DECEL)))
    }

    @Test
    fun `안부 확인 중 정상 주행을 다시 시작하면 장시간 정차 확인은 해제된다`() {
        feed(0, 125, Gear.DRIVE, 0.0)
        effects.clear()
        feed(126, 127, Gear.DRIVE, 20.0)

        assertTrue(effects.any { it.second == CareEffect.CloseCheckIn })
        assertEquals(CareModeMonitor.Stage.NORMAL, monitor.stage)
    }

    @Test
    fun `보호자 하루 요약 문장`() {
        val hour = 3_600_000L
        val sessions = listOf(DriveSession("s1", "grandpa", true, startedAtMillis = 14 * hour, endedAtMillis = 14 * hour + 80 * 60_000L))
        val events = listOf(
            CareEvent("grandpa", CareEvent.Type.HARSH_DECEL, 1),
            CareEvent("grandpa", CareEvent.Type.CHECK_IN_ASKED, 2, CareReason.LONG_STOP_NO_INPUT),
            CareEvent("grandpa", CareEvent.Type.CHECK_IN_ANSWERED, 3, CareReason.LONG_STOP_NO_INPUT),
            CareEvent("daughter", CareEvent.Type.HARSH_DECEL, 4),
        )

        val summary = CareDailySummary.from("grandpa", "아버지", sessions, events, 20 * hour) { (it / hour % 24).toInt() }

        assertEquals("아버지님 운전 1시간 20분, 급감속 1회, 야간 운전 없음, 안부 확인 1회(응답함)", summary.message)
    }
}

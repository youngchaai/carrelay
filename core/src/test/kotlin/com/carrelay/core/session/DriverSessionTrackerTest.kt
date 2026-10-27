package com.carrelay.core.session

import com.carrelay.core.model.DoorEvent
import com.carrelay.core.model.Gear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverSessionTrackerTest {

    private var seq = 0
    private fun tracker(last: String? = "daughter") = DriverSessionTracker(last) { "s${++seq}" }

    @Test
    fun `주차 중 선택하면 확인된 세션이 시작되고 브리핑을 띄운다`() {
        val t = tracker()
        t.onGear(Gear.PARK, 0, 1000.0)

        val effects = t.selectDriver("mom", 10, 1000.0)

        assertEquals(listOf(SessionEffect.ShowBriefing("mom")), effects)
        assertEquals("mom", t.currentSession?.driverId)
        assertTrue(t.currentSession!!.confirmed)
    }

    @Test
    fun `주행 중에는 운전자를 선택할 수 없다`() {
        val t = tracker()
        t.onGear(Gear.PARK, 0, null)
        t.selectDriver("mom", 1, null)
        t.onGear(Gear.DRIVE, 2, null)

        assertFalse(t.canSelectDriver)
        assertNull(t.selectDriver("dad", 3, null))
        assertEquals("mom", t.currentSession?.driverId)
    }

    @Test
    fun `선택 없이 출발하면 직전 운전자로 자동 지정하되 미확인으로 남긴다`() {
        val t = tracker(last = "daughter")
        t.onGear(Gear.PARK, 0, 500.0)

        val effects = t.onGear(Gear.DRIVE, 5, 500.0)

        val auto = effects.single() as SessionEffect.AutoAssigned
        assertEquals("daughter", auto.session.driverId)
        assertFalse(auto.session.confirmed)
    }

    @Test
    fun `직전 운전자가 없으면 게스트로 자동 지정한다`() {
        val t = tracker(last = null)
        t.onGear(Gear.PARK, 0, null)

        val auto = t.onGear(Gear.DRIVE, 5, null).single() as SessionEffect.AutoAssigned

        assertEquals("guest", auto.session.driverId)
    }

    @Test
    fun `주차 중 운전석 문이 열렸다 닫히면 운전자 변경을 묻고 다른 사람을 고르면 교대한다`() {
        val t = tracker()
        t.onGear(Gear.PARK, 0, 1000.0)
        t.selectDriver("mom", 1, 1000.0)
        t.onGear(Gear.DRIVE, 2, 1000.0)
        t.onGear(Gear.PARK, 100, 1168.0)

        assertTrue(t.onDriverDoor(DoorEvent.OPENED).isEmpty())
        assertEquals(listOf(SessionEffect.AskDriverChanged), t.onDriverDoor(DoorEvent.CLOSED))
        assertTrue(t.state is SessionState.ConfirmingChange)

        val effects = t.selectDriver("dad", 110, 1168.0)!!

        val ended = (effects[0] as SessionEffect.SessionEnded).session
        assertEquals("mom", ended.driverId)
        assertEquals(168.0, ended.distanceKm!!, 0.001)
        assertEquals(SessionEffect.ShowBriefing("dad"), effects[1])
        assertEquals("dad", t.currentSession?.driverId)
    }

    @Test
    fun `재확인 중 그대로를 고르거나 응답 없이 출발하면 같은 운전자가 계속한다`() {
        val t = tracker()
        t.onGear(Gear.PARK, 0, null)
        t.selectDriver("mom", 1, null)
        t.onDriverDoor(DoorEvent.OPENED)
        t.onDriverDoor(DoorEvent.CLOSED)

        assertEquals(listOf(SessionEffect.KeptSameDriver), t.onGear(Gear.DRIVE, 5, null))
        assertEquals("mom", t.currentSession?.driverId)
        assertTrue(t.state is SessionState.Active)
    }

    @Test
    fun `주행 중 문 이벤트는 무시한다`() {
        val t = tracker()
        t.onGear(Gear.PARK, 0, null)
        t.selectDriver("mom", 1, null)
        t.onGear(Gear.DRIVE, 2, null)

        t.onDriverDoor(DoorEvent.OPENED)
        assertTrue(t.onDriverDoor(DoorEvent.CLOSED).isEmpty())
    }

    @Test
    fun `운전 끝내기 후에는 직전 운전자를 기억하고 선택 대기로 돌아간다`() {
        val t = tracker(last = null)
        t.onGear(Gear.PARK, 0, null)
        t.selectDriver("mom", 1, null)

        val ended = (t.endSession(50, null).single() as SessionEffect.SessionEnded).session

        assertEquals(50L, ended.endedAtMillis)
        assertEquals(SessionState.AwaitingDriver("mom"), t.state)
    }
}

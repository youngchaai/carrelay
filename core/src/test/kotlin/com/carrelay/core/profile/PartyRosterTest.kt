package com.carrelay.core.profile

import com.carrelay.core.model.Driver
import com.carrelay.core.model.Gear
import com.carrelay.core.model.LicenseClass
import com.carrelay.core.session.DriverSessionTracker
import com.carrelay.core.session.SessionEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PartyRosterTest {

    private val party = listOf(
        Driver("kim", "김 과장", department = "영업1팀"),
        Driver("lee", "이 대리", department = "영업1팀", careModeEnabled = true, guardianName = "가족"),
    )

    @Test
    fun `이름은 비거나 길거나 겹치거나 게스트면 안 된다`() {
        assertNotNull(PartyRoster.nameProblem("  ", party))
        assertNotNull(PartyRoster.nameProblem("가".repeat(PartyRoster.MAX_NAME + 1), party))
        assertNotNull(PartyRoster.nameProblem("김과장", party)) // 띄어쓰기만 다르면 같은 이름
        assertNotNull(PartyRoster.nameProblem("게스트", party))
        assertNull(PartyRoster.nameProblem("박 사원", party))
        assertNull(PartyRoster.nameProblem("김 과장", party, selfId = "kim")) // 자기 이름 그대로 저장
    }

    @Test
    fun `새 사람은 공백을 정리해 맨 뒤에 붙고 정원을 넘지 않는다`() {
        val added = PartyRoster.add(
            party,
            Driver("", "  박 사원 ", department = " 영업2팀 ", licenseClass = LicenseClass.CLASS2_NORMAL, licensedSinceYear = 2025),
            newId = "d-1",
        )
        val p = added.last()
        assertEquals("d-1", p.id)
        assertEquals("박 사원", p.name)
        assertEquals("영업2팀", p.department)
        assertEquals(LicenseClass.CLASS2_NORMAL, p.licenseClass)

        val full = (1..PartyRoster.MAX_DRIVERS).map { Driver("x$it", "사람$it") }
        assertFalse(PartyRoster.canAdd(full))
    }

    @Test
    fun `고쳐도 케어 모드는 본인 동의 절차로만 바뀐다`() {
        val edited = PartyRoster.update(party, Driver("lee", "이 대리", department = "총무팀", careModeEnabled = false, largeText = true))
        val lee = edited.first { it.id == "lee" }
        assertEquals("총무팀", lee.department)
        assertTrue(lee.largeText)
        assertTrue(lee.careModeEnabled)
    }

    @Test
    fun `삭제한 사람은 기록 이름용으로 남고 케어 모드는 꺼진다`() {
        val r = PartyRoster.remove(party, emptyList(), "lee")
        assertEquals(listOf("kim"), r.drivers.map { it.id })
        assertEquals("이 대리", r.former.single().name)
        assertFalse(r.former.single().careModeEnabled)
        // 없는 id 는 그대로
        assertEquals(r, PartyRoster.remove(r.drivers, r.former, "nobody"))
    }

    @Test
    fun `다른 사람을 지워도 남은 사람의 색은 그대로이고 새 사람은 빈 색을 받는다`() {
        val four = listOf(Driver("a", "가"), Driver("b", "나"), Driver("c", "다"), Driver("d", "라"))
        val r = PartyRoster.remove(four, emptyList(), "b")
        assertEquals(listOf(0, 2, 3), r.drivers.map { it.tone })
        val added = PartyRoster.add(r.drivers, Driver("", "마"), newId = "e")
        assertEquals(1, added.last().tone)
    }

    @Test
    fun `삭제한 사람은 직전 운전자로 자동 지정되지 않는다`() {
        val t = DriverSessionTracker("lee") { "s1" }
        t.onGear(Gear.PARK, 0, null)
        t.forgetDriver("lee")

        val effects = t.onGear(Gear.DRIVE, 10, null)
        val auto = effects.single() as SessionEffect.AutoAssigned
        assertEquals(Driver.GUEST_ID, auto.session.driverId)
    }

    @Test
    fun `삭제를 되돌리면 직전 운전자도 다시 채워진다`() {
        val t = DriverSessionTracker("lee") { "s1" }
        t.onGear(Gear.PARK, 0, null)
        t.forgetDriver("lee")
        assertNull(t.lastDriverId)
        t.rememberLastDriver("lee")
        assertEquals("lee", t.lastDriverId)
    }
}

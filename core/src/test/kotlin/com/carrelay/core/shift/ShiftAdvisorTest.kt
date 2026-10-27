package com.carrelay.core.shift

import com.carrelay.core.model.Driver
import com.carrelay.core.model.DriverCondition
import com.carrelay.core.model.DrivingSample
import com.carrelay.core.model.Gear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ShiftAdvisorTest {

    private val minute = 60_000L
    private val mom = Driver("mom", "엄마")
    private val dad = Driver("dad", "아빠")
    private val grandpa = Driver("grandpa", "아버지", careModeEnabled = true)

    @Test
    fun `2시간 연속 주행이면 교대를 권하고 컨디션 좋은 파티원을 후보로 고른다`() {
        val advisor = ShiftAdvisor()
        val others = listOf(dad to DriverCondition("dad", sleepHours = 7.5))

        assertNull(advisor.evaluate(119 * minute, 0, mom, DriverCondition("mom", 7.0), others))
        val advice = advisor.evaluate(120 * minute, 0, mom, DriverCondition("mom", 7.0), others)

        assertNotNull(advice)
        assertEquals("dad", advice!!.reliefDriverId)
        assertEquals("2시간 연속 운전 중이에요. 가까운 휴게소에서 아빠님과 교대하거나 쉬어 가실래요?", advice.message)
    }

    @Test
    fun `수면 5시간 미만이면 1시간 30분에 권하고 케어 모드는 1시간에 권한다`() {
        val advisor = ShiftAdvisor()
        assertEquals(90 * minute, advisor.intervalFor(mom, DriverCondition("mom", sleepHours = 4.0)))
        assertEquals(60 * minute, advisor.intervalFor(grandpa, null))
        assertEquals(120 * minute, advisor.intervalFor(mom, null))
    }

    @Test
    fun `나중에를 누르면 30분 뒤 다시 권한다`() {
        val advisor = ShiftAdvisor()
        assertNotNull(advisor.evaluate(120 * minute, 0, mom, null, emptyList()))
        assertNull(advisor.evaluate(140 * minute, 0, mom, null, emptyList()))
        assertNotNull(advisor.evaluate(150 * minute, 0, mom, null, emptyList()))
    }

    @Test
    fun `휴식 필요 상태인 사람은 교대 후보가 아니다`() {
        val advisor = ShiftAdvisor()
        val others = listOf(dad to DriverCondition("dad", sleepHours = 4.0))

        val advice = advisor.evaluate(120 * minute, 0, mom, null, others)!!

        assertNull(advice.reliefDriverId)
    }

    @Test
    fun `심야 연속 주행 1시간이면 권한다`() {
        val advisor = ShiftAdvisor()
        val advice = advisor.evaluate(70 * minute, 60 * minute, mom, null, emptyList())!!
        assertEquals(ShiftAdvice.Reason.NIGHT_DRIVING, advice.reason)
    }

    @Test
    fun `연속 주행 시계는 10분 이상 주차하면 초기화된다`() {
        val clock = ContinuousDrivingClock(hourOf = { 14 })
        var t = 0L
        repeat(61) { clock.onSample(DrivingSample(t, Gear.DRIVE, 80.0)); t += minute } // 60분 주행
        assertEquals(60 * minute, clock.continuousMillis)

        repeat(5) { clock.onSample(DrivingSample(t, Gear.PARK, 0.0)); t += minute }   // 5분 정차: 휴식 아님
        clock.onSample(DrivingSample(t, Gear.DRIVE, 10.0)); t += minute                // 다시 1분 주행
        assertEquals(61 * minute, clock.continuousMillis)

        repeat(12) { clock.onSample(DrivingSample(t, Gear.PARK, 0.0)); t += minute }  // 10분 넘게 주차: 초기화
        assertEquals(0L, clock.continuousMillis)
    }

    @Test
    fun `컨디션 차이가 분명할 때만 오늘의 운전자를 추천한다`() {
        val drivers = listOf(dad, mom, Driver.GUEST)
        val rec = DriverRecommender.recommend(
            drivers,
            mapOf("dad" to DriverCondition("dad", 4.0), "mom" to DriverCondition("mom", 7.0)),
        )!!
        assertEquals("mom", rec.driverId)
        assertEquals("오늘 장거리는 엄마님이 운전하는 걸 추천해요 (엄마 수면 7시간 / 아빠 수면 4시간)", rec.message)

        assertNull(
            DriverRecommender.recommend(
                drivers,
                mapOf("dad" to DriverCondition("dad", 7.0), "mom" to DriverCondition("mom", 7.5)),
            ),
        )
    }
}

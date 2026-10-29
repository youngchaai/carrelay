package com.carrelay.core.reststop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RestStopFinderTest {

    // 남쪽으로 달리는 차 (37.30, 127.10), 북쪽 4km 뒤와 남쪽 6km 앞에 휴게소
    private val behind = RestStop("뒤쪽휴게소", 37.336, 127.10, RestStop.Kind.SERVICE)
    private val ahead = RestStop("앞쪽휴게소", 37.246, 127.10, RestStop.Kind.SERVICE)
    private val far = RestStop("먼휴게소", 36.50, 127.10, RestStop.Kind.SERVICE)

    @Test
    fun `진행 방향을 알면 뒤쪽이 더 가까워도 앞쪽 휴게소를 고른다`() {
        val pick = RestStopFinder.nearest(listOf(behind, ahead, far), 37.30, 127.10, headingDeg = 180.0)!!
        assertEquals("앞쪽휴게소", pick.stop.name)
        assertTrue(pick.ahead)
        assertEquals(6.0, pick.distanceKm, 0.2)
    }

    @Test
    fun `방향을 모르면 가장 가까운 곳을 고른다`() {
        val pick = RestStopFinder.nearest(listOf(behind, ahead), 37.30, 127.10, headingDeg = null)!!
        assertEquals("뒤쪽휴게소", pick.stop.name)
        assertFalse(pick.ahead)
    }

    @Test
    fun `앞쪽에 없으면 가장 가까운 곳으로 대신한다`() {
        val pick = RestStopFinder.nearest(listOf(behind), 37.30, 127.10, headingDeg = 180.0)!!
        assertEquals("뒤쪽휴게소", pick.stop.name)
        assertFalse(pick.ahead)
    }

    @Test
    fun `반경 안에 없으면 고르지 않는다`() {
        assertNull(RestStopFinder.nearest(listOf(far), 37.30, 127.10, headingDeg = 180.0, maxKm = 50.0))
    }

    @Test
    fun `거리와 방위를 계산한다`() {
        assertEquals(111.2, RestStopFinder.distanceKm(37.0, 127.0, 38.0, 127.0), 0.5)
        assertEquals(0.0, RestStopFinder.bearingDeg(37.0, 127.0, 38.0, 127.0), 0.5)
        assertEquals(180.0, RestStopFinder.bearingDeg(38.0, 127.0, 37.0, 127.0), 0.5)
    }
}

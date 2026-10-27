package com.carrelay.core.handover

import com.carrelay.core.memo.MemoBoard
import com.carrelay.core.model.Driver
import com.carrelay.core.model.MemoPreset
import com.carrelay.core.model.Signal
import com.carrelay.core.model.SignalStatus
import com.carrelay.core.model.VehicleSnapshot
import com.carrelay.core.model.WarningLight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandoverBriefingBuilderTest {

    private val dad = Driver("dad", "아빠")
    private val mom = Driver("mom", "엄마")
    private val daughter = Driver("daughter", "딸")
    private val names = mapOf("dad" to "아빠", "mom" to "엄마", "daughter" to "딸")
    private val now = 1_000_000_000L

    private fun build(
        driver: Driver,
        current: VehicleSnapshot,
        lastSeen: VehicleSnapshot?,
        memos: List<com.carrelay.core.model.Memo> = emptyList(),
    ) = HandoverBriefingBuilder.build(driver, current, lastSeen, memos, { names[it] ?: it }, now)

    @Test
    fun `지난 확인 이후 새로 켜진 경고등을 가장 먼저 알려준다`() {
        val lastSeen = VehicleSnapshot(now - 3_600_000, fuelPercent = 50)
        val current = VehicleSnapshot(now, fuelPercent = 48, activeWarnings = setOf(WarningLight.TIRE_PRESSURE))

        val briefing = build(mom, current, lastSeen)

        val first = briefing.items.first()
        assertEquals(BriefingItem.Kind.NEW_WARNING, first.kind)
        assertEquals("지난 확인 이후 타이어 공기압 경고가 새로 켜졌어요", first.message)
    }

    @Test
    fun `이미 켜져 있던 경고등은 새 경고가 아니다`() {
        val lastSeen = VehicleSnapshot(now - 1000, activeWarnings = setOf(WarningLight.ENGINE_CHECK))
        val current = VehicleSnapshot(now, activeWarnings = setOf(WarningLight.ENGINE_CHECK))

        val briefing = build(mom, current, lastSeen)

        assertTrue(briefing.items.any { it.kind == BriefingItem.Kind.ONGOING_WARNING })
        assertFalse(briefing.items.any { it.kind == BriefingItem.Kind.NEW_WARNING })
    }

    @Test
    fun `연료가 낮으면 출발 전 주유를 권하고 마지막 확인 대비 변화도 보여준다`() {
        val lastSeen = VehicleSnapshot(now - 86_400_000, fuelPercent = 45)
        val current = VehicleSnapshot(now, fuelPercent = 12, rangeKm = 70)

        val briefing = build(mom, current, lastSeen)

        val low = briefing.items.first { it.kind == BriefingItem.Kind.ENERGY_LOW }
        assertEquals("연료 12%, 주행 가능 거리 70km. 출발 전 주유가 필요해요", low.message)
        val change = briefing.items.first { it.kind == BriefingItem.Kind.ENERGY_CHANGE }
        assertEquals("연료: 마지막 확인 때 45% → 지금 12%", change.message)
    }

    @Test
    fun `메모는 읽어도 해결 전까지 계속 남고 작성자에게도 보인다`() {
        var memos = MemoBoard.add(emptyList(), "m1", "dad", now - 10_000, preset = MemoPreset.WIPER_NOISE)
        val current = VehicleSnapshot(now, fuelPercent = 60)

        // 딸이 처음 보면 '새 메모'
        val first = build(daughter, current, null, memos)
        assertEquals(BriefingItem.Kind.NEW_MEMO, first.items.first().kind)
        assertTrue(first.items.first().fromPerson)

        // 딸이 읽음 처리 → 다음에도 '미해결'로 남음
        memos = MemoBoard.markRead(memos, "m1", "daughter", now)
        val second = build(daughter, current, null, memos)
        assertTrue(second.items.any { it.kind == BriefingItem.Kind.UNRESOLVED_MEMO && it.memoId == "m1" })

        // 작성자인 아빠의 다음 사용에도 남음
        val forAuthor = build(dad, current, null, memos)
        assertTrue(forAuthor.items.any { it.memoId == "m1" && it.message.startsWith("아직 해결 안 된 내가 남긴 메모") })
    }

    @Test
    fun `메모를 해결해도 현재 켜진 경고등은 사라지지 않는다`() {
        var memos = MemoBoard.add(emptyList(), "m1", "dad", now - 10_000, preset = MemoPreset.WARNING_LIGHT)
        memos = MemoBoard.resolve(memos, "m1", "mom", "정비소 예약함", now - 5_000)
        val current = VehicleSnapshot(now, activeWarnings = setOf(WarningLight.ENGINE_CHECK))

        val briefing = build(daughter, current, VehicleSnapshot(now - 20_000), memos)

        assertTrue(briefing.unresolvedMemos.isEmpty())
        assertTrue(briefing.items.any { it.kind == BriefingItem.Kind.NEW_WARNING })
        assertTrue(briefing.items.any { it.kind == BriefingItem.Kind.RESOLVED_MEMO && it.message.contains("정비소 예약함") })
    }

    @Test
    fun `지원하지 않는 신호는 숨기지 않고 알려준다`() {
        val current = VehicleSnapshot(
            now,
            unavailable = mapOf(Signal.FUEL to SignalStatus.UNSUPPORTED),
        )

        val briefing = build(mom, current, null)

        assertTrue(briefing.items.any { it.kind == BriefingItem.Kind.SIGNAL_UNAVAILABLE && it.message == "연료 정보: 이 차량은 지원하지 않아요" })
    }

    @Test
    fun `특이사항이 없으면 한 줄로 끝낸다`() {
        val current = VehicleSnapshot(now, fuelPercent = 80)

        val briefing = build(mom, current, VehicleSnapshot(now - 1000, fuelPercent = 81))

        assertEquals(1, briefing.items.size)
        assertEquals(BriefingItem.Kind.ALL_CLEAR, briefing.items.single().kind)
        assertEquals("엄마님, 연료 80%, 새 경고등과 남은 메모가 없어요.", briefing.speech)
    }

    @Test
    fun `오래된 차량 값은 경고한다`() {
        val current = VehicleSnapshot(now - 10 * 60_000, fuelPercent = 70)

        val briefing = build(mom, current, null)

        assertTrue(briefing.items.any { it.kind == BriefingItem.Kind.STALE_DATA && it.message == "차량 정보가 10분 전 값이에요" })
    }

    @Test
    fun `첫 화면은 상위 3개만 보여준다`() {
        var memos = MemoBoard.add(emptyList(), "m1", "dad", now, preset = MemoPreset.WIPER_NOISE)
        memos = MemoBoard.add(memos, "m2", "dad", now, preset = MemoPreset.TIRE_CHECK)
        val current = VehicleSnapshot(now, fuelPercent = 10, activeWarnings = setOf(WarningLight.ENGINE_OIL, WarningLight.WASHER_FLUID))

        val briefing = build(mom, current, VehicleSnapshot(now - 1000, fuelPercent = 30), memos)

        assertTrue(briefing.items.size > 3)
        assertEquals(3, briefing.topItems.size)
    }
}

package com.carrelay.core.memo

import com.carrelay.core.model.MemoPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceMemoParserTest {

    private val family = listOf(
        VoiceMemoParser.Person("dad", "아빠"),
        VoiceMemoParser.Person("mom", "엄마"),
        VoiceMemoParser.Person("daughter", "딸"),
        VoiceMemoParser.Person("grandpa", "할아버지"),
    )

    private fun parse(s: String) = VoiceMemoParser.parse(s, family)

    @Test
    fun `다음 사람한테 주유하라고 전해 줘`() {
        val d = parse("다음 사람한테 주유하라고 전해 줘")!!
        assertEquals("주유해 주세요", d.text)
        assertNull(d.toDriverId)
        assertEquals(MemoPreset.FUEL_LOW, d.preset)
    }

    @Test
    fun `가족 이름으로 받는 사람을 고른다`() {
        val d = parse("엄마한테 타이어 바람 넣으라고 전해줘.")!!
        assertEquals("타이어 바람 넣어 주세요", d.text)
        assertEquals("mom", d.toDriverId)
        assertEquals(MemoPreset.TIRE_CHECK, d.preset)

        val g = parse("할아버지께 경고등 켜졌다고 전해 주세요")!!
        assertEquals("경고등 켜졌어요", g.text)
        assertEquals("grandpa", g.toDriverId)
        assertEquals(MemoPreset.WARNING_LIGHT, g.preset)
    }

    @Test
    fun `옮기는 말투를 메모 말투로 바꾼다`() {
        assertEquals("와이퍼에서 소리 나요", parse("와이퍼에서 소리 난다고 말해 줘")!!.text)
        assertEquals("기름이 부족해요", parse("기름이 부족하다고 알려줘")!!.text)
        assertEquals("내일 장거리 가요", parse("내일 장거리 간다고 남겨 줘")!!.text)
        assertEquals("세차 좀 해 주세요", parse("아빠한테 세차 좀 해 달라고 전해 줘")!!.text)
        assertEquals("트렁크 열지 마세요", parse("다음 사람한테 트렁크 열지 말라고 해 줘")!!.text)
        assertEquals("창문 모두 닫아 주세요", parse("창문 모두 닫으라고 전해 줘")!!.text)
        assertEquals("차가 추워요", parse("차가 춥다고 전해 줘")!!.text)
        assertEquals("우산 트렁크에 있어요", parse("우산 트렁크에 있다고 전해 줘")!!.text)
        assertEquals("에어컨 꺼 주세요", parse("에어컨 끄라고 전해 줘")!!.text)
        assertEquals("열쇠 주세요", parse("딸한테 열쇠 주라고 전해 줘")!!.text)
    }

    @Test
    fun `모르는 말투와 충전해 같은 낱말은 그대로 둔다`() {
        val d = parse("다음 사람한테 충전해 줘")!!
        assertEquals("충전해 줘", d.text)
        assertEquals(MemoPreset.CHARGE_NEEDED, d.preset)
        assertEquals("트렁크에 우산 있어", parse("트렁크에 우산 있어")!!.text)
        assertEquals("내일 장거리 여행", parse("내일 장거리 여행이라고 전해 줘")!!.text)
        assertNull(parse("트렁크에 우산 있어")!!.toDriverId)
    }

    @Test
    fun `내용이 없으면 메모를 만들지 않는다`() {
        assertNull(parse(""))
        assertNull(parse("  전해 줘. "))
        assertNull(parse("다음 사람한테 전해 줘"))
    }

    @Test
    fun `띄어쓰기 없이 인식돼도 처리한다`() {
        assertEquals("주유해 주세요", parse("다음사람한테 주유하라고전해줘")!!.text)
    }
}

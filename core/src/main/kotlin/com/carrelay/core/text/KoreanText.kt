package com.carrelay.core.text

import kotlin.math.roundToInt

/** 화면·음성에 쓰는 한국어 표현 도우미. */
object KoreanText {

    /** 140분 → "2시간 20분", 45분 → "45분" */
    fun duration(millis: Long): String {
        val totalMinutes = (millis / 60_000L).coerceAtLeast(0L)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours == 0L -> "${minutes}분"
            minutes == 0L -> "${hours}시간"
            else -> "${hours}시간 ${minutes}분"
        }
    }

    /** 12.34 → "12km", 0.4 → "0.4km" */
    fun distance(km: Double): String =
        if (km >= 10) "${km.roundToInt()}km" else "${(km * 10).roundToInt() / 10.0}km"

    /** 받침이 있으면 "이", 없으면 "가": "엔진 경고등" → "엔진 경고등이", "공기압 경고" → "공기압 경고가" */
    fun subject(word: String): String = word + if (hasFinalConsonant(word)) "이" else "가"

    /** 받침이 있으면 "을", 없으면 "를" */
    fun obj(word: String): String = word + if (hasFinalConsonant(word)) "을" else "를"

    private fun hasFinalConsonant(word: String): Boolean {
        val last = word.trimEnd().lastOrNull { it in '가'..'힣' } ?: return false
        return (last.code - '가'.code) % 28 != 0
    }

    fun hours(h: Double): String {
        val rounded = (h * 2).roundToInt() / 2.0
        return if (rounded % 1.0 == 0.0) "${rounded.toInt()}시간" else "${rounded}시간"
    }
}

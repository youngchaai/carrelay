package com.carrelay.core.memo

import com.carrelay.core.model.MemoPreset

/** 말로 남긴 메모를 정리한 결과 */
data class VoiceMemoDraft(
    /** 메모 본문 ("주유해 주세요") */
    val text: String,
    /** 특정 가족에게 남긴 말이면 그 사람 id ("엄마한테 …"), 아니면 null(다음 운전자 모두) */
    val toDriverId: String?,
    /** 내용에 맞는 메모 종류(아이콘·'남겼어요' 표시용). 맞는 것이 없으면 null */
    val preset: MemoPreset?,
)

/**
 * "다음 사람한테 주유하라고 전해 줘" → "주유해 주세요".
 *
 * 음성 인식 결과에서 ① 부탁하는 말("전해 줘", "알려 줘")과 ② 받는 사람("엄마한테", "다음 사람한테")을 떼고,
 * ③ 옮기는 말투("~하라고", "~다고")를 메모에 맞는 말투("~해 주세요", "~해요")로 바꿉니다.
 * 바꾸는 규칙에 없는 말은 그대로 둡니다(뜻을 바꾸지 않는 것이 우선).
 */
object VoiceMemoParser {

    data class Person(val id: String, val name: String)

    const val MAX_LENGTH = 60

    fun parse(utterance: String, people: List<Person> = emptyList()): VoiceMemoDraft? {
        var s = normalize(utterance)
        if (s.isEmpty()) return null

        // ① 끝의 부탁하는 말
        s = s.replace(REQUEST_TAIL, "").trim()

        // ② 받는 사람: 가족 이름(긴 이름부터) 또는 '다음 사람'
        var to: String? = null
        for (p in people.sortedByDescending { it.name.length }) {
            val r = Regex("(^|\\s)${Regex.escape(p.name)}\\s*님?\\s*(한테|에게|께|더러|보고)(\\s|$)")
            if (r.containsMatchIn(s)) {
                to = p.id
                s = r.replace(s, " ").trim()
                break
            }
        }
        s = s.replace(EVERYONE, " ").trim()
        s = s.replace(Regex("\\s+"), " ")

        // ③ 옮기는 말투 → 메모 말투
        s = politeEnding(s).trim()
        if (s.isEmpty()) return null
        if (s.length > MAX_LENGTH) s = s.take(MAX_LENGTH).trimEnd() + "…"
        return VoiceMemoDraft(text = s, toDriverId = to, preset = presetFor(s))
    }

    /** 내용으로 메모 종류를 고릅니다(앞의 것이 우선). */
    fun presetFor(text: String): MemoPreset? = PRESET_KEYWORDS.firstOrNull { (_, words) -> words.any { it in text } }?.first

    // ── 규칙 ────────────────────────────────────────────

    // "~라고 해 줘"의 '해'는 '고' 바로 뒤에서만 뗍니다("충전해 줘"의 '전해'를 떼지 않도록 낱말 앞도 확인).
    private val REQUEST_TAIL = Regex(
        "(?:(?<=고)\\s*(?:해|전해|전달해|말해|알려|남겨|얘기해|이야기해)|(?:^|\\s)(?:전해|전달해|말해|알려|남겨|메모해|메모\\s*남겨|얘기해|이야기해))" +
            "\\s*(?:줘요|줘|주세요|줄래요?|주라)?\\s*$",
    )
    // 받는 사람 표시는 조사(한테·에게·께)가 붙은 것만 뗍니다("창문 모두 닫으라고"의 '모두'는 남김).
    private val EVERYONE = Regex(
        "(^|\\s)(?:다음\\s*(?:에\\s*)?(?:타는\\s*|탈\\s*)?(?:사람|운전자|분)\\s*분?|모두|다들|가족들?)\\s*(?:한테|에게|께|더러)(?=\\s|$)",
    )

    private val PRESET_KEYWORDS: List<Pair<MemoPreset, List<String>>> = listOf(
        MemoPreset.WARNING_LIGHT to listOf("경고등"),
        MemoPreset.WIPER_NOISE to listOf("와이퍼"),
        MemoPreset.FUEL_LOW to listOf("주유", "기름", "연료"),
        MemoPreset.CHARGE_NEEDED to listOf("충전"),
        MemoPreset.TIRE_CHECK to listOf("타이어", "공기압", "바람"),
        MemoPreset.STRANGE_NOISE to listOf("소리", "소음", "잡음"),
        MemoPreset.NEEDS_CLEANING to listOf("청소", "세차", "쓰레기", "더러"),
        MemoPreset.LONG_TRIP_PLANNED to listOf("장거리", "멀리", "여행"),
    )

    /** "보라고"처럼 받침 없는 줄기에 바로 '라고'가 붙는 동사(명사+라고와 헷갈리지 않게 아는 것만) */
    private val VOWEL_VERBS_EXACT = setOf("보", "오", "끄", "쓰", "켜", "가", "빼", "대", "사", "타", "서")
    private val VOWEL_VERBS_SUFFIX = listOf(
        "바꾸", "채우", "치우", "세우", "비우", "끼우", "맡기", "버리", "마시", "고치", "올리", "내리", "잠그", "가져오", "데려오",
    )
    /** ㅂ 불규칙 ("춥다고" → "추워요") */
    private val B_IRREGULAR = setOf("춥", "덥", "뜨겁", "무겁", "가볍", "어렵", "시끄럽", "차갑")

    private fun normalize(raw: String): String =
        raw.replace(Regex("[\"“”'‘’.!?~,…]"), " ").replace(Regex("\\s+"), " ").trim()

    private fun politeEnding(text: String): String {
        val i = text.lastIndexOf(' ')
        val head = if (i >= 0) text.substring(0, i + 1) else ""
        val w = if (i >= 0) text.substring(i + 1) else text
        val out: String = when {
            w.endsWith("말라고") -> w.dropLast(3) + "마세요" // "열지 말라고" → "열지 마세요"
            w == "달라고" -> return head.trimEnd() + " 주세요"
            w.endsWith("달라고") -> w.dropLast(3) + " 주세요" // "넣어달라고" → "넣어 주세요"
            w.endsWith("주라고") -> w.dropLast(3) + "주세요" // "열쇠 주라고" → "열쇠 주세요", "바꿔주라고" → "바꿔주세요"
            w.endsWith("하라고") -> w.dropLast(3) + "해 주세요"
            w.endsWith("으라고") && w.length >= 4 && Hangul.hasFinal(w[w.length - 4]) ->
                Hangul.infinitive(w.dropLast(3)) + " 주세요" // "넣으라고" → "넣어 주세요"
            w.endsWith("라고") && isVowelVerb(w.dropLast(2)) -> Hangul.infinitive(w.dropLast(2)) + " 주세요"
            w.endsWith("이라고") -> w.dropLast(3) // "내일 장거리 여행이라고" → "내일 장거리 여행"
            w.endsWith("라고") -> w.dropLast(2)
            w.endsWith("다고") && w.length >= 3 -> declarative(w.dropLast(2)) ?: (w.dropLast(1)) // 모르면 "~다"
            else -> w
        }
        return head + out
    }

    private fun isVowelVerb(stem: String): Boolean =
        stem in VOWEL_VERBS_EXACT || VOWEL_VERBS_SUFFIX.any { stem.endsWith(it) && stem.length <= it.length + 2 }

    /** "필요하" / "났" / "난" / "있" → "필요해요" / "났어요" / "나요" / "있어요" (규칙에 없으면 null) */
    private fun declarative(stem: String): String? {
        if (stem.isEmpty()) return null
        if (stem.endsWith("하")) return stem.dropLast(1) + "해요"
        val last = stem.last()
        if (!Hangul.isSyllable(last)) return null
        val fin = Hangul.finalOf(last)
        return when {
            fin == Hangul.FINAL_SS -> "${stem}어요" // 했/났/켜졌 + 어요
            stem.endsWith("는") && stem.length >= 2 -> Hangul.infinitive(stem.dropLast(1)) + "요" // 먹는 → 먹어요
            fin == Hangul.FINAL_N -> Hangul.infinitive(stem.dropLast(1) + Hangul.withoutFinal(last)) + "요" // 난 → 나요
            fin == Hangul.FINAL_B && B_IRREGULAR.any { stem.endsWith(it) } ->
                stem.dropLast(1) + Hangul.withoutFinal(last) + "워요" // 춥 → 추워요
            else -> Hangul.infinitive(stem) + "요" // 있 → 있어요, 크 → 커요
        }
    }
}

/** 한글 음절 조립·분해 (어미 활용용 최소 기능) */
internal object Hangul {
    private const val BASE = 0xAC00
    private const val LAST = 0xD7A3
    const val FINAL_N = 4
    const val FINAL_B = 17
    const val FINAL_SS = 20

    // 중성 번호
    private const val A = 0
    private const val EO = 4
    private const val YEO = 6
    private const val O = 8
    private const val WA = 9
    private const val WAE = 10
    private const val OE = 11
    private const val U = 13
    private const val WO = 14
    private const val EU = 18
    private const val I = 20

    fun isSyllable(c: Char): Boolean = c.code in BASE..LAST
    fun finalOf(c: Char): Int = if (isSyllable(c)) (c.code - BASE) % 28 else 0
    fun hasFinal(c: Char): Boolean = finalOf(c) != 0
    private fun medialOf(c: Char): Int = ((c.code - BASE) / 28) % 21
    private fun initialOf(c: Char): Int = (c.code - BASE) / (21 * 28)
    private fun compose(initial: Int, medial: Int, final: Int = 0): Char = (BASE + (initial * 21 + medial) * 28 + final).toChar()
    fun withoutFinal(c: Char): Char = if (isSyllable(c)) compose(initialOf(c), medialOf(c)) else c

    private fun bright(c: Char): Boolean = isSyllable(c) && medialOf(c) in setOf(A, O, 2 /* ㅑ */)

    /** 줄기 + 아/어 ("넣" → "넣어", "보" → "봐", "하" → "해", "마시" → "마셔") */
    fun infinitive(stem: String): String {
        if (stem.isEmpty()) return stem
        if (stem.endsWith("하")) return stem.dropLast(1) + "해"
        val last = stem.last()
        if (!isSyllable(last)) return stem + "어"
        if (hasFinal(last)) return stem + if (bright(last)) "아" else "어"
        val init = initialOf(last)
        val prev = stem.dropLast(1)
        return when (medialOf(last)) {
            O -> prev + compose(init, WA)
            U -> prev + compose(init, WO)
            I -> prev + compose(init, YEO)
            OE -> prev + compose(init, WAE)
            EU -> prev + compose(init, if (prev.isNotEmpty() && bright(prev.last())) A else EO)
            else -> stem // 가·서·빼·켜·새 등은 그대로
        }
    }
}

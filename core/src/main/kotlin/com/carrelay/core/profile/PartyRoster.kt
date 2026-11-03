package com.carrelay.core.profile

import com.carrelay.core.model.Driver

/**
 * 파티원(가족·직원) 추가·고치기·삭제. 원본 목록을 바꾸지 않고 새 목록을 돌려줍니다.
 *
 * 규칙
 * - 이름은 필수, 10자 이내, 파티 안에서 겹치지 않게(띄어쓰기 무시), '게스트'는 쓸 수 없음.
 * - 한 차에 최대 [MAX_DRIVERS]명. 카드가 한 화면에 다 보이고, 실제 공용차도 이 정도입니다.
 * - 삭제해도 지난 운전 기록·운행기록부·메모의 이름은 남아야 하므로(회사 차는 증빙), 지운 사람은 [Removal.former] 로 옮깁니다.
 * - 지금 운전 중인 사람은 삭제하지 않습니다(운전 구간이 주인 없이 남지 않게).
 */
object PartyRoster {
    const val MAX_DRIVERS = 8
    const val MAX_NAME = 10
    const val MAX_DEPARTMENT = 12

    /** 이름 문제를 사람이 읽을 문장으로. 문제없으면 null */
    fun nameProblem(name: String, others: List<Driver>, selfId: String? = null): String? {
        val n = name.trim()
        val key = n.replace(" ", "")
        return when {
            n.isEmpty() -> "이름을 적어 주세요"
            n.length > MAX_NAME -> "이름은 ${MAX_NAME}자까지 쓸 수 있어요"
            key == Driver.GUEST.name -> "'게스트'는 이미 있는 이름이에요"
            others.any { it.id != selfId && it.name.replace(" ", "") == key } -> "같은 이름이 이미 있어요"
            else -> null
        }
    }

    fun canAdd(drivers: List<Driver>): Boolean = drivers.size < MAX_DRIVERS

    /** 새 사람을 맨 뒤에 붙입니다. 이름·부서는 앞뒤 공백을 지웁니다. */
    fun add(drivers: List<Driver>, draft: Driver, newId: String): List<Driver> {
        require(canAdd(drivers)) { "최대 ${MAX_DRIVERS}명까지예요" }
        require(nameProblem(draft.name, drivers) == null) { "이름 확인 필요" }
        return drivers + clean(draft).copy(id = newId, isGuest = false, tone = nextTone(drivers))
    }

    /** 지금 파티원이 쓰지 않는 가장 작은 색 번호 */
    fun nextTone(drivers: List<Driver>): Int {
        val used = drivers.mapIndexed { i, d -> d.tone ?: i }.toSet()
        return generateSequence(0) { it + 1 }.first { it !in used }
    }

    /** 같은 id 를 고친 내용으로 바꿉니다. 케어 모드 켜고 끄기는 본인 동의 절차로만 하므로 여기서는 그대로 둡니다. */
    fun update(drivers: List<Driver>, draft: Driver): List<Driver> {
        require(nameProblem(draft.name, drivers, selfId = draft.id) == null) { "이름 확인 필요" }
        return drivers.mapIndexed { i, d ->
            if (d.id == draft.id) clean(draft).copy(careModeEnabled = d.careModeEnabled, isGuest = false, tone = d.tone ?: i) else d
        }
    }

    data class Removal(val drivers: List<Driver>, val former: List<Driver>)

    /** 삭제: 목록에서 빼고, 기록 표시용으로 [former] 에 남깁니다(같은 id 는 한 번만). */
    fun remove(drivers: List<Driver>, former: List<Driver>, id: String): Removal {
        val gone = drivers.firstOrNull { it.id == id } ?: return Removal(drivers, former)
        // 남는 사람의 색이 바뀌지 않게, 색 번호가 없던 사람은 지금 순서를 색 번호로 굳힙니다.
        val fixed = drivers.mapIndexed { i, d -> if (d.tone == null) d.copy(tone = i) else d }
        return Removal(
            drivers = fixed.filter { it.id != id },
            former = former.filter { it.id != id } + gone.copy(careModeEnabled = false),
        )
    }

    private fun clean(d: Driver) = d.copy(
        name = d.name.trim(),
        department = d.department?.trim()?.take(MAX_DEPARTMENT)?.takeIf { it.isNotEmpty() },
        guardianName = d.guardianName?.trim()?.take(MAX_NAME)?.takeIf { it.isNotEmpty() },
    )
}

package com.carrelay.core.handover

import com.carrelay.core.incident.IncidentLog
import com.carrelay.core.memo.MemoBoard
import com.carrelay.core.model.Driver
import com.carrelay.core.model.Incident
import com.carrelay.core.model.IncidentStatus
import com.carrelay.core.model.Memo
import com.carrelay.core.model.Signal
import com.carrelay.core.model.VehicleSnapshot
import com.carrelay.core.model.WarningLight
import com.carrelay.core.text.KoreanText

/** 브리핑 한 줄. [priority] 가 클수록 먼저 보여주고 먼저 읽어 줍니다. */
data class BriefingItem(
    val kind: Kind,
    val priority: Int,
    val message: String,
    /** 사람이 남긴 정보면 true. 차량 센서가 확인한 사실과 화면에서 구분합니다. */
    val fromPerson: Boolean = false,
    val memoId: String? = null,
    val incidentId: String? = null,
) {
    enum class Kind {
        /** 처리 안 된 사고·파손 (처리 완료 전까지 계속 표시) */
        OPEN_INCIDENT,
        NEW_WARNING,
        ONGOING_WARNING,
        ENERGY_LOW,
        ENERGY_CHANGE,
        DISTANCE_SINCE_LAST,
        NEW_MEMO,
        UNRESOLVED_MEMO,
        RESOLVED_MEMO,
        SIGNAL_UNAVAILABLE,
        STALE_DATA,
        ALL_CLEAR,
    }
}

data class Briefing(
    val driver: Driver,
    val current: VehicleSnapshot,
    /** 이 운전자가 마지막으로 차를 확인한(넘겨준) 시점의 상태. 처음 타면 null. */
    val lastSeen: VehicleSnapshot?,
    val items: List<BriefingItem>,
    val unresolvedMemos: List<Memo>,
) {
    /** 첫 화면에는 상위 3개만 보여 줍니다. */
    val topItems: List<BriefingItem> get() = items.take(TOP_COUNT)

    /** 음성 안내 문장. 정상 상태를 길게 읽지 않도록 상위 항목만 읽습니다. */
    val speech: String
        get() = buildString {
            append("${driver.name}님, ")
            append(topItems.joinToString(" ") { it.message.trimEnd('.') + "." })
        }

    companion object {
        const val TOP_COUNT = 3
    }
}

/**
 * 인수인계 브리핑을 만듭니다.
 *
 * 핵심 원칙(아이디어 검토 문서 반영)
 * 1. 현재 상태와 '내가 마지막으로 확인했을 때'를 함께 보여 줍니다. 그 사이 누가 무엇을 했는지는 추정하지 않습니다.
 * 2. 차량 데이터와 사람의 메모를 묶되, 사람의 메모는 [BriefingItem.fromPerson] 으로 구분합니다.
 * 3. 메모의 읽음/해결과 차량 경고는 독립적입니다. 메모를 해결해도 켜져 있는 경고등은 계속 보입니다.
 * 4. 우선순위 상위 3개로 시작하고, 특이사항이 없으면 한 줄로 끝냅니다.
 */
object HandoverBriefingBuilder {

    const val LOW_ENERGY_PERCENT = 15
    const val ENERGY_CHANGE_MIN_POINTS = 5

    fun build(
        driver: Driver,
        current: VehicleSnapshot,
        lastSeen: VehicleSnapshot?,
        memos: List<Memo>,
        nameOf: (driverId: String) -> String,
        nowMillis: Long,
        incidents: List<Incident> = emptyList(),
    ): Briefing {
        val items = mutableListOf<BriefingItem>()

        // 0) 처리 안 된 사고·파손: 처리 완료 전까지 모든 운전자에게 맨 위에 보여 줍니다.
        IncidentLog.open(incidents).forEach { inc ->
            items += BriefingItem(
                BriefingItem.Kind.OPEN_INCIDENT,
                priority = 110,
                message = "${inc.summary}: ${if (inc.status == IncidentStatus.IN_PROGRESS) "수리·보험 처리 중이에요" else "아직 고치지 않았어요"}",
                fromPerson = true,
                incidentId = inc.id,
            )
        }

        // 1) 경고등: 지난 확인 이후 새로 켜진 것과 계속 켜져 있는 것을 구분
        val previousWarnings: Set<WarningLight> = lastSeen?.activeWarnings ?: emptySet()
        current.activeWarnings.sortedByDescending { it.severity }.forEach { warning ->
            if (lastSeen != null && warning !in previousWarnings) {
                items += BriefingItem(
                    BriefingItem.Kind.NEW_WARNING,
                    priority = 100 + warning.severity,
                    message = "지난번 이후 ${KoreanText.subject(warning.label)} 새로 켜졌어요",
                )
            } else {
                items += BriefingItem(
                    BriefingItem.Kind.ONGOING_WARNING,
                    priority = 80 + warning.severity,
                    message = "${KoreanText.subject(warning.label)} 켜져 있어요",
                )
            }
        }

        // 2) 연료·배터리
        val energy = current.energyPercent
        val label = current.energyLabel
        if (energy != null) {
            val rangeText = current.rangeKm?.let { ", ${it}km 갈 수 있어요" } ?: ""
            if (energy <= LOW_ENERGY_PERCENT) {
                items += BriefingItem(
                    BriefingItem.Kind.ENERGY_LOW,
                    priority = 95,
                    message = "$label ${energy}%$rangeText. 출발 전에 ${if (label == "연료") "주유해" else "충전해"} 주세요",
                )
            }
            val before = lastSeen?.energyPercent
            if (before != null && kotlin.math.abs(before - energy) >= ENERGY_CHANGE_MIN_POINTS) {
                items += BriefingItem(
                    BriefingItem.Kind.ENERGY_CHANGE,
                    priority = 40,
                    message = "${KoreanText.subject(label)} 지난번 ${before}%에서 지금 ${energy}%로 ${if (energy > before) "늘었어요" else "줄었어요"}",
                )
            }
        }

        // 3) 마지막 확인 이후 주행거리
        val odoNow = current.odometerKm
        val odoBefore = lastSeen?.odometerKm
        if (odoNow != null && odoBefore != null && odoNow - odoBefore >= 1.0) {
            items += BriefingItem(
                BriefingItem.Kind.DISTANCE_SINCE_LAST,
                priority = 30,
                message = "지난번 이후 ${KoreanText.distance(odoNow - odoBefore)} 달렸어요",
            )
        }

        // 4) 사람의 메모: 안 읽은 메모 > 읽었지만 미해결 메모
        val unresolved = MemoBoard.unresolvedFor(memos, driver.id)
        unresolved.forEach { memo ->
            val to = memo.toDriverId
            val author = when {
                memo.authorId == driver.id -> "내가 남긴"
                to == driver.id -> "${KoreanText.subject(nameOf(memo.authorId))} 나에게 남긴"
                to != null -> "${KoreanText.subject(nameOf(memo.authorId))} ${nameOf(to)}에게 남긴"
                else -> "${nameOf(memo.authorId)}의"
            }
            val isNew = !memo.isReadBy(driver.id)
            items += BriefingItem(
                kind = if (isNew) BriefingItem.Kind.NEW_MEMO else BriefingItem.Kind.UNRESOLVED_MEMO,
                priority = if (isNew) 90 else 60,
                message = if (isNew) "$author 메모: ${memo.text}" else "아직 해결 안 된 $author 메모: ${memo.text}",
                fromPerson = true,
                memoId = memo.id,
            )
        }

        // 5) 지난 확인 이후 해결된 메모
        if (lastSeen != null) {
            MemoBoard.recentlyResolved(memos, sinceMillis = lastSeen.capturedAtMillis)
                .filter { it.resolution?.byDriverId != driver.id }
                .forEach { memo ->
                    val r = memo.resolution!!
                    items += BriefingItem(
                        BriefingItem.Kind.RESOLVED_MEMO,
                        priority = 20,
                        message = "${nameOf(r.byDriverId)}님이 '${memo.text}' 메모를 해결했어요" +
                            if (r.note.isNotBlank()) " (${r.note})" else "",
                        fromPerson = true,
                        memoId = memo.id,
                    )
                }
        }

        // 6) 읽지 못한 신호와 오래된 값: 숨기지 않고 알려 줍니다.
        current.unavailable
            .filterKeys { it == Signal.FUEL || it == Signal.EV_BATTERY || it == Signal.WARNINGS }
            .forEach { (signal, status) ->
                items += BriefingItem(
                    BriefingItem.Kind.SIGNAL_UNAVAILABLE,
                    priority = 10,
                    message = "${signal.label} 정보: ${status.label}",
                )
            }
        if (current.isStale(nowMillis)) {
            items += BriefingItem(
                BriefingItem.Kind.STALE_DATA,
                priority = 50,
                message = "차량 정보가 ${KoreanText.duration(nowMillis - current.capturedAtMillis)} 전 값이에요",
            )
        }

        // 7) 특이사항 없음은 한 줄로
        val notable = items.filter { it.kind != BriefingItem.Kind.SIGNAL_UNAVAILABLE }
        if (notable.isEmpty()) {
            val energyText = energy?.let { "$label ${it}%, " } ?: ""
            items += BriefingItem(
                BriefingItem.Kind.ALL_CLEAR,
                priority = 15, // '읽을 수 없는 신호' 안내(10)보다 위에
                message = "${energyText}${if (Signal.WARNINGS in current.unavailable) "" else "새 경고등과 "}남은 메모가 없어요",
            )
        }

        return Briefing(
            driver = driver,
            current = current,
            lastSeen = lastSeen,
            items = items.sortedByDescending { it.priority },
            unresolvedMemos = unresolved,
        )
    }
}

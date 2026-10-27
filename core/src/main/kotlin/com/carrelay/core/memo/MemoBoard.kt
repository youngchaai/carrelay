package com.carrelay.core.memo

import com.carrelay.core.model.Memo
import com.carrelay.core.model.MemoPreset
import com.carrelay.core.model.Resolution

/**
 * 메모 목록을 다루는 순수 함수 모음. 원본 리스트를 바꾸지 않고 새 리스트를 돌려줍니다.
 */
object MemoBoard {

    fun add(
        memos: List<Memo>,
        id: String,
        authorId: String,
        nowMillis: Long,
        preset: MemoPreset? = null,
        text: String? = null,
    ): List<Memo> {
        val body: String = text?.trim()?.takeIf { it.isNotEmpty() }
            ?: preset?.text
            ?: throw IllegalArgumentException("메모 내용이 비어 있어요")
        return memos + Memo(
            id = id,
            authorId = authorId,
            text = body,
            createdAtMillis = nowMillis,
            preset = preset,
        )
    }

    /** 읽음 처리. 해결 상태는 바꾸지 않습니다. */
    fun markRead(memos: List<Memo>, memoId: String, driverId: String, nowMillis: Long): List<Memo> =
        memos.map { memo ->
            if (memo.id == memoId && !memo.readBy.containsKey(driverId)) {
                memo.copy(readBy = memo.readBy + (driverId to nowMillis))
            } else {
                memo
            }
        }

    /** 해결 처리. 해결한 사람은 자동으로 읽음 처리됩니다. */
    fun resolve(
        memos: List<Memo>,
        memoId: String,
        driverId: String,
        note: String,
        nowMillis: Long,
    ): List<Memo> =
        memos.map { memo ->
            if (memo.id == memoId && !memo.isResolved) {
                memo.copy(
                    readBy = memo.readBy + (driverId to (memo.readBy[driverId] ?: nowMillis)),
                    resolution = Resolution(byDriverId = driverId, note = note, atMillis = nowMillis),
                )
            } else {
                memo
            }
        }

    /**
     * 이 운전자에게 보여줄 미해결 메모. 작성자 본인에게도 보입니다(문제가 남아 있으므로).
     * 이 운전자가 아직 읽지 않은 메모가 앞에 옵니다.
     */
    fun unresolvedFor(memos: List<Memo>, driverId: String): List<Memo> =
        memos.filter { !it.isResolved }
            .sortedWith(
                compareBy<Memo> { it.isReadBy(driverId) }
                    .thenByDescending { it.createdAtMillis },
            )

    /** 최근 해결된 메모(해결 기록을 다음 운전자에게 알려주기 위함). */
    fun recentlyResolved(memos: List<Memo>, sinceMillis: Long): List<Memo> =
        memos.filter { it.resolution != null && it.resolution.atMillis >= sinceMillis }
            .sortedByDescending { it.resolution!!.atMillis }
}

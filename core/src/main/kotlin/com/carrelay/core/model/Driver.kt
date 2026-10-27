package com.carrelay.core.model

import kotlinx.serialization.Serializable

/**
 * 차를 함께 쓰는 파티원 한 명.
 *
 * @property careModeEnabled 케어 모드. 본인 동의로만 켜고 끌 수 있습니다.
 * @property guardianName 케어 모드 알림을 받을 보호자 표시 이름 (연락처는 서버에만 둡니다).
 */
@Serializable
data class Driver(
    val id: String,
    val name: String,
    val isGuest: Boolean = false,
    val careModeEnabled: Boolean = false,
    val guardianName: String? = null,
) {
    companion object {
        const val GUEST_ID = "guest"
        val GUEST = Driver(id = GUEST_ID, name = "게스트", isGuest = true)
    }
}

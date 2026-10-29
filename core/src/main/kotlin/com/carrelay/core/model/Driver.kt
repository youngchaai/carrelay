package com.carrelay.core.model

import kotlinx.serialization.Serializable

/** 운전면허 종류 (면허 번호는 고유식별정보라 저장하지 않습니다) */
@Serializable
enum class LicenseClass(val label: String) {
    CLASS1_LARGE("1종 대형"),
    CLASS1_NORMAL("1종 보통"),
    CLASS2_NORMAL("2종 보통"),
    CLASS2_SMALL("2종 소형"),
}

/**
 * 차를 함께 쓰는 파티원 한 명.
 *
 * @property careModeEnabled 케어 모드. 본인 동의로만 켜고 끌 수 있습니다.
 * @property guardianName 케어 모드 알림을 받을 보호자 표시 이름 (연락처는 서버에만 둡니다).
 * @property licenseExpiryEpochDay 면허 갱신(적성검사) 기한. LocalDate.toEpochDay() 값.
 * @property licensedSinceYear 면허를 처음 받은 해 (초보 운전 표시용).
 * @property largeText 큰 글씨 모드. 이 사람이 운전할 때만 차 화면의 글씨와 버튼을 키웁니다.
 */
@Serializable
data class Driver(
    val id: String,
    val name: String,
    val isGuest: Boolean = false,
    val careModeEnabled: Boolean = false,
    val guardianName: String? = null,
    val licenseClass: LicenseClass? = null,
    val licenseExpiryEpochDay: Long? = null,
    val licensedSinceYear: Int? = null,
    val largeText: Boolean = false,
) {
    companion object {
        const val GUEST_ID = "guest"
        val GUEST = Driver(id = GUEST_ID, name = "게스트", isGuest = true)
    }
}

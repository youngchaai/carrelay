package com.carrelay.core.profile

import com.carrelay.core.model.Driver

enum class LicenseState(val label: String) {
    OK("유효"),
    RENEW_SOON("갱신 임박"),
    EXPIRED("갱신 기한 지남"),
    UNKNOWN("정보 없음"),
}

data class LicenseInfo(
    val state: LicenseState,
    /** 갱신 기한까지 남은 날 (지났으면 음수) */
    val daysLeft: Long?,
    /** 도로교통법상 초보운전자: 처음 면허를 받은 날부터 2년 미만 (여기서는 연도로 근사) */
    val isNovice: Boolean,
    val yearsLicensed: Int?,
) {
    /** "갱신 D-20", "갱신 기한 3일 지남" */
    val renewalLabel: String?
        get() = when {
            daysLeft == null -> null
            daysLeft < 0 -> "갱신 기한 ${-daysLeft}일 지남"
            else -> "갱신 D-$daysLeft"
        }
}

/** 면허 상태 판단. 날짜는 밖에서 넣어 테스트하기 쉽게 합니다. */
object LicenseCheck {
    const val RENEW_SOON_DAYS = 60L
    const val NOVICE_YEARS = 2

    fun of(driver: Driver, todayEpochDay: Long, currentYear: Int): LicenseInfo {
        val expiry = driver.licenseExpiryEpochDay
        val daysLeft = expiry?.let { it - todayEpochDay }
        val state = when {
            driver.licenseClass == null && expiry == null -> LicenseState.UNKNOWN
            daysLeft == null -> LicenseState.OK
            daysLeft < 0 -> LicenseState.EXPIRED
            daysLeft <= RENEW_SOON_DAYS -> LicenseState.RENEW_SOON
            else -> LicenseState.OK
        }
        val years = driver.licensedSinceYear?.let { currentYear - it }
        return LicenseInfo(
            state = state,
            daysLeft = daysLeft,
            isNovice = years != null && years < NOVICE_YEARS,
            yearsLicensed = years,
        )
    }
}

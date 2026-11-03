package com.carrelay.core.model

import kotlinx.serialization.Serializable

/**
 * 차를 어떤 그룹이 함께 쓰는지.
 * 가족 차: 케어 모드·컨디션 추천을 씁니다.
 * 회사 차: 운행 목적을 받아 운행기록부를 자동으로 채우고, 케어 모드·컨디션 추천은 쓰지 않습니다(직원 감시로 오해되지 않게).
 */
@Serializable
enum class CarUsage(val label: String) {
    FAMILY("가족 차"),
    COMPANY("회사 차"),
}

/**
 * 운행 목적. 업무용승용차 운행기록부(국세청 고시 서식)의 '업무용 사용거리'는 출퇴근용 + 일반 업무용입니다.
 */
@Serializable
enum class TripPurpose(val label: String, val isBusinessUse: Boolean) {
    COMMUTE("출퇴근", true),
    BUSINESS("일반 업무", true),
    PERSONAL("개인 용무", false),
}

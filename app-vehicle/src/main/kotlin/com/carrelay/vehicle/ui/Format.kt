package com.carrelay.vehicle.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
private val dateTimeFormat = DateTimeFormatter.ofPattern("M/d HH:mm").withZone(ZoneId.systemDefault())

fun formatTime(epochMillis: Long): String = timeFormat.format(Instant.ofEpochMilli(epochMillis))

fun formatDateTime(epochMillis: Long): String = dateTimeFormat.format(Instant.ofEpochMilli(epochMillis))

private val hourMinute = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
private val monthDay = DateTimeFormatter.ofPattern("M/d").withZone(ZoneId.systemDefault())

/** "오늘 08:10" / "어제 19:30" / "3일 전" / "10/28" — 사람이 말하듯 짧게 */
fun formatWhen(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val zone = ZoneId.systemDefault()
    val day = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val days = java.time.temporal.ChronoUnit.DAYS.between(day, today)
    val at = Instant.ofEpochMilli(epochMillis)
    return when {
        days <= 0L -> "오늘 ${hourMinute.format(at)}"
        days == 1L -> "어제 ${hourMinute.format(at)}"
        days < 7L -> "${days}일 전"
        else -> monthDay.format(at)
    }
}

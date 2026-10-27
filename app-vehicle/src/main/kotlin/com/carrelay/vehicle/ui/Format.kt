package com.carrelay.vehicle.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
private val dateTimeFormat = DateTimeFormatter.ofPattern("M/d HH:mm").withZone(ZoneId.systemDefault())

fun formatTime(epochMillis: Long): String = timeFormat.format(Instant.ofEpochMilli(epochMillis))

fun formatDateTime(epochMillis: Long): String = dateTimeFormat.format(Instant.ofEpochMilli(epochMillis))

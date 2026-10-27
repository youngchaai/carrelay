package com.carrelay.vehicle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.carrelay.core.care.CareReason
import com.carrelay.core.text.KoreanText
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.components.BigButton
import com.carrelay.vehicle.ui.components.BigOutlinedButton
import com.carrelay.vehicle.ui.components.LabeledValue
import kotlin.math.roundToInt

/**
 * 주행 중 화면(CarUxRestrictions 대응). 글은 짧게, 버튼은 최대 2개만 둡니다.
 * 우선순위: 케어 모드 안부 확인 > 교대·휴식 권유 > 기본 상태
 */
@Composable
fun DrivingScreen(
    ui: UiState,
    onAnswerCare: () -> Unit,
    onAcceptShift: () -> Unit,
    onDeclineShift: () -> Unit,
) {
    val checkIn = ui.careCheckIn
    val advice = ui.shiftAdvice
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            checkIn != null -> {
                Text("괜찮으세요?", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(
                    when (checkIn) {
                        CareReason.LONG_STOP_NO_INPUT -> "정차가 길어지고 조작이 없어서 여쭤봐요"
                        CareReason.REPEATED_HARSH_DECEL -> "급감속이 반복돼서 여쭤봐요"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BigButton("괜찮아요", onAnswerCare, Modifier.fillMaxWidth(0.7f).height(150.dp))
            }

            advice != null -> {
                Text(
                    advice.message,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(Modifier.fillMaxWidth(0.85f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    BigButton("휴게소 추가", onAcceptShift, Modifier.weight(1f).height(130.dp))
                    BigOutlinedButton("나중에", onDeclineShift, Modifier.weight(1f).height(130.dp))
                }
            }

            else -> {
                val driver = ui.currentDriver
                Text(
                    if (driver != null) "${driver.name}님 운전 중" else "운전 중",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                val sample = ui.driving
                Row(Modifier.fillMaxWidth(0.9f), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    LabeledValue("속도", sample?.speedKmh?.let { "${it.roundToInt()}km/h" } ?: "-", Modifier.weight(1f))
                    LabeledValue("기어", sample?.gear?.label ?: "-", Modifier.weight(1f))
                    LabeledValue("연속 운전", KoreanText.duration(ui.continuousMillis), Modifier.weight(1f))
                    LabeledValue(
                        ui.snapshot?.energyLabel ?: "연료",
                        ui.snapshot?.energyPercent?.let { "$it%" } ?: "-",
                        Modifier.weight(1f),
                    )
                }
                Text(
                    "주행 중에는 화면 조작 대신 음성으로 안내해요",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

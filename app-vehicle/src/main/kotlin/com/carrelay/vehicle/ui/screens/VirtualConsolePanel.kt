package com.carrelay.vehicle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carrelay.core.model.Gear
import com.carrelay.core.model.WarningLight
import com.carrelay.vehicle.data.FakeVehicleGateway
import kotlin.math.roundToInt

/**
 * 가상 차량 패널. Pleos Connect Virtual Console(문·기어·속도·브레이크·연료·경고등 조작) 대신
 * 어느 에뮬레이터에서나 시나리오 A·B 를 재현하기 위한 시연 도구입니다. 실제 차량 빌드에는 나타나지 않습니다.
 */
@Composable
fun VirtualConsolePanel(
    fake: FakeVehicleGateway,
    onAddDrivingTime: (minutes: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val car by fake.car.collectAsStateWithLifecycle()

    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("가상 차량", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "시연용 패널이에요. 실제 차량 데이터가 아니에요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ConsoleGroup("기어") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Gear.PARK, Gear.REVERSE, Gear.NEUTRAL, Gear.DRIVE).forEach { g ->
                    ConsoleChip(g.label, selected = car.gear == g, modifier = Modifier.weight(1f)) { fake.setGear(g) }
                }
            }
        }

        ConsoleGroup("속도 ${car.speedKmh.roundToInt()}km/h  (목표 ${car.targetSpeedKmh.roundToInt()})") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 30, 60, 100).forEach { v ->
                    ConsoleChip(
                        "$v",
                        selected = car.targetSpeedKmh.roundToInt() == v,
                        enabled = car.gear.isDriveGear,
                        modifier = Modifier.weight(1f),
                    ) { fake.setTargetSpeed(v.toDouble()) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConsoleChip("급정거", enabled = car.gear.isDriveGear, modifier = Modifier.weight(1f)) { fake.hardBrake() }
                ConsoleChip("급가속", enabled = car.gear.isDriveGear, modifier = Modifier.weight(1f)) { fake.hardAccelerate() }
                ConsoleChip("페달\n조작", enabled = car.gear.isDriveGear, modifier = Modifier.weight(1f)) { fake.tapPedal() }
            }
            Text(
                "브레이크 ${if (car.brakePressed) "밟음" else "뗌"} · 가속 ${if (car.acceleratorPressed) "밟음" else "뗌"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 시연에서 자주 쓰는 조작을 스크롤 없이 보이도록 위에 둡니다.
        ConsoleGroup("문 · 시간") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConsoleChip("운전석 문\n열고 닫기", enabled = car.gear == Gear.PARK, modifier = Modifier.weight(1f)) {
                    fake.cycleDriverDoor()
                }
                ConsoleChip("연속 주행\n+30분", enabled = car.gear.isDriveGear, modifier = Modifier.weight(1f)) {
                    onAddDrivingTime(30)
                }
            }
        }

        ConsoleGroup("연료 ${car.fuelPercent}%") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConsoleChip("-10", modifier = Modifier.weight(1f)) { fake.setFuel(car.fuelPercent - 10) }
                ConsoleChip("+10", modifier = Modifier.weight(1f)) { fake.setFuel(car.fuelPercent + 10) }
                ConsoleChip("12%", modifier = Modifier.weight(1f)) { fake.setFuel(12) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("연료 정보 지원", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = car.energySupported, onCheckedChange = { fake.setEnergySupported(it) })
            }
        }

        ConsoleGroup("경고등") {
            WarningLight.entries.forEach { w ->
                ConsoleChip(w.label, selected = w in car.warnings, modifier = Modifier.fillMaxWidth()) { fake.toggleWarning(w) }
            }
        }

        Text(
            "누적 주행거리 ${"%.1f".format(car.odometerKm)}km",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConsoleGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun ConsoleChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = when {
            !enabled -> colors.surfaceVariant.copy(alpha = 0.4f)
            selected -> colors.primary
            else -> colors.surfaceVariant
        },
        contentColor = if (selected && enabled) colors.onPrimary else colors.onSurface,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
        )
    }
}

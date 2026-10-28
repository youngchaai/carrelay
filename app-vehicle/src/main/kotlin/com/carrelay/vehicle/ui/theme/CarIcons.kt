package com.carrelay.vehicle.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 카릴레이 아이콘: 24 그리드 위의 단색 선형 아이콘. 추가 라이브러리 없이 Icon(tint=...) 로 색을 입힙니다.
 */
object CarIcons {
    val Person by lazy { stroke("Person", "M12 11a4 4 0 1 0 0-8a4 4 0 0 0 0 8z", "M4 21c0-4.4 3.6-7 8-7s8 2.6 8 7") }
    val Fuel by lazy {
        stroke(
            "Fuel",
            "M4 21V5a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v16", "M3 21h12", "M4 10h10",
            "M14 9h2a2 2 0 0 1 2 2v6a1.5 1.5 0 0 0 3 0V8l-3-3",
        )
    }
    val Bolt by lazy { stroke("Bolt", "M13 2L5 14h6l-1 8 8-12h-6z") }
    val Warning by lazy { stroke("Warning", "M12 3L22 20H2z", "M12 9v5", "M12 17.5v0.5") }
    val Memo by lazy { stroke("Memo", "M5 3h10l4 4v14H5z", "M15 3v4h4", "M8 12h8", "M8 16h6") }
    val Clock by lazy { stroke("Clock", CIRCLE, "M12 7v5l3 2") }
    val Check by lazy { stroke("Check", "M5 12.5l4.5 4.5L19 7") }
    val CheckCircle by lazy { stroke("CheckCircle", CIRCLE, "M8 12.5l3 3 5-6") }
    val Info by lazy { stroke("Info", CIRCLE, "M12 11v5", "M12 8v0.01") }
    val Shield by lazy { stroke("Shield", "M12 3l8 3v6c0 4.5-3.4 8.3-8 9-4.6-0.7-8-4.5-8-9V6z", "M9 12l2 2 4-4") }
    val Home by lazy { stroke("Home", "M3 11l9-7 9 7", "M5 10v10h14V10", "M10 20v-6h4v6") }
    val Records by lazy { stroke("Records", "M8 6h12", "M8 12h12", "M8 18h12", "M4 6h0.01", "M4 12h0.01", "M4 18h0.01") }
    val Settings by lazy {
        stroke(
            "Settings",
            "M4 7h10", "M18 7h2", "M4 17h4", "M12 17h8",
            "M16 9a2 2 0 1 0 0-4a2 2 0 0 0 0 4z", "M10 19a2 2 0 1 0 0-4a2 2 0 0 0 0 4z",
        )
    }
    val Speaker by lazy { stroke("Speaker", "M4 9h4l5-4v14l-5-4H4z", "M16.5 8.5a5 5 0 0 1 0 7", "M19 6a8.5 8.5 0 0 1 0 12") }
    val Handover by lazy { stroke("Handover", "M4 8h13", "M14 5l3 3-3 3", "M20 16H7", "M10 13l-3 3 3 3") }
    val Rest by lazy {
        stroke("Rest", "M4 9h12v5a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5z", "M16 10h1.5a2.5 2.5 0 0 1 0 5H16", "M8 3v3", "M12 3v3")
    }
    val Route by lazy {
        stroke(
            "Route",
            "M6 21a2 2 0 1 0 0-4a2 2 0 0 0 0 4z", "M18 7a2 2 0 1 0 0-4a2 2 0 0 0 0 4z",
            "M8 19h7.5a3.5 3.5 0 0 0 0-7h-7a3.5 3.5 0 0 1 0-7H16",
        )
    }
    val Tire by lazy { stroke("Tire", CIRCLE, "M12 16a4 4 0 1 0 0-8a4 4 0 0 0 0 8z") }
    val Wiper by lazy { stroke("Wiper", "M3 18a13 13 0 0 1 18 0", "M12 18L7 9") }
    val Sound by lazy { stroke("Sound", "M3 12h2", "M7 8v8", "M11 5v14", "M15 8v8", "M19 11v2") }
    val Sparkle by lazy {
        stroke("Sparkle", "M12 3v4", "M12 17v4", "M3 12h4", "M17 12h4", "M6 6l2 2", "M16 16l2 2", "M18 6l-2 2", "M8 16l-2 2")
    }
    val Gauge by lazy { stroke("Gauge", "M4 17a8 8 0 1 1 16 0", "M12 17l4-5") }
    val Bell by lazy { stroke("Bell", "M6 16v-5a6 6 0 0 1 12 0v5l2 2H4z", "M10 20.5a2 2 0 0 0 4 0") }
    val Close by lazy { stroke("Close", "M6 6l12 12", "M18 6L6 18") }
    val Car by lazy {
        stroke("Car", "M5 15l1.6-5.2A2 2 0 0 1 8.5 8.4h7a2 2 0 0 1 1.9 1.4L19 15", "M3 15h18v4H3z", "M7 19v1.5", "M17 19v1.5")
    }

    private const val CIRCLE = "M12 21a9 9 0 1 0 0-18a9 9 0 0 0 0 18z"

    private fun stroke(name: String, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        paths.forEach { d ->
            builder.addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }
}

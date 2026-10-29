package com.carrelay.vehicle

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.carrelay.vehicle.data.AaosCarSignals
import com.carrelay.vehicle.ui.CarRelayApp
import com.carrelay.vehicle.ui.CarRelayViewModel
import com.carrelay.vehicle.ui.theme.CarRelayTheme

/**
 * P0 는 사용자가 앱을 직접 실행하는 것을 전제로 합니다.
 * (시동·문 열림 시 자동 실행은 Pleos 의 허용 조건을 확인한 뒤 확장합니다.)
 */
class MainActivity : ComponentActivity() {

    private val viewModel: CarRelayViewModel by viewModels { CarRelayViewModel.Factory }

    // Pleos 빌드: 속도(급정거·케어 모드)와 위치(휴게소 경유지·보호자 알림 위치) 권한
    private val runtimePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.onPermissionsChanged()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // targetSdk 35: 화면 여백은 CarRelayApp 의 safeDrawingPadding 으로 처리
        if (BuildConfig.USE_PLEOS_SDK) {
            val missing = listOf(AaosCarSignals.PERMISSION_SPEED, PERMISSION_LOCATION)
                .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
            if (missing.isNotEmpty()) runtimePermissions.launch(missing.toTypedArray())
        }
        setContent {
            CarRelayTheme {
                CarRelayApp(viewModel)
            }
        }
    }

    private companion object {
        const val PERMISSION_LOCATION = "pleos.car.permission.FUSED_LOCATION"
    }
}

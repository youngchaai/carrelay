package com.carrelay.vehicle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
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

    /**
     * 개발용(디버그 빌드만): 시연 영상 녹화 때 연속 주행 시간을 앞당깁니다. Pleos 빌드에는 가상 차량 패널이 없어서 둡니다.
     *   adb shell am broadcast --user 10 -p com.carrelay.vehicle -a com.carrelay.vehicle.DEBUG_ADD_DRIVING --ei minutes 120
     */
    private val debugReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.debugAddDrivingTime(intent.getIntExtra("minutes", 30))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // targetSdk 35: 화면 여백은 CarRelayApp 의 safeDrawingPadding 으로 처리
        if (BuildConfig.USE_PLEOS_SDK) {
            val missing = listOf(AaosCarSignals.PERMISSION_SPEED, PERMISSION_LOCATION)
                .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
            if (missing.isNotEmpty()) runtimePermissions.launch(missing.toTypedArray())
        }
        if (BuildConfig.DEBUG) {
            ContextCompat.registerReceiver(this, debugReceiver, IntentFilter(ACTION_DEBUG_ADD_DRIVING), ContextCompat.RECEIVER_EXPORTED)
        }
        setContent {
            CarRelayTheme {
                CarRelayApp(viewModel)
            }
        }
    }

    override fun onDestroy() {
        if (BuildConfig.DEBUG) unregisterReceiver(debugReceiver)
        super.onDestroy()
    }

    private companion object {
        const val PERMISSION_LOCATION = "pleos.car.permission.FUSED_LOCATION"
        const val ACTION_DEBUG_ADD_DRIVING = "com.carrelay.vehicle.DEBUG_ADD_DRIVING"
    }
}

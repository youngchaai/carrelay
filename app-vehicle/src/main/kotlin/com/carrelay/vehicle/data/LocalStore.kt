package com.carrelay.vehicle.data

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import com.carrelay.core.care.CareEvent
import com.carrelay.core.model.CarUsage
import com.carrelay.core.model.DriveSession
import com.carrelay.core.model.Driver
import com.carrelay.core.model.DriverCondition
import com.carrelay.core.model.Incident
import com.carrelay.core.model.Inspection
import com.carrelay.core.model.Memo
import com.carrelay.core.model.VehicleSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

/** 보호자 알림 기록. 서버 연동 전에는 차량 앱 안에만 남깁니다. */
@Serializable
data class GuardianAlert(
    val driverId: String,
    val atMillis: Long,
    val message: String,
)

/**
 * 앱 전체 저장 데이터. 네트워크가 끊겨도, 앱이 종료·재시작돼도 메모와 기록이 남도록 기기에 저장합니다.
 * (P1: 동기화 서버로 파티원 폰과 공유)
 */
@Serializable
data class AppData(
    val drivers: List<Driver> = emptyList(),
    val memos: List<Memo> = emptyList(),
    /** 운전자별 '마지막으로 확인한(넘겨준) 시점'의 차량 상태 */
    val lastSeen: Map<String, VehicleSnapshot> = emptyMap(),
    val sessions: List<DriveSession> = emptyList(),
    /** 재시작 복원용: 진행 중이던 운전 구간 */
    val activeSession: DriveSession? = null,
    val lastDriverId: String? = null,
    val careEvents: List<CareEvent> = emptyList(),
    /** 폰 앱(Health Connect)에서 받은 컨디션 요약. 시연에서는 샘플 데이터. */
    val conditions: Map<String, DriverCondition> = emptyMap(),
    val guardianAlerts: List<GuardianAlert> = emptyList(),
    /** 시연용 빠른 판단(케어 모드 임계값을 초 단위로 줄임) */
    val demoFastThresholds: Boolean = false,
    /** 사고·파손 기록 (처리 완료 전까지 모든 운전자에게 표시) */
    val incidents: List<Incident> = emptyList(),
    /** 인수 점검 결과 */
    val inspections: List<Inspection> = emptyList(),
    /** 이 차량의 이름 (회사 차는 '업무 1호차' 처럼) */
    val vehicleName: String = "우리 집 그랜저",
    /** 가족 차 / 회사 차. 회사 차는 운행 목적을 받아 운행기록부를 자동으로 채웁니다. */
    val usage: CarUsage = CarUsage.FAMILY,
    /** 파티에서 삭제한 사람. 지난 운전 기록·운행기록부·메모에 이름을 그대로 보여 주려고 남깁니다. */
    val formerDrivers: List<Driver> = emptyList(),
)

class LocalStore(context: Context) {

    private val file = AtomicFile(File(context.filesDir, "carrelay.json"))
    private val saveLock = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun load(): AppData = withContext(Dispatchers.IO) {
        try {
            // readFully 는 쓰기 도중 종료된 경우 백업 파일에서 복구합니다.
            // (예전에는 면허 값이 빈 시연용 프로필에 샘플 값을 채웠는데, 이제 사람이 프로필에서 면허를 '모름'으로
            //  지울 수 있으므로 저장된 값을 그대로 씁니다.)
            json.decodeFromString(AppData.serializer(), file.readFully().decodeToString())
        } catch (e: FileNotFoundException) {
            SeedData.initial() // 첫 실행
        } catch (e: Exception) {
            Log.e(TAG, "저장 데이터 읽기 실패, 기본값으로 시작", e)
            SeedData.initial()
        }
    }

    /** 저장 요청이 겹쳐도 들어온 순서대로 하나씩 씁니다. */
    suspend fun save(data: AppData): Unit = saveLock.withLock { write(data) }

    private suspend fun write(data: AppData): Unit = withContext(Dispatchers.IO) {
        var out: FileOutputStream? = null
        try {
            out = file.startWrite()
            out.write(json.encodeToString(AppData.serializer(), data).encodeToByteArray())
            file.finishWrite(out)
        } catch (e: Exception) {
            out?.let { file.failWrite(it) }
            Log.e(TAG, "저장 실패", e)
        }
    }

    private companion object {
        const val TAG = "LocalStore"
    }
}

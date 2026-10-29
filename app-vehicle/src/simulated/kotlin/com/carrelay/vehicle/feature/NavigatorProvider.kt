package com.carrelay.vehicle.feature

import android.content.Context

/** carrelay.pleosSdk=false (기본): 실제 쉼터 목록에서 고르되 내비 연동은 흉내만 냅니다. */
object NavigatorProvider {
    @Suppress("UNUSED_PARAMETER")
    fun create(context: Context, catalog: RestStopCatalog): RestStopNavigator = SimulatedRestStopNavigator(catalog)
}

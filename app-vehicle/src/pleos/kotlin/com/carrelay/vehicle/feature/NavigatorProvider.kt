package com.carrelay.vehicle.feature

import android.content.Context

/** carrelay.pleosSdk=true: Pleos NaviHelper·FusedLocation 으로 실제 경유지를 추가합니다. */
object NavigatorProvider {
    fun create(context: Context, catalog: RestStopCatalog): RestStopNavigator =
        PleosRestStopNavigator(context.applicationContext, catalog)
}

package com.tryo.dashboard.feature.usage

import com.tryo.dashboard.core.network.DashboardApi
import org.json.JSONObject

object UsageRepository {
    fun load(key: String, callback: (JSONObject?, String?) -> Unit) {
        DashboardApi.loadFirestoreUsage(key, callback)
    }
}

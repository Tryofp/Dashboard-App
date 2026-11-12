package com.tryo.dashboard.feature.tracker

import com.tryo.dashboard.core.network.DashboardApi
import org.json.JSONObject

object TrackerRepository {
    fun load(key: String, offset: Int, limit: Int, callback: (JSONObject?, String?) -> Unit) {
        DashboardApi.loadLogs(key, "tracker", offset, limit, callback)
    }
}

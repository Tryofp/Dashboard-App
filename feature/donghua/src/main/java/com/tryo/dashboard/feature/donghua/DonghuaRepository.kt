package com.tryo.dashboard.feature.donghua

import com.tryo.dashboard.core.network.DashboardApi
import org.json.JSONObject

data class ProgressItem(
    val title: String,
    val episode: Int,
    val type: String,
    val status: String,
    val updatedAt: String
)

object DonghuaRepository {
    fun load(key: String, type: String, callback: (List<ProgressItem>?, String?) -> Unit) {
        DashboardApi.loadDonghuaProgress(key, type) { result, error ->
            if (error != null || result?.optString("status") != "success") {
                callback(null, error ?: result?.optString("message", "Gagal memuat daftar.") ?: "Gagal memuat daftar.")
                return@loadDonghuaProgress
            }
            val items = mutableListOf<ProgressItem>()
            val rows = result.optJSONArray("items")
            if (rows != null) {
                for (index in 0 until rows.length()) {
                    val row = rows.optJSONObject(index) ?: continue
                    items.add(
                        ProgressItem(
                            title = row.optString("title"),
                            episode = row.optInt("episode", 0),
                            type = row.optString("type", "donghua"),
                            status = row.optString("status"),
                            updatedAt = row.optString("updatedAt")
                        )
                    )
                }
            }
            callback(items, null)
        }
    }

    fun save(key: String, method: String, title: String, episode: Int, type: String, status: String,
             callback: (JSONObject?, String?) -> Unit) {
        DashboardApi.saveDonghuaProgress(key, method, title, episode, type, status, callback)
    }

    fun delete(key: String, title: String, type: String, callback: (JSONObject?, String?) -> Unit) {
        DashboardApi.deleteDonghuaProgress(key, title, type, callback)
    }
}
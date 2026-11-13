package com.tryo.dashboard.core.network

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Fetches the reviewed, versioned module manifest from the dashboard-v2 branch.
 * Failure is non-fatal: callers retain the built-in safe defaults.
 */
object RemoteModuleConfig {
    private const val MANIFEST_URL =
        "https://raw.githubusercontent.com/Tryofp/Dashboard-App/dashboard-v2/remote/modules.json"
    private val executor = Executors.newSingleThreadExecutor()

    data class Config(
        val appId: String,
        val minimumVersionCode: Int,
        val enabledModules: Set<String>,
        val moduleNames: Map<String, String>
    )

    fun fetch(callback: (Config?, String?) -> Unit) {
        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(MANIFEST_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 12000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Cache-Control", "no-cache")
                }
                val code = connection.responseCode
                if (code !in 200..299) {
                    callback(null, "Manifest HTTP $code")
                    return@execute
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                if (json.optInt("schemaVersion", 0) != 1) {
                    callback(null, "Versi manifest tidak didukung")
                    return@execute
                }
                val app = json.optJSONObject("app") ?: JSONObject()
                val modules = json.optJSONArray("modules")
                val enabled = linkedSetOf<String>()
                val names = linkedMapOf<String, String>()
                if (modules != null) {
                    for (index in 0 until modules.length()) {
                        val item = modules.optJSONObject(index) ?: continue
                        val id = item.optString("id").trim()
                        if (id.isNotEmpty()) {
                            names[id] = item.optString("name", id)
                            if (item.optBoolean("enabled", false)) enabled.add(id)
                        }
                    }
                }
                callback(
                    Config(
                        appId = app.optString("id"),
                        minimumVersionCode = app.optInt("minimumVersionCode", 1),
                        enabledModules = enabled,
                        moduleNames = names
                    ),
                    null
                )
            } catch (error: Exception) {
                callback(null, error.localizedMessage ?: "Manifest tidak dapat dimuat")
            } finally {
                connection?.disconnect()
            }
        }
    }
}

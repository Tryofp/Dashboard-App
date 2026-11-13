package com.tryo.dashboard.core.network

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

object DashboardApi {
    private const val ENDPOINT = "https://script.google.com/macros/s/AKfycbzGeuYqqiwf0ZJcbevv6ra7gTEAzK0YO9h5g2-pWQB_TG06dbIvFiTdPHJ0w2Uf61kQBw/exec"
    private val executor = Executors.newSingleThreadExecutor()

    fun authenticate(key: String, callback: (JSONObject?, String?) -> Unit) {
        post(JSONObject().put("Type", "DashboardAuth").put("Key", key), callback)
    }

    fun loadLogs(key: String, tab: String, offset: Int, limit: Int, callback: (JSONObject?, String?) -> Unit) {
        post(
            JSONObject()
                .put("Type", "DashboardLogs")
                .put("Key", key)
                .put("Tab", tab)
                .put("Offset", offset)
                .put("Limit", limit),
            callback
        )
    }

    fun loadFirestoreUsage(key: String, callback: (JSONObject?, String?) -> Unit) {
        post(
            JSONObject()
                .put("Type", "DashboardFirestoreUsage")
                .put("Key", key),
            callback
        )
    }

    fun loadDonghuaProgress(key: String, type: String, callback: (JSONObject?, String?) -> Unit) {
        post(
            JSONObject()
                .put("Type", "DashboardDonghuaList")
                .put("Key", key)
                .put("ProgressType", type),
            callback
        )
    }

    fun saveDonghuaProgress(
        key: String, method: String, title: String, episode: Int, type: String, status: String,
        callback: (JSONObject?, String?) -> Unit
    ) {
        post(
            JSONObject()
                .put("Type", "DashboardDonghuaProgress")
                .put("Key", key)
                .put("Method", method)
                .put("Title", title)
                .put("Episode", episode)
                .put("ProgressType", type)
                .put("Status", status),
            callback
        )
    }

    fun deleteDonghuaProgress(
        key: String, title: String, type: String, callback: (JSONObject?, String?) -> Unit
    ) {
        post(
            JSONObject()
                .put("Type", "DashboardDonghuaProgress")
                .put("Key", key)
                .put("Method", "DELETE")
                .put("Title", title)
                .put("ProgressType", type),
            callback
        )
    }

    private fun post(payload: JSONObject, callback: (JSONObject?, String?) -> Unit) {
        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20000
                    readTimeout = 25000
                    doOutput = true
                    instanceFollowRedirects = false
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                }
                connection.outputStream.use {
                    it.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
                }

                val code = connection.responseCode
                val location = connection.getHeaderField("Location")
                val responseText: String = if (code in 300..399 && !location.isNullOrBlank()) {
                    connection.disconnect()
                    connection = (URL(location).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 20000
                        readTimeout = 25000
                        setRequestProperty("Accept", "application/json")
                    }
                    val followCode = connection.responseCode
                    val stream = if (followCode in 200..299) connection.inputStream else connection.errorStream
                    stream?.bufferedReader()?.use { reader -> reader.readText() }.orEmpty()
                } else {
                    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                    stream?.bufferedReader()?.use { reader -> reader.readText() }.orEmpty()
                }

                val json = runCatching { JSONObject(responseText) }.getOrNull()
                if (json == null) callback(null, "Respons server tidak dapat dibaca (HTTP $code).")
                else callback(json, null)
            } catch (error: Exception) {
                callback(null, error.localizedMessage ?: "Koneksi server gagal.")
            } finally {
                connection?.disconnect()
            }
        }
    }
}

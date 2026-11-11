package com.tryofp.pluginnotifications

import android.content.Context
import android.os.Build
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

object FcmRegistration {
    private const val ENDPOINT = "https://script.google.com/macros/s/AKfycbxJzU3MiL_o9PjkF4h-prSw-P8vLVfJqGFXM7pFJOPzIzJXaGmu6z6oK0oJl54a6bQy0Q/exec"
    private val executor = Executors.newSingleThreadExecutor()

    fun register(context: Context, token: String, callback: (Boolean, String) -> Unit) {
        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                val agent = "Android/" + Build.VERSION.RELEASE + " " + Build.MODEL
                val payload = """{"Type":"FcmRegister","Token":${jsonString(token)},"UserAgent":${jsonString(agent)},"Platform":"android","AppVersion":"1.0.0"}"""
                connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 20000
                    doOutput = true
                    instanceFollowRedirects = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                }
                connection.outputStream.use { it.write(payload.toByteArray(StandardCharsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val ok = code in 200..299 && response.contains("\"status\":\"success\"")
                callback(ok, if (ok) "registered" else "HTTP $code: ${response.take(180)}")
            } catch (error: Exception) {
                callback(false, error.localizedMessage ?: "Unknown network error")
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun jsonString(value: String): String {
        return "\"" + value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r") + "\""
    }
}

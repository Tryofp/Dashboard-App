package com.tryo.dashboard

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

internal data class DashboardUpdate(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val releaseNotes: String
)

internal object UpdateManager {
    private const val MANIFEST_URL =
        "https://raw.githubusercontent.com/Tryofp/Dashboard-App/dashboard-v2/remote/update.json"
    private const val MAX_APK_BYTES = 100L * 1024L * 1024L
    private val executor = Executors.newSingleThreadExecutor()

    fun checkForUpdate(activity: Activity, callback: (DashboardUpdate?) -> Unit) {
        executor.execute {
            var connection: HttpURLConnection? = null
            val update = try {
                connection = (URL(MANIFEST_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                    requestMethod = "GET"
                    setRequestProperty("Cache-Control", "no-cache")
                }
                if (connection.responseCode !in 200..299) throw IllegalStateException("Update server unavailable")
                val json = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                val latestCode = json.optLong("versionCode", 0L)
                val latestName = json.optString("versionName", "")
                val apkUrl = json.optString("apkUrl", "")
                val hash = json.optString("sha256", "").lowercase()
                val notes = json.optString("releaseNotes", "Pembaruan Tryo Dashboard tersedia.")
                val currentCode = if (Build.VERSION.SDK_INT >= 28) {
                    activity.packageManager.getPackageInfo(activity.packageName, 0).longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    activity.packageManager.getPackageInfo(activity.packageName, 0).versionCode.toLong()
                }
                if (latestCode <= currentCode || latestName.isBlank() ||
                    !isApprovedReleaseUrl(apkUrl) || !hash.matches(Regex("[a-f0-9]{64}"))
                ) null else DashboardUpdate(latestCode, latestName, apkUrl, hash, notes)
            } catch (_: Exception) {
                null
            } finally {
                connection?.disconnect()
            }
            if (!activity.isFinishing && !activity.isDestroyed) {
                activity.runOnUiThread { callback(update) }
            }
        }
    }

    fun showUpdateDialog(activity: Activity, update: DashboardUpdate) {
        AlertDialog.Builder(activity)
            .setTitle("Update Tryo Dashboard tersedia")
            .setMessage("Versi ${update.versionName} sudah tersedia.\n\n${update.releaseNotes}\n\nUnduh dan pasang sekarang?")
            .setPositiveButton("Update") { _, _ -> downloadAndInstall(activity, update) }
            .setNegativeButton("Nanti", null)
            .setCancelable(true)
            .show()
    }

    private fun downloadAndInstall(activity: Activity, update: DashboardUpdate) {
        if (Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(activity, "Izinkan pemasangan aplikasi untuk Tryo Dashboard, lalu buka ulang aplikasi.", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivity(intent)
            return
        }

        Toast.makeText(activity, "Mengunduh update…", Toast.LENGTH_SHORT).show()
        executor.execute {
            var connection: HttpURLConnection? = null
            val apk = File(File(activity.cacheDir, "updates").apply { mkdirs() }, "tryo-dashboard-update.apk")
            try {
                connection = openApprovedDownloadConnection(update.apkUrl)
                val declaredLength = connection.contentLength.toLong()
                if (declaredLength > MAX_APK_BYTES) throw SecurityException("Ukuran APK melebihi batas yang diizinkan.")
                val digest = MessageDigest.getInstance("SHA-256")
                var totalBytes = 0L
                connection.inputStream.use { input ->
                    apk.outputStream().use { output ->
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            totalBytes += count
                            if (totalBytes > MAX_APK_BYTES) throw SecurityException("Ukuran APK melebihi batas yang diizinkan.")
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                        }
                    }
                }
                val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
                if (!actualHash.equals(update.sha256, ignoreCase = true)) {
                    apk.delete()
                    throw SecurityException("Verifikasi file update gagal. APK tidak dipasang.")
                }
                if (totalBytes == 0L) throw SecurityException("File APK kosong.")
                activity.runOnUiThread {
                    if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                    val uri = FileProvider.getUriForFile(
                        activity,
                        "${activity.packageName}.fileprovider",
                        apk
                    )
                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    activity.startActivity(installIntent)
                }
            } catch (error: Exception) {
                apk.delete()
                activity.runOnUiThread {
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        Toast.makeText(
                            activity,
                            error.message ?: "Gagal mengunduh update.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun isApprovedReleaseUrl(value: String): Boolean {
        return try {
            val uri = java.net.URI(value)
            uri.scheme == "https" &&
                uri.host.equals("github.com", ignoreCase = true) &&
                uri.path.startsWith("/Tryofp/Dashboard-App/releases/download/") &&
                uri.userInfo == null
        } catch (_: Exception) {
            false
        }
    }

    private fun openApprovedDownloadConnection(value: String): HttpURLConnection {
        var currentUrl = URL(value)
        repeat(6) { hop ->
            val connection = (currentUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "Tryo-Dashboard-Updater")
                setRequestProperty("Accept", "application/vnd.android.package-archive")
            }
            val code = connection.responseCode
            if (code in 200..299) return connection
            if (code !in listOf(301, 302, 303, 307, 308)) {
                connection.disconnect()
                throw IllegalStateException("Unduhan gagal (HTTP $code).")
            }
            val location = connection.getHeaderField("Location")
            connection.disconnect()
            if (location.isNullOrBlank() || hop == 5) throw SecurityException("Redirect unduhan tidak valid.")
            val next = URL(currentUrl, location)
            val host = next.host.lowercase()
            val approvedHost = host == "github.com" ||
                host == "release-assets.githubusercontent.com" ||
                host == "objects.githubusercontent.com"
            if (next.protocol != "https" || !approvedHost || next.userInfo != null) {
                throw SecurityException("Server pengalihan unduhan tidak diizinkan.")
            }
            currentUrl = next
        }
        throw SecurityException("Terlalu banyak pengalihan unduhan.")
    }
}

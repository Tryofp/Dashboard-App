package com.tryo.dashboard.feature.pluginmanager

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class PluginCatalogEntry(
    val name: String,
    val path: String,
    val category: String,
    val revision: String,
    val sizeBytes: Long,
    val rawUrl: String,
    val githubUrl: String
)

object PluginCatalogRepository {
    private const val TREE_URL = "https://api.github.com/repos/Tryofp/Plugin-Manager/git/trees/main?recursive=1"
    private const val CACHE_DURATION_MS = 5 * 60 * 1000L
    private val executor = Executors.newSingleThreadExecutor()

    @Volatile private var cachedEntries: List<PluginCatalogEntry>? = null
    @Volatile private var cachedAt = 0L

    fun load(forceRefresh: Boolean = false, callback: (List<PluginCatalogEntry>?, String?) -> Unit) {
        executor.execute {
            val currentCache = cachedEntries
            if (!forceRefresh && currentCache != null &&
                System.currentTimeMillis() - cachedAt < CACHE_DURATION_MS
            ) {
                callback(currentCache, null)
                return@execute
            }

            var connection: HttpURLConnection? = null
            try {
                connection = (URL(TREE_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 20000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "Tryo-Dashboard-Android")
                }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) throw IllegalStateException("GitHub mengembalikan HTTP $code.")

                val json = JSONObject(body)
                if (json.optBoolean("truncated", false)) {
                    throw IllegalStateException("Daftar repositori terlalu besar untuk dimuat sekaligus.")
                }
                val tree = json.optJSONArray("tree")
                    ?: throw IllegalStateException("Struktur katalog GitHub tidak valid.")
                val entries = ArrayList<PluginCatalogEntry>()

                for (index in 0 until tree.length()) {
                    val item = tree.optJSONObject(index) ?: continue
                    val path = item.optString("path")
                    val isPlugin = path.startsWith("Plugin/")
                    val isTool = path.startsWith("Tools/")
                    if ((!isPlugin && !isTool) || !path.endsWith(".lua", true) ||
                        item.optString("type") != "blob"
                    ) continue

                    val encodedPath = path.split("/").joinToString("/") { segment ->
                        java.net.URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
                    }
                    entries.add(
                        PluginCatalogEntry(
                            name = path.substringAfterLast("/").removeSuffix(".lua"),
                            path = path,
                            category = if (isPlugin) "Plugin" else "Tools",
                            revision = item.optString("sha").take(7),
                            sizeBytes = item.optLong("size", 0L),
                            rawUrl = "https://raw.githubusercontent.com/Tryofp/Plugin-Manager/refs/heads/main/$encodedPath",
                            githubUrl = "https://github.com/Tryofp/Plugin-Manager/blob/main/$encodedPath"
                        )
                    )
                }

                val sorted = entries.sortedWith(compareBy<PluginCatalogEntry> { it.category }.thenBy { it.name.lowercase() })
                cachedEntries = sorted
                cachedAt = System.currentTimeMillis()
                callback(sorted, null)
            } catch (error: Exception) {
                callback(null, error.localizedMessage ?: "Katalog plugin gagal dimuat.")
            } finally {
                connection?.disconnect()
            }
        }
    }
}

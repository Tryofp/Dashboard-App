package com.tryo.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.tryo.dashboard.core.network.DashboardApi
import com.tryo.dashboard.core.network.RemoteModuleConfig
import com.tryo.dashboard.core.security.DashboardKeyStore
import com.tryo.dashboard.core.notifications.FcmRegistration
import com.tryo.dashboard.feature.pluginmanager.PluginManagerFeature
import com.tryo.dashboard.feature.pluginmanager.PluginManagerScreen
import com.tryo.dashboard.feature.tracker.TrackerFeature
import com.tryo.dashboard.feature.tracker.TrackerScreen
import com.tryo.dashboard.feature.tracker.TrackerRepository
import com.tryo.dashboard.feature.usage.UsageFeature
import com.tryo.dashboard.feature.usage.UsageDataScreen
import com.tryo.dashboard.feature.usage.UsageRepository
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {
    private val bg = Color.rgb(16, 24, 39)
    private val panel = Color.rgb(30, 41, 59)
    private val muted = Color.rgb(166, 180, 198)
    private val white = Color.WHITE
    private var dashboardKey: String? = null
    private var selectedTab = PluginManagerFeature.ID
    private var remoteEnabledModules = setOf(PluginManagerFeature.ID, TrackerFeature.ID, UsageFeature.ID)
    private var offset = 0
    private var total = 0
    private var hasMore = false
    private var loading = false
    private val usageRefreshHandler = Handler(Looper.getMainLooper())
    private val usageRefreshRunnable = object : Runnable {
        override fun run() {
            if (selectedTab == "usage" && !isFinishing) {
                loadUsage()
                usageRefreshHandler.postDelayed(this, 5 * 60 * 1000L)
            }
        }
    }
    private lateinit var root: LinearLayout
    private lateinit var notificationStatus: TextView
    private lateinit var logContainer: LinearLayout
    private lateinit var logStatus: TextView
    private lateinit var loadMoreButton: Button
    private lateinit var managerTab: Button
    private lateinit var trackerTab: Button
    private lateinit var feedbackTab: Button
    private lateinit var usageTab: Button
    private lateinit var hubCountView: TextView
    private lateinit var feedbackCountView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        TryoMessagingService.ensureChannel(this)
        requestNotificationPermissionIfNeeded()
        refreshAndRegisterToken()
        dashboardKey = DashboardKeyStore.read(this)
        if (dashboardKey.isNullOrBlank()) showLogin() else showDashboard()
        refreshRemoteModuleConfig()
    }

    override fun onDestroy() {
        usageRefreshHandler.removeCallbacks(usageRefreshRunnable)
        super.onDestroy()
    }

    private fun refreshRemoteModuleConfig() {
        RemoteModuleConfig.fetch { config, error ->
            runOnUiThread {
                if (config == null) {
                    // Remote config is optional; keep the last-known safe defaults.
                    return@runOnUiThread
                }
                if (config.appId != packageName) return@runOnUiThread
                val currentVersion = if (Build.VERSION.SDK_INT >= 28) {
                    packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, 0).versionCode
                }
                if (config.minimumVersionCode > currentVersion) {
                    Toast.makeText(this, "Dashboard App perlu diperbarui.", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                remoteEnabledModules = config.enabledModules
                if (::trackerTab.isInitialized) {
                    managerTab.visibility = if ("plugin-manager" in remoteEnabledModules) View.VISIBLE else View.GONE
                    trackerTab.visibility = if ("tracker" in remoteEnabledModules) View.VISIBLE else View.GONE
                    usageTab.visibility = if ("usage-data" in remoteEnabledModules) View.VISIBLE else View.GONE
                    if (selectedTab == "plugin-manager" && "plugin-manager" !in remoteEnabledModules) {
                        switchTab(if ("tracker" in remoteEnabledModules) "tracker" else "feedback")
                    } else if (selectedTab == "tracker" && "tracker" !in remoteEnabledModules) {
                        switchTab(if ("plugin-manager" in remoteEnabledModules) "plugin-manager" else "feedback")
                    } else if (selectedTab == "usage" && "usage-data" !in remoteEnabledModules) {
                        switchTab(
                            when {
                                "plugin-manager" in remoteEnabledModules -> "plugin-manager"
                                "tracker" in remoteEnabledModules -> "tracker"
                                else -> "feedback"
                            }
                        )
                    }
                    updateTabButtons()
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4101)
        }
    }

    private fun refreshAndRegisterToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                FcmRegistration.register(applicationContext, token) { success, message ->
                    runOnUiThread {
                        if (::notificationStatus.isInitialized) {
                            notificationStatus.text = if (success) "● Notifikasi aktif" else "● Notifikasi: $message"
                        }
                    }
                }
            }
            .addOnFailureListener { error ->
                runOnUiThread {
                    if (::notificationStatus.isInitialized) notificationStatus.text = "● Firebase: ${error.localizedMessage}"
                }
            }
    }

    private fun showLogin() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(36), dp(28), dp(36))
            setBackgroundColor(bg)
        }
        root.addView(text("TRYO PLUGIN MANAGER", 23f, white, true, Gravity.CENTER))
        root.addView(text("Dashboard Log", 15f, muted, false, Gravity.CENTER).apply {
            setPadding(0, dp(8), 0, dp(26))
        })
        val keyInput = EditText(this).apply {
            hint = "Masukkan kunci akses dashboard"
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 15f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            background = rounded(panel, 12)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        root.addView(keyInput, LinearLayout.LayoutParams(-1, dp(54)))
        val loginButton = button("Masuk Dashboard")
        val buttonParams = LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(14) }
        root.addView(loginButton, buttonParams)
        val message = text("Kunci hanya disimpan terenkripsi di perangkat ini.", 12f, muted, false, Gravity.CENTER)
        root.addView(message.apply { setPadding(0, dp(18), 0, 0) })
        loginButton.setOnClickListener {
            val key = keyInput.text?.toString()?.trim().orEmpty()
            if (key.length < 40) {
                message.text = "Kunci akses tidak valid."
                return@setOnClickListener
            }
            loginButton.isEnabled = false
            loginButton.text = "Memverifikasi…"
            message.text = "Menghubungkan ke server…"
            DashboardApi.authenticate(key) { result, error ->
                runOnUiThread {
                    loginButton.isEnabled = true
                    loginButton.text = "Masuk Dashboard"
                    if (error == null && result?.optString("status") == "success") {
                        DashboardKeyStore.save(this, key)
                        dashboardKey = key
                        showDashboard()
                    } else {
                        message.text = error ?: result?.optString("message", "Autentikasi gagal.") ?: "Autentikasi gagal."
                    }
                }
            }
        }
        setContentView(root)
        applySystemInsets(root, 28, 36, 28, 36)
    }

    private fun showDashboard() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(14))
            setBackgroundColor(bg)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val heading = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        heading.addView(text("TRYO PLUGIN MANAGER", 18f, white, true))
        notificationStatus = text("● Menghubungkan notifikasi…", 11f, muted)
        heading.addView(notificationStatus)
        header.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))
        val logout = button("Kunci")
        header.addView(logout, LinearLayout.LayoutParams(dp(72), dp(38)))
        logout.setOnClickListener {
            DashboardKeyStore.clear(this)
            dashboardKey = null
            showLogin()
        }
        root.addView(header)

        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(18), 0, dp(16))
        }
        val hubStat = statCard("TOTAL HUB LOAD", "…")
        val feedbackStat = statCard("TOTAL FEEDBACK", "…")
        hubCountView = hubStat.findViewWithTag("stat_value_TOTAL HUB LOAD")
        feedbackCountView = feedbackStat.findViewWithTag("stat_value_TOTAL FEEDBACK")
        stats.addView(hubStat, LinearLayout.LayoutParams(0, dp(76), 1f).apply { marginEnd = dp(6) })
        stats.addView(feedbackStat, LinearLayout.LayoutParams(0, dp(76), 1f).apply { marginStart = dp(6) })
        root.addView(stats)

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(12))
        }
        managerTab = button("Plugin Manager")
        trackerTab = button("Tracker")
        feedbackTab = button("Feedback")
        usageTab = button("Usage")
        tabs.addView(managerTab, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(2) })
        tabs.addView(trackerTab, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(2); marginEnd = dp(2) })
        tabs.addView(feedbackTab, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(2); marginEnd = dp(2) })
        tabs.addView(usageTab, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(2) })
        root.addView(tabs)
        managerTab.setOnClickListener { switchTab("plugin-manager") }
        trackerTab.setOnClickListener { switchTab("tracker") }
        feedbackTab.setOnClickListener { switchTab("feedback") }
        usageTab.setOnClickListener { switchTab("usage") }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        logStatus = text("Memuat riwayat…", 12f, muted)
        toolbar.addView(logStatus, LinearLayout.LayoutParams(0, dp(38), 1f))
        val refresh = button("Refresh")
        toolbar.addView(refresh, LinearLayout.LayoutParams(dp(88), dp(38)))
        refresh.setOnClickListener { when (selectedTab) { "usage" -> loadUsage(); "plugin-manager" -> loadPluginManager(); else -> loadLogs(true) } }
        root.addView(toolbar)

        val scroll = ScrollView(this).apply {
            isFillViewport = false
            clipToPadding = false
        }
        logContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(8))
        }
        scroll.addView(logContainer)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        loadMoreButton = button("Muat Lebih Banyak")
        root.addView(loadMoreButton, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(8) })
        loadMoreButton.setOnClickListener { loadLogs(false) }

        setContentView(root)
        applySystemInsets(root, 18, 20, 18, 14)
        updateTabButtons()
        loadStatCounts()
        loadPluginManager()
    }

    private fun statCard(label: String, value: String): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(13), dp(10), dp(13), dp(10))
            background = rounded(panel, 12)
        }
        card.addView(text(label, 10f, muted, true))
        card.addView(text(value, 22f, white, true).apply { tag = "stat_value_${label}" })
        return card
    }

    private fun loadStatCounts() {
        val key = dashboardKey ?: return
        TrackerRepository.load(key, 0, 1) { result, _ ->
            runOnUiThread {
                if (result?.optString("status") == "success" && ::hubCountView.isInitialized) {
                    hubCountView.text = result.optInt("total", 0).toString()
                }
            }
        }
        DashboardApi.loadLogs(key, "feedback", 0, 1) { result, _ ->
            runOnUiThread {
                if (result?.optString("status") == "success" && ::feedbackCountView.isInitialized) {
                    feedbackCountView.text = result.optInt("total", 0).toString()
                }
            }
        }
    }

    private fun switchTab(tab: String) {
        if (selectedTab == tab) return
        selectedTab = tab
        updateTabButtons()
        usageRefreshHandler.removeCallbacks(usageRefreshRunnable)
        if (tab == "usage") {
            loadUsage()
            usageRefreshHandler.postDelayed(usageRefreshRunnable, 5 * 60 * 1000L)
        } else if (tab == "plugin-manager") {
            loadPluginManager()
        } else {
            loadLogs(true)
        }
    }

    private fun updateTabButtons() {
        if (!::managerTab.isInitialized || !::trackerTab.isInitialized || !::feedbackTab.isInitialized || !::usageTab.isInitialized) return
        managerTab.background = rounded(if (selectedTab == "plugin-manager") Color.rgb(37, 99, 235) else panel, 10)
        trackerTab.background = rounded(if (selectedTab == "tracker") Color.rgb(37, 99, 235) else panel, 10)
        feedbackTab.background = rounded(if (selectedTab == "feedback") Color.rgb(37, 99, 235) else panel, 10)
        usageTab.background = rounded(if (selectedTab == "usage") Color.rgb(37, 99, 235) else panel, 10)
    }

    private fun loadPluginManager() {
        loading = false
        logContainer.removeAllViews()
        loadMoreButton.visibility = View.GONE
        logStatus.text = "Pusat kontrol Dashboard"
        logContainer.addView(
            PluginManagerScreen.create(this, panel, white, muted, ::dp)
        )
    }

    private fun loadUsage() {
        if (loading) return
        val key = dashboardKey ?: return
        loading = true
        logStatus.text = "Menghitung estimasi operasi Firestore…"
        loadMoreButton.visibility = View.GONE
        UsageRepository.load(key) { result, error ->
            runOnUiThread {
                loading = false
                logContainer.removeAllViews()
                if (error != null || result?.optString("status") != "success") {
                    logStatus.text = "Firestore Usage tidak tersedia"
                    addEmptyState(error ?: result?.optString("message", "Gagal mengambil metrik.") ?: "Gagal mengambil metrik.")
                    return@runOnUiThread
                }

                val limits = result.optJSONObject("limits")
                val reads = result.optLong("reads", 0L)
                val writes = result.optLong("writes", 0L)
                val deletes = result.optLong("deletes", 0L)
                val readLimit = limits?.optLong("reads", 50000L) ?: 50000L
                val writeLimit = limits?.optLong("writes", 20000L) ?: 20000L
                val deleteLimit = limits?.optLong("deletes", 20000L) ?: 20000L

                logStatus.text = "Estimasi penggunaan • " + result.optString("period", "WIB")
                logContainer.addView(UsageDataScreen.metricCard(this, "ESTIMATED DOCUMENT READS", reads, readLimit, panel, white, muted, ::dp))
                logContainer.addView(UsageDataScreen.metricCard(this, "ESTIMATED DOCUMENT WRITES", writes, writeLimit, panel, white, muted, ::dp))
                logContainer.addView(UsageDataScreen.metricCard(this, "ESTIMATED DOCUMENT DELETES", deletes, deleteLimit, panel, white, muted, ::dp))
                val storage = result.optJSONObject("storage")
                if (storage != null && storage.optBoolean("available", true)) {
                    logContainer.addView(UsageDataScreen.storageCard(this, storage.optLong("bytes", 0L), limits?.optLong("storageBytes", 1073741824L) ?: 1073741824L, panel, white, muted, ::dp))
                } else {
                    logContainer.addView(text("DATABASE STORAGE\nTidak dihitung dalam mode estimasi aplikasi.", 12f, muted).apply {
                        setPadding(dp(4), dp(10), dp(4), dp(12))
                    })
                }
                logContainer.addView(text("Terakhir diperbarui: " + formatTime(result.optString("updatedAt", "")), 11f, muted).apply {
                    setPadding(dp(4), dp(10), dp(4), dp(3))
                })
                logContainer.addView(text(result.optString("note", "Metrik dapat mengalami keterlambatan."), 11f, muted).apply {
                    setPadding(dp(4), dp(2), dp(4), dp(12))
                })
            }
        }
    }


    private fun loadLogs(reset: Boolean) {
        if (loading) return
        val key = dashboardKey ?: return
        if (reset) {
            offset = 0
            total = 0
            hasMore = false
            logContainer.removeAllViews()
        }
        loading = true
        logStatus.text = if (reset) "Memuat data…" else "Memuat data berikutnya…"
        loadMoreButton.visibility = View.GONE
        val callback: (JSONObject?, String?) -> Unit = { result, error ->
            runOnUiThread {
                loading = false
                if (error != null || result?.optString("status") != "success") {
                    logStatus.text = error ?: result?.optString("message", "Gagal memuat log.") ?: "Gagal memuat log."
                    if (offset == 0) addEmptyState("Data belum dapat dimuat. Periksa koneksi lalu tekan Refresh.")
                    return@runOnUiThread
                }
                total = result.optInt("total", 0)
                if (selectedTab == "tracker" && ::hubCountView.isInitialized) hubCountView.text = total.toString()
                if (selectedTab == "feedback" && ::feedbackCountView.isInitialized) feedbackCountView.text = total.toString()
                val records = result.optJSONArray("records")
                var added = 0
                if (records != null) {
                    for (i in 0 until records.length()) {
                        val record = records.optJSONObject(i) ?: continue
                        logContainer.addView(if (selectedTab == "tracker") TrackerScreen.recordCard(this, record, panel, white, muted, ::dp, ::formatTime) else feedbackCard(record))
                        added++
                    }
                }
                offset += added
                hasMore = result.optBoolean("hasMore", offset < total)
                logStatus.text = "Menampilkan ${offset} dari ${total} data"
                loadMoreButton.visibility = if (hasMore) View.VISIBLE else View.GONE
                if (offset == 0) addEmptyState(if (selectedTab == "tracker") "Belum ada log tracker." else "Belum ada feedback.")
            }
        }
        if (selectedTab == "tracker") {
            TrackerRepository.load(key, offset, 50, callback)
        } else {
            DashboardApi.loadLogs(key, selectedTab, offset, 50, callback)
        }
    }

    private fun feedbackCard(item: JSONObject): View {
        val card = cardColumn()
        val title = "${item.optString("Username", "Unknown")}  •  ${item.optInt("Rating", 0)}/5"
        card.addView(text(title, 16f, white, true))
        card.addView(text(item.optString("Category", "Other"), 11f, Color.rgb(96, 165, 250), true))
        card.addView(text(item.optString("Message", ""), 13f, white))
        card.addView(text("Game: ${item.optString("PlaceName", "-")}  •  Device: ${item.optString("Device", "-")}", 11f, muted))
        card.addView(text("User ID: ${item.optString("UserId", "-")}", 11f, muted))
        card.addView(text(formatTime(item.optString("Timestamp", "")), 11f, Color.rgb(96, 165, 250)))
        return card
    }

    private fun cardColumn(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(13), dp(14), dp(13))
        background = rounded(panel, 12)
        val params = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
        layoutParams = params
    }

    private fun addEmptyState(message: String) {
        if (logContainer.childCount > 0) return
        logContainer.addView(text(message, 13f, muted, false, Gravity.CENTER).apply {
            setPadding(dp(12), dp(35), dp(12), dp(35))
        })
    }

    private fun text(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false,
        gravity: Int = Gravity.START
    ): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        this.gravity = gravity
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(3), 0, dp(3))
    }

    private fun button(label: String): Button = Button(this).apply {
        text = label
        textSize = 12f
        isAllCaps = false
        setTextColor(white)
        background = rounded(Color.rgb(37, 99, 235), 10)
        stateListAnimator = null
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun applySystemInsets(content: View, left: Int, top: Int, right: Int, bottom: Int) {
        val baseLeft = dp(left)
        val baseTop = dp(top)
        val baseRight = dp(right)
        val baseBottom = dp(bottom)
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(
                baseLeft + safe.left,
                baseTop + safe.top,
                baseRight + safe.right,
                baseBottom + safe.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(content)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun formatTime(value: String): String {
        if (value.isBlank()) return "Waktu tidak tersedia"
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = input.parse(value) ?: return value
            SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale("id", "ID")).format(date)
        } catch (_: Exception) {
            value
        }
    }
}

package com.tryo.dashboard.feature.pluginmanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher

/**
 * Plugin Manager home and live read-only catalog.
 * The app lists the public Lua files and exposes their source URLs; it does not
 * pretend to toggle scripts inside an Infinite Yield runtime it cannot control.
 */
object PluginManagerScreen {
    fun create(
        context: Context,
        panelColor: Int,
        textColor: Int,
        mutedColor: Int,
        dp: (Int) -> Int,
        onNavigate: (String) -> Unit
    ): View {
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(text(context, "Selamat datang di Tryo Dashboard", 19f, textColor, true, dp).apply {
            setPadding(dp(4), dp(14), dp(4), dp(6))
        })
        content.addView(text(
            context,
            "Kelola katalog skrip Plugin Manager dan akses fitur dashboard.",
            13f, mutedColor, false, dp
        ).apply { setPadding(dp(4), 0, dp(4), dp(12)) })

        val catalogHeader = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        catalogHeader.addView(text(context, "KATALOG PLUGIN", 14f, textColor, true, dp),
            LinearLayout.LayoutParams(0, -2, 1f))
        val refreshButton = actionButton(context, "Refresh", dp)
        catalogHeader.addView(refreshButton, LinearLayout.LayoutParams(dp(84), dp(38)))
        content.addView(catalogHeader)

        val status = text(context, "Mengambil daftar dari GitHub…", 12f, mutedColor, false, dp).apply {
            setPadding(dp(4), dp(5), dp(4), dp(8))
        }
        content.addView(status)

        val search = EditText(context).apply {
            hint = "Cari nama plugin atau file…"
            textSize = 13f
            setSingleLine(true)
            setTextColor(textColor)
            setHintTextColor(mutedColor)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(panelColor, dp(10))
        }
        content.addView(search, LinearLayout.LayoutParams(-1, dp(46)).apply {
            bottomMargin = dp(8)
        })

        val filterRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val listContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        var allEntries: List<PluginCatalogEntry> = emptyList()
        var selectedCategory = "Semua"
        val filterButtons = linkedMapOf<String, Button>()

        fun renderEntries() {
            filterButtons.forEach { (label, button) ->
                button.background = rounded(
                    if (selectedCategory == label) 0xFF2563EB.toInt() else panelColor,
                    dp(9)
                )
            }
            listContainer.removeAllViews()
            val query = search.text?.toString()?.trim()?.lowercase().orEmpty()
            val filtered = allEntries.filter { entry ->
                (selectedCategory == "Semua" || entry.category == selectedCategory) &&
                    (query.isEmpty() || entry.name.lowercase().contains(query) ||
                        entry.path.lowercase().contains(query))
            }
            if (filtered.isEmpty()) {
                listContainer.addView(text(
                    context,
                    if (allEntries.isEmpty()) "Belum ada data plugin." else "Plugin tidak ditemukan.",
                    13f, mutedColor, false, dp
                ).apply {
                    gravity = Gravity.CENTER
                    setPadding(dp(8), dp(22), dp(8), dp(22))
                })
                return
            }
            filtered.forEach { entry ->
                listContainer.addView(pluginCard(
                    context, entry, panelColor, textColor, mutedColor, dp
                ))
            }
        }

        listOf("Semua", "Plugin", "Tools").forEach { label ->
            val filter = actionButton(context, label, dp)
            filterButtons[label] = filter
            filter.setOnClickListener {
                selectedCategory = label
                renderEntries()
            }
            filterRow.addView(filter, LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                if (label != "Tools") marginEnd = dp(6)
            })
        }
        content.addView(filterRow, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(10)
        })
        content.addView(listContainer)

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = renderEntries()
            override fun afterTextChanged(s: Editable?) = Unit
        })

        refreshButton.setOnClickListener {
            status.text = "Memperbarui katalog dari GitHub…"
            refreshButton.isEnabled = false
            PluginCatalogRepository.load(forceRefresh = true) { entries, error ->
                Handler(Looper.getMainLooper()).post {
                    refreshButton.isEnabled = true
                    if (error != null || entries == null) {
                        status.text = "Gagal memuat katalog: ${error ?: "respons kosong"}"
                        if (allEntries.isEmpty()) renderEntries()
                    } else {
                        allEntries = entries
                        status.text = "${entries.size} file tersedia • sumber: GitHub / main"
                        renderEntries()
                    }
                }
            }
        }

        PluginCatalogRepository.load { entries, error ->
            Handler(Looper.getMainLooper()).post {
                if (error != null || entries == null) {
                    status.text = "Gagal memuat katalog: ${error ?: "respons kosong"}"
                    renderEntries()
                } else {
                    allEntries = entries
                    status.text = "${entries.size} file tersedia • sumber: GitHub / main"
                    renderEntries()
                }
            }
        }

        content.addView(text(
            context,
            "Salin URL untuk mendapatkan tautan skrip mentah. Instalasi dan status aktif di dalam game memerlukan penghubung runtime Infinite Yield.",
            11f, mutedColor, false, dp
        ).apply { setPadding(dp(4), dp(10), dp(4), dp(14)) })

        content.addView(infoCard(
            context, "TRACKER", "Riwayat aktivitas dan event aplikasi • Buka",
            panelColor, textColor, mutedColor, dp
        ) { onNavigate("tracker") })
        content.addView(infoCard(
            context, "FEEDBACK", "Masukan dan penilaian pengguna • Buka",
            panelColor, textColor, mutedColor, dp
        ) { onNavigate("feedback") })
        content.addView(text(
            context,
            "Data operasional tetap melalui Apps Script dan Firestore.",
            11f, mutedColor, false, dp
        ).apply { setPadding(dp(4), dp(12), dp(4), dp(12)) })
        return content
    }

    private fun pluginCard(
        context: Context,
        entry: PluginCatalogEntry,
        panelColor: Int,
        textColor: Int,
        mutedColor: Int,
        dp: (Int) -> Int
    ): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(11), dp(13), dp(11))
            background = rounded(panelColor, dp(12))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(8)
            }
        }
        val heading = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        heading.addView(text(context, entry.name, 14f, textColor, true, dp),
            LinearLayout.LayoutParams(0, -2, 1f))
        val category = TextView(context).apply {
            text = entry.category.uppercase()
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFF93C5FD.toInt())
            setPadding(dp(7), dp(4), dp(7), dp(4))
            background = rounded(0xFF1E3A5F.toInt(), dp(7))
        }
        heading.addView(category)
        card.addView(heading)
        card.addView(text(context, entry.path, 11f, mutedColor, false, dp))
        val sizeLabel = if (entry.sizeBytes >= 1024) {
            "${entry.sizeBytes / 1024} KB"
        } else {
            "${entry.sizeBytes} B"
        }
        card.addView(text(
            context, "Revisi ${entry.revision}  •  $sizeLabel",
            10f, mutedColor, false, dp
        ))

        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val copy = actionButton(context, "Salin URL", dp)
        val open = actionButton(context, "GitHub", dp)
        actions.addView(copy, LinearLayout.LayoutParams(0, dp(36), 1f).apply {
            marginEnd = dp(6)
        })
        actions.addView(open, LinearLayout.LayoutParams(0, dp(36), 1f))
        copy.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Plugin URL", entry.rawUrl))
            Toast.makeText(context, "URL ${entry.name} disalin.", Toast.LENGTH_SHORT).show()
        }
        open.setOnClickListener {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.githubUrl)))
            } catch (_: Exception) {
                Toast.makeText(context, "Tidak dapat membuka GitHub.", Toast.LENGTH_SHORT).show()
            }
        }
        card.addView(actions, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8)
        })
        return card
    }

    private fun infoCard(
        context: Context,
        title: String,
        description: String,
        panelColor: Int,
        textColor: Int,
        mutedColor: Int,
        dp: (Int) -> Int,
        onClick: (() -> Unit)? = null
    ): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(13))
            background = rounded(panelColor, dp(12))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(10)
            }
        }
        card.addView(text(context, title, 13f, textColor, true, dp))
        card.addView(text(context, description, 12f, mutedColor, false, dp))
        if (onClick != null) {
            card.isClickable = true
            card.isFocusable = true
            card.setOnClickListener { onClick() }
        }
        return card
    }

    private fun actionButton(context: Context, label: String, dp: (Int) -> Int): Button =
        Button(context).apply {
            text = label
            textSize = 11f
            isAllCaps = false
            setTextColor(android.graphics.Color.WHITE)
            background = rounded(0xFF2563EB.toInt(), dp(9))
            stateListAnimator = null
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(5), 0, dp(5), 0)
        }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }

    private fun text(
        context: Context,
        value: String,
        size: Float,
        color: Int,
        bold: Boolean,
        dp: (Int) -> Int
    ): TextView = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(color)
        gravity = Gravity.START
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(3), 0, dp(3))
    }
}

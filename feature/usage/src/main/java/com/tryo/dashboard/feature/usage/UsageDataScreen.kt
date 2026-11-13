package com.tryo.dashboard.feature.usage

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.view.View
import org.json.JSONObject
import java.util.Locale

object UsageDataScreen {
    fun render(
        context: Context,
        result: JSONObject,
        panelColor: Int,
        textColor: Int,
        mutedColor: Int,
        dp: (Int) -> Int,
        formatTime: (String) -> String
    ): List<View> {
        val limits = result.optJSONObject("limits")
        val views = mutableListOf<View>()
        views.add(metricCard(context, "ESTIMATED DOCUMENT READS", result.optLong("reads", 0L), limits?.optLong("reads", 50000L) ?: 50000L, panelColor, textColor, mutedColor, dp))
        views.add(metricCard(context, "ESTIMATED DOCUMENT WRITES", result.optLong("writes", 0L), limits?.optLong("writes", 20000L) ?: 20000L, panelColor, textColor, mutedColor, dp))
        views.add(metricCard(context, "ESTIMATED DOCUMENT DELETES", result.optLong("deletes", 0L), limits?.optLong("deletes", 20000L) ?: 20000L, panelColor, textColor, mutedColor, dp))
        val storage = result.optJSONObject("storage")
        if (storage != null && storage.optBoolean("available", true)) {
            views.add(storageCard(context, storage.optLong("bytes", 0L), limits?.optLong("storageBytes", 1073741824L) ?: 1073741824L, panelColor, textColor, mutedColor, dp))
        } else {
            views.add(TextView(context).apply {
                text = "DATABASE STORAGE\nTidak dihitung dalam mode estimasi aplikasi."
                textSize = 12f
                setTextColor(mutedColor)
                setPadding(dp(4), dp(10), dp(4), dp(12))
            })
        }
        views.add(TextView(context).apply {
            text = "Terakhir diperbarui: " + formatTime(result.optString("updatedAt", ""))
            textSize = 11f
            setTextColor(mutedColor)
            setPadding(dp(4), dp(10), dp(4), dp(3))
        })
        views.add(TextView(context).apply {
            text = result.optString("note", "Metrik dapat mengalami keterlambatan.")
            textSize = 11f
            setTextColor(mutedColor)
            setPadding(dp(4), dp(2), dp(4), dp(12))
        })
        return views
    }

    fun metricCard(context: Context, label: String, used: Long, limit: Long, panelColor: Int, textColor: Int, mutedColor: Int, dp: (Int) -> Int): LinearLayout {
        val safeLimit = limit.coerceAtLeast(1L)
        val percent = ((used.toDouble() / safeLimit.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        val card = baseCard(context, panelColor, dp)
        addText(context, card, label, 11f, mutedColor, true, dp)
        addText(context, card, String.format(Locale.US, "%,d / %,d", used, limit), 21f, textColor, true, dp)
        val bar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = percent
            progressTintList = ColorStateList.valueOf(progressColor(percent))
        }
        card.addView(bar, LinearLayout.LayoutParams(-1, dp(8)).apply { topMargin = dp(9) })
        addText(context, card, "$percent% dari kuota harian gratis (estimasi aplikasi)", 11f, mutedColor, false, dp)
        return card
    }

    fun storageCard(context: Context, usedBytes: Long, limitBytes: Long, panelColor: Int, textColor: Int, mutedColor: Int, dp: (Int) -> Int): LinearLayout {
        val safeLimit = limitBytes.coerceAtLeast(1L)
        val percent = ((usedBytes.toDouble() / safeLimit.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        val usedMb = usedBytes.toDouble() / (1024.0 * 1024.0)
        val limitMb = safeLimit.toDouble() / (1024.0 * 1024.0)
        val card = baseCard(context, panelColor, dp)
        addText(context, card, "DATABASE STORAGE", 11f, mutedColor, true, dp)
        addText(context, card, String.format(Locale.US, "%.1f MB / %.0f MB", usedMb, limitMb), 21f, textColor, true, dp)
        val bar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = percent
            progressTintList = ColorStateList.valueOf(progressColor(percent))
        }
        card.addView(bar, LinearLayout.LayoutParams(-1, dp(8)).apply { topMargin = dp(9) })
        addText(context, card, "$percent% dari batas storage gratis", 11f, mutedColor, false, dp)
        return card
    }

    private fun baseCard(context: Context, color: Int, dp: (Int) -> Int) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(13), dp(14), dp(13))
        background = GradientDrawable().apply { setColor(color); cornerRadius = dp(12).toFloat() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
    }

    private fun addText(context: Context, parent: LinearLayout, value: String, size: Float, color: Int, bold: Boolean, dp: (Int) -> Int) {
        parent.addView(TextView(context).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(3), 0, dp(3))
        })
    }

    private fun progressColor(percent: Int) = when {
        percent >= 90 -> Color.rgb(239, 68, 68)
        percent >= 75 -> Color.rgb(245, 158, 11)
        else -> Color.rgb(34, 197, 94)
    }
}

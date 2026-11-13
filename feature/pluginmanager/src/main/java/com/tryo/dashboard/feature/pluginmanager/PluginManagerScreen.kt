package com.tryo.dashboard.feature.pluginmanager

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * UI owned by the Plugin Manager feature module.
 * The app shell supplies its current palette and density conversion.
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
        content.addView(text(context, "Selamat datang di Dashboard App", 19f, textColor, true, dp).apply {
            setPadding(dp(4), dp(14), dp(4), dp(6))
        })
        content.addView(text(
            context,
            "Plugin Manager adalah modul utama. Status, pembaruan, dan pengelolaan plugin akan ditempatkan di sini.",
            13f, mutedColor, false, dp
        ).apply {
            setPadding(dp(4), 0, dp(4), dp(14))
        })
        content.addView(infoCard(context, "PLUGIN MANAGER", "Modul utama • status dan pembaruan plugin", panelColor, textColor, mutedColor, dp))
        content.addView(infoCard(context, "TRACKER", "Riwayat aktivitas dan event aplikasi  •  Buka", panelColor, textColor, mutedColor, dp) {
            onNavigate("tracker")
        })
        content.addView(infoCard(context, "FEEDBACK", "Masukan dan penilaian pengguna  •  Buka", panelColor, textColor, mutedColor, dp) {
            onNavigate("feedback")
        })
        content.addView(infoCard(context, "USAGE DATA", "Estimasi operasi dan penggunaan Firestore  •  Buka", panelColor, textColor, mutedColor, dp) {
            onNavigate("usage")
        })
        content.addView(text(
            context,
            "Data operasional tetap melalui Apps Script dan Firestore.",
            11f, mutedColor, false, dp
        ).apply {
            setPadding(dp(4), dp(12), dp(4), dp(12))
        })
        return content
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
            background = GradientDrawable().apply {
                setColor(panelColor)
                cornerRadius = dp(12).toFloat()
            }
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

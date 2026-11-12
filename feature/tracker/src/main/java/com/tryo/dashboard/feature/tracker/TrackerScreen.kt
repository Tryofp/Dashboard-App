package com.tryo.dashboard.feature.tracker

import android.content.Context
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject

object TrackerScreen {
    fun recordCard(context: Context, item: JSONObject, panelColor: Int, textColor: Int, mutedColor: Int, dp: (Int) -> Int, formatTime: (String) -> String): LinearLayout {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(13))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(panelColor)
                cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
        }
        fun line(value: String, size: Float, color: Int, bold: Boolean = false) {
            card.addView(TextView(context).apply {
                text = value
                textSize = size
                setTextColor(color)
                if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0, dp(3), 0, dp(3))
            })
        }
        line(item.optString("Username", "Unknown"), 16f, textColor, true)
        line("User ID: ${item.optString("UserId", "-")}", 11f, mutedColor)
        line("Game: ${item.optString("MapName", "-")}", 13f, textColor)
        line("Device: ${item.optString("Device", "-")}  •  ${item.optString("Playtime", "-")}", 12f, mutedColor)
        line("Place ID: ${item.optString("PlaceId", "-")}", 11f, mutedColor)
        line(formatTime(item.optString("Timestamp", "")), 11f, Color.rgb(96, 165, 250))
        return card
    }
}

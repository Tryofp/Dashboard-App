package com.tryo.dashboard.feature.donghua

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

object DonghuaScreen {
    fun render(
        context: Context, items: List<ProgressItem>, selectedType: String,
        panel: Int, white: Int, muted: Int, dp: (Int) -> Int,
        onTypeChange: (String) -> Unit, onAdd: () -> Unit,
        onEdit: (ProgressItem) -> Unit, onDelete: (ProgressItem) -> Unit
    ): View {
        val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val category = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(12))
        }
        fun categoryButton(label: String, type: String): Button =
            Button(context).apply {
                text = label
                textSize = 11f
                isAllCaps = false
                setTextColor(white)
                background = rounded(if (selectedType == type) Color.rgb(37, 99, 235) else panel, dp(9))
                setOnClickListener { onTypeChange(type) }
            }
        category.addView(categoryButton("DONGHUA", "donghua"),
            LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginEnd = dp(5) })
        category.addView(categoryButton("MANHWA", "manhwa"),
            LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginStart = dp(5) })
        content.addView(category)

        val add = Button(context).apply {
            text = "+ Tambah Judul"
            textSize = 12f
            isAllCaps = false
            setTextColor(white)
            background = rounded(Color.rgb(37, 99, 235), dp(10))
            setOnClickListener { onAdd() }
        }
        content.addView(add, LinearLayout.LayoutParams(-1, dp(46)).apply { bottomMargin = dp(12) })

        if (items.isEmpty()) {
            content.addView(TextView(context).apply {
                text = "Belum ada " + (if (selectedType == "manhwa") "Manhwa" else "Donghua") +
                    ".\nTekan Tambah Judul untuk mulai."
                textSize = 13f
                setTextColor(muted)
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(34), dp(12), dp(34))
            })
        } else {
            items.forEach { item ->
                val card = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(13), dp(12), dp(13), dp(12))
                    background = rounded(panel, dp(12))
                    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) }
                }
                card.addView(TextView(context).apply {
                    text = item.title
                    textSize = 15f
                    setTextColor(white)
                    typeface = Typeface.DEFAULT_BOLD
                })
                card.addView(TextView(context).apply {
                    text = "Episode " + item.episode + if (item.status.isNotBlank()) "  •  " + item.status else ""
                    textSize = 12f
                    setTextColor(muted)
                    setPadding(0, dp(5), 0, dp(8))
                })
                val actions = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
                val edit = Button(context).apply {
                    text = "Ubah Episode"
                    textSize = 11f
                    isAllCaps = false
                    setTextColor(white)
                    background = rounded(Color.rgb(37, 99, 235), dp(8))
                    setOnClickListener { onEdit(item) }
                }
                val delete = Button(context).apply {
                    text = "Hapus"
                    textSize = 11f
                    isAllCaps = false
                    setTextColor(white)
                    background = rounded(Color.rgb(153, 27, 27), dp(8))
                    setOnClickListener { onDelete(item) }
                }
                actions.addView(edit, LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginEnd = dp(5) })
                actions.addView(delete, LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginStart = dp(5) })
                card.addView(actions)
                content.addView(card)
            }
        }
        return content
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }
}
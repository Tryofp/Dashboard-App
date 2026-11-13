package com.tryo.dashboard.feature.donghua

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView

object DonghuaScreen {
    fun render(
        context: Context, items: List<ProgressItem>, selectedType: String,
        panel: Int, white: Int, muted: Int, dp: (Int) -> Int,
        onTypeChange: (String) -> Unit, onAdd: () -> Unit,
        onEdit: (ProgressItem) -> Unit, onDelete: (ProgressItem) -> Unit
    ): View {
        val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val blue = Color.rgb(37, 99, 235)
        val search = EditText(context).apply {
            hint = "Cari judul Donghua / Manhwa..."
            textSize = 13f
            setSingleLine(true)
            setTextColor(white)
            setHintTextColor(muted)
            setPadding(dp(14), 0, dp(14), 0)
            background = rounded(panel, dp(11))
        }
        content.addView(search, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(10) })

        val category = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        fun categoryButton(label: String, type: String): Button =
            Button(context).apply {
                text = label
                textSize = 10f
                isAllCaps = false
                setTextColor(white)
                stateListAnimator = null
                background = rounded(if (selectedType == type) blue else panel, dp(18))
                setOnClickListener { onTypeChange(type) }
            }
        category.addView(categoryButton("Semua", "all"),
            LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginEnd = dp(4) })
        category.addView(categoryButton("Donghua", "donghua"),
            LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginStart = dp(4); marginEnd = dp(4) })
        category.addView(categoryButton("Manhwa", "manhwa"),
            LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginStart = dp(4) })
        content.addView(category)

        val toolbar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
        }
        val count = TextView(context).apply {
            text = "${items.size} judul tersimpan"
            textSize = 11f
            setTextColor(muted)
        }
        toolbar.addView(count, LinearLayout.LayoutParams(0, -2, 1f))
        val filterButton = Button(context).apply {
            text = "☷ Filter"
            textSize = 11f
            isAllCaps = false
            setTextColor(white)
            stateListAnimator = null
            background = rounded(panel, dp(9))
        }
        toolbar.addView(filterButton, LinearLayout.LayoutParams(dp(92), dp(38)).apply { marginEnd = dp(7) })
        val addButton = Button(context).apply {
            text = "+ Tambah"
            textSize = 11f
            isAllCaps = false
            setTextColor(white)
            stateListAnimator = null
            background = rounded(blue, dp(9))
            setOnClickListener { onAdd() }
        }
        toolbar.addView(addButton, LinearLayout.LayoutParams(dp(100), dp(38)))
        content.addView(toolbar)

        val rows = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        content.addView(rows)
        var selectedStatus = "Semua"

        fun refreshRows() {
            rows.removeAllViews()
            val query = search.text?.toString()?.trim()?.lowercase().orEmpty()
            val visible = items.filter { item ->
                (selectedType == "all" || item.type.equals(selectedType, true)) &&
                    (query.isEmpty() || item.title.lowercase().contains(query)) &&
                    (selectedStatus == "Semua" || item.status.equals(selectedStatus, true))
            }
            count.text = "${visible.size} dari ${items.size} judul"
            if (visible.isEmpty()) {
                rows.addView(TextView(context).apply {
                    text = if (items.isEmpty()) "Belum ada judul di daftar.\nTekan + Tambah untuk mulai." else "Tidak ada judul yang cocok."
                    textSize = 13f
                    setTextColor(muted)
                    gravity = Gravity.CENTER
                    setPadding(dp(12), dp(34), dp(12), dp(34))
                })
                return
            }
            visible.forEach { item ->
                val card = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(13), dp(10), dp(7), dp(10))
                    background = rounded(panel, dp(12))
                    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
                }
                val details = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                details.addView(TextView(context).apply {
                    text = item.title
                    textSize = 14f
                    setTextColor(white)
                    typeface = Typeface.DEFAULT_BOLD
                })
                val categoryName = if (item.type.equals("manhwa", true)) "Manhwa" else "Donghua"
                val progressLabel = if (categoryName == "Manhwa") "Chapter" else "Episode"
                val meta = buildString {
                    append(progressLabel).append(" ").append(item.episode)
                    append("  •  ").append(categoryName)
                    if (item.status.isNotBlank()) append("  •  ").append(item.status)
                }
                details.addView(TextView(context).apply {
                    text = meta
                    textSize = 10.5f
                    setTextColor(muted)
                    setPadding(0, dp(5), 0, 0)
                })
                card.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
                val menu = TextView(context).apply {
                    text = "⋮"
                    textSize = 25f
                    gravity = Gravity.CENTER
                    setTextColor(muted)
                    setPadding(dp(12), 0, dp(12), 0)
                    setOnClickListener { anchor ->
                        PopupMenu(context, anchor).apply {
                            menu.add("Ubah progress").setOnMenuItemClickListener {
                                onEdit(item); true
                            }
                            menu.add("Hapus judul").setOnMenuItemClickListener {
                                onDelete(item); true
                            }
                            show()
                        }
                    }
                }
                card.addView(menu, LinearLayout.LayoutParams(dp(44), dp(46)))
                rows.addView(card)
            }
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refreshRows() }
            override fun afterTextChanged(s: Editable?) {}
        })
        filterButton.setOnClickListener {
            val options = arrayOf("Semua", "Tamat", "Gantung", "Bersambung")
            AlertDialog.Builder(context)
                .setTitle("Filter status")
                .setSingleChoiceItems(options, options.indexOf(selectedStatus).coerceAtLeast(0)) { dialog, which ->
                    selectedStatus = options[which]
                    filterButton.text = if (selectedStatus == "Semua") "☷ Filter" else "☷ $selectedStatus"
                    refreshRows()
                    dialog.dismiss()
                }
                .setNegativeButton("Tutup", null)
                .show()
        }
        refreshRows()
        return content
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }
}

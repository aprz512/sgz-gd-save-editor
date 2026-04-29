package com.aprz.gdsaveeditor

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class FactionListFragment : Fragment() {

    var vstates: JsonArray? = null
    var actors: JsonArray? = null
    var citys: JsonArray? = null

    private fun getActorName(id: Int): String {
        if (actors == null) return "$id"
        return (0 until actors!!.size()).map { actors!!.get(it).asJsonObject }
            .find { it.get("ID")?.asInt == id }?.get("姓名")?.asString ?: "$id"
    }

    private fun getCityName(id: Int): String {
        if (citys == null) return "$id"
        return (0 until citys!!.size()).map { citys!!.get(it).asJsonObject }
            .find { it.get("ID")?.asInt == id }?.get("城池名")?.asString ?: "$id"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val scroll = ScrollView(requireContext())
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val all = vstates
        if (all == null) {
            scroll.addView(layout)
            return scroll
        }

        for (i in 0 until all.size()) {
            val v = all.get(i).asJsonObject
            val monarchId = v.get("君主")?.asInt ?: -1
            val monarchName = if (monarchId >= 0) getActorName(monarchId) else "无"
            val targetCity = getCityName(v.get("目标城池")?.asInt ?: 0)
            val status = v.get("状态")?.asString ?: "正常"
            val friends = (v.get("友好势力")?.asJsonArray ?: JsonArray()).joinToString(", ") { getActorName(it.asInt) }
            val enemies = (v.get("仇恨势力")?.asJsonArray ?: JsonArray()).joinToString(", ") { getActorName(it.asInt) }
            val heirs = (v.get("继承人")?.asJsonArray ?: JsonArray()).joinToString(", ") { getActorName(it.asInt) }

            val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                radius = 24f
                setCardBackgroundColor(requireContext().getColor(android.R.color.transparent))
                strokeWidth = 1
                strokeColor = requireContext().getColor(android.R.color.darker_gray)
                isClickable = true
                isFocusable = true
                setOnClickListener { showEditDialog(v) }
            }

            val inner = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 14, 16, 14)
            }

            inner.addView(TextView(requireContext()).apply {
                text = "势力 ${v.get("ID")?.asString} — $monarchName"
                textSize = 16f
                setTextColor(resources.getColor(android.R.color.primary_text_dark, null))
            })
            inner.addView(TextView(requireContext()).apply {
                text = "目标: $targetCity  |  状态: $status"
                textSize = 12f
                setPadding(0, 4, 0, 0)
                setTextColor(resources.getColor(android.R.color.secondary_text_dark, null))
            })
            inner.addView(TextView(requireContext()).apply {
                text = "继承人: ${heirs.ifEmpty { "无" }}"
                textSize = 12f
                setPadding(0, 2, 0, 0)
                setTextColor(resources.getColor(android.R.color.secondary_text_dark, null))
            })
            inner.addView(TextView(requireContext()).apply {
                text = "友好: ${friends.ifEmpty { "无" }}"
                textSize = 12f
                setPadding(0, 2, 0, 0)
                setTextColor(resources.getColor(android.R.color.secondary_text_dark, null))
            })
            inner.addView(TextView(requireContext()).apply {
                text = "仇恨: ${enemies.ifEmpty { "无" }}"
                textSize = 12f
                setPadding(0, 2, 0, 0)
                setTextColor(resources.getColor(android.R.color.secondary_text_dark, null))
            })

            card.addView(inner)
            layout.addView(card)
        }

        scroll.addView(layout)
        return scroll
    }

    private fun showEditDialog(v: JsonObject) {
        val fields = listOf("君主" to "number", "目标城池" to "number", "状态" to "text")
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        val inputs = mutableMapOf<String, EditText>()

        for ((key, type) in fields) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }
            row.addView(TextView(requireContext()).apply {
                text = key; width = 180
                setTextColor(resources.getColor(android.R.color.primary_text_dark, null))
            })
            val edit = EditText(requireContext()).apply {
                setText(v.get(key)?.asString ?: "")
                if (type == "number") inputType = InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(edit)
            layout.addView(row)
            inputs[key] = edit
        }

        // Extra fields
        for (label in listOf("友好势力" to "friends", "仇恨势力" to "enemies", "继承人" to "heirs")) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }
            row.addView(TextView(requireContext()).apply {
                text = label.first; width = 180
                setTextColor(resources.getColor(android.R.color.primary_text_dark, null))
            })
            val edit = EditText(requireContext()).apply {
                setText(v.get(label.first)?.asJsonArray?.joinToString(",") { it.asString } ?: "")
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(edit)
            layout.addView(row)
            inputs[label.second] = edit
        }

        val scrollView = ScrollView(requireContext()).apply { addView(layout) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("编辑势力 ${v.get("ID")?.asString}")
            .setView(scrollView)
            .setPositiveButton("保存") { _, _ ->
                for ((key, edit) in inputs) {
                    when (key) {
                        "friends" -> v.add("友好势力", parseIdArray(edit.text.toString()))
                        "enemies" -> v.add("仇恨势力", parseIdArray(edit.text.toString()))
                        "heirs" -> v.add("继承人", parseIdArray(edit.text.toString()))
                        else -> {
                            val value = edit.text.toString().trim()
                            if (value.isEmpty()) v.remove(key)
                            else v.get(key)?.asInt?.let { v.addProperty(key, value.toIntOrNull() ?: return@let) }
                                ?: v.addProperty(key, value)
                        }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun parseIdArray(s: String): JsonArray {
        val arr = JsonArray()
        s.split(",", "，").mapNotNull { it.trim().toIntOrNull() }.forEach { arr.add(it) }
        return arr
    }
}

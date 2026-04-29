package com.aprz.gdsaveeditor

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class FactionListFragment : Fragment() {

    var vstates: JsonArray? = null
    var actors: JsonArray? = null
    var citys: JsonArray? = null

    private var rootView: View? = null
    private var isViewReady = false

    private fun getActorName(id: Int): String {
        val a = actors ?: return "$id"
        return (0 until a.size()).map { a.get(it).asJsonObject }
            .find { it.get("ID")?.asInt == id }?.get("姓名")?.asString ?: "$id"
    }

    private fun getCityName(id: Int): String {
        val c = citys ?: return "$id"
        return (0 until c.size()).map { c.get(it).asJsonObject }
            .find { it.get("ID")?.asInt == id }?.get("城池名")?.asString ?: "$id"
    }

    fun render() {
        if (!isViewReady) return
        val view = rootView as? ScrollView ?: return
        val layout = view.getChildAt(0) as? LinearLayout ?: return
        layout.removeAllViews()

        val all = vstates ?: return
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
                isClickable = true; isFocusable = true
                setOnClickListener { showEditDialog(v) }
            }

            val inner = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 14, 16, 14)
            }
            fun tv(t: String, size: Float, top: Int = 0, color: Int = android.R.color.primary_text_dark) {
                inner.addView(TextView(requireContext()).apply {
                    text = t; textSize = size
                    setPadding(0, top, 0, 0)
                    setTextColor(resources.getColor(color, null))
                })
            }
            tv("势力 ${v.get("ID")?.asString} — $monarchName", 16f)
            tv("目标: $targetCity  |  状态: $status", 12f, 4, android.R.color.secondary_text_dark)
            tv("继承人: ${heirs.ifEmpty { "无" }}", 12f, 2, android.R.color.secondary_text_dark)
            tv("友好: ${friends.ifEmpty { "无" }}", 12f, 2, android.R.color.secondary_text_dark)
            tv("仇恨: ${enemies.ifEmpty { "无" }}", 12f, 2, android.R.color.secondary_text_dark)

            card.addView(inner)
            layout.addView(card)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val scroll = ScrollView(requireContext())
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        scroll.addView(layout)
        rootView = scroll
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isViewReady = true
        render()
    }

    private fun showEditDialog(v: JsonObject) {
        val fields = listOf("君主" to "number", "目标城池" to "number", "状态" to "text")
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; setPadding(48, 24, 48, 24)
        }
        val inputs = mutableMapOf<String, EditText>()

        for ((key, type) in fields) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL; setPadding(0, 8, 0, 8)
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
            row.addView(edit); layout.addView(row); inputs[key] = edit
        }

        for ((label, key) in listOf("友好势力" to "friends", "仇恨势力" to "enemies", "继承人" to "heirs")) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL; setPadding(0, 8, 0, 8)
            }
            row.addView(TextView(requireContext()).apply {
                text = label; width = 180
                setTextColor(resources.getColor(android.R.color.primary_text_dark, null))
            })
            val edit = EditText(requireContext()).apply {
                setText(v.get(label)?.asJsonArray?.joinToString(",") { it.asString } ?: "")
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(edit); layout.addView(row); inputs[key] = edit
        }

        val sv = ScrollView(requireContext()).apply { addView(layout) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("编辑势力 ${v.get("ID")?.asString}")
            .setView(sv)
            .setPositiveButton("保存") { _, _ ->
                for ((key, edit) in inputs) {
                    when (key) {
                        "friends" -> v.add("友好势力", parseIdArray(edit.text.toString()))
                        "enemies" -> v.add("仇恨势力", parseIdArray(edit.text.toString()))
                        "heirs" -> v.add("继承人", parseIdArray(edit.text.toString()))
                        else -> {
                            val value = edit.text.toString().trim()
                            if (value.isEmpty()) v.remove(key)
                            else {
                                val num = value.toIntOrNull()
                                if (num != null) v.addProperty(key, num)
                                else v.addProperty(key, value)
                            }
                        }
                    }
                }
                render()
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

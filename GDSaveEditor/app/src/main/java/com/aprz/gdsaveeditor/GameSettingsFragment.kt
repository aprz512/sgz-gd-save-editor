package com.aprz.gdsaveeditor

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class GameSettingsFragment : Fragment() {

    var saveData: JsonObject? = null
    private var pendingData: JsonObject? = null
    private val inputMap = mutableMapOf<String, EditText>()
    private var isViewCreated = false

    fun loadSettings(data: JsonObject) {
        pendingData = data
        if (isViewCreated) applySettings(data)
    }

    private fun applySettings(data: JsonObject) {
        saveData = data
        inputMap["year"]?.setText(data.get("year")?.asString ?: "189")
        inputMap["month"]?.setText(data.get("month")?.asString ?: "1")
        val gs = data.get("game_set")?.asJsonObject
        if (gs != null) {
            for (key in gs.keySet()) {
                inputMap["gs_$key"]?.setText(gs.get(key)?.asString ?: "")
            }
        }
        val players = data.get("players")?.asJsonArray
        if (players != null && players.size() > 0) {
            val p = players.get(0).asJsonObject
            inputMap["power"]?.setText(p.get("power_value")?.asString ?: "50")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isViewCreated = true
        pendingData?.let { applySettings(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val scroll = ScrollView(requireContext())
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        fun addRow(label: String, key: String, type: String = "number") {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 12, 0, 12)
            }
            row.addView(TextView(requireContext()).apply {
                text = label; width = 200
                setTextColor(resources.getColor(android.R.color.primary_text_dark, null))
            })
            val edit = EditText(requireContext()).apply {
                if (type == "number") inputType = InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(edit)
            layout.addView(row)
            inputMap[key] = edit
        }

        fun addHeader(text: String) {
            layout.addView(TextView(requireContext()).apply {
                setText(text); textSize = 18f; setPadding(0, 24, 0, 8)
                setTextColor(resources.getColor(android.R.color.primary_text_dark, null))
            })
        }

        addRow("年份", "year")
        addRow("月份", "month")

        addHeader("游戏规则")
        addRow("人物大限", "gs_人物大限", "text")
        addRow("出仕地点", "gs_出仕地点", "text")
        addRow("出仕时间", "gs_出仕时间", "text")
        addRow("技能系统", "gs_技能系统", "text")
        addRow("监狱系统", "gs_监狱系统", "text")
        addRow("自动复活", "gs_自动复活", "text")
        addRow("武将成长", "gs_武将成长", "text")

        addHeader("玩家")
        addRow("战力值", "power")

        scroll.addView(layout)
        return scroll
    }

    fun collectSettings() {
        val data = saveData ?: return
        inputMap["year"]?.text?.toString()?.toIntOrNull()?.let { data.addProperty("year", it) }
        inputMap["month"]?.text?.toString()?.toIntOrNull()?.let { data.addProperty("month", it) }

        val gs = (data.get("game_set")?.asJsonObject) ?: JsonObject()
        for ((key, edit) in inputMap) {
            if (key.startsWith("gs_")) {
                val gsKey = key.removePrefix("gs_")
                val value = edit.text.toString().trim()
                if (value.isNotEmpty()) gs.addProperty(gsKey, value)
            }
        }
        data.add("game_set", gs)

        inputMap["power"]?.text?.toString()?.toIntOrNull()?.let { power ->
            val players = (data.get("players")?.asJsonArray) ?: JsonArray()
            if (players.size() > 0) {
                players.get(0).asJsonObject.addProperty("power_value", power)
            }
        }
    }
}

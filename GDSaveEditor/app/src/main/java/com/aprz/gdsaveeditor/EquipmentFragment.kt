package com.aprz.gdsaveeditor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class EquipmentFragment : Fragment() {

    var weapons: JsonArray? = null
    var steeds: JsonArray? = null
    var suits: JsonArray? = null
    var jewelrys: JsonArray? = null

    private var tableContainer: LinearLayout? = null
    private var currentType: String = "weapons"
    private var viewReady = false

    fun refresh() {
        if (viewReady) render(currentType)
    }

    private fun render(type: String) {
        currentType = type
        val container = tableContainer ?: return
        container.removeAllViews()

        val data = when (type) {
            "steeds" -> steeds; "suits" -> suits; "jewelrys" -> jewelrys; else -> weapons
        }
        if (data == null || data.size() == 0) {
            container.addView(TextView(requireContext()).apply {
                text = "暂无数据"
                textSize = 14f
                setPadding(16, 32, 16, 32)
                setTextColor(resources.getColor(android.R.color.secondary_text_dark, null))
            })
            return
        }

        // Detect format: 4.sav has full detail, 2.sav has only ID+数量
        val first = data.get(0).asJsonObject
        val hasDetail = first.has("名称")

        val table = TableLayout(requireContext()).apply { isStretchAllColumns = true }

        if (hasDetail) {
            val headers = when (type) {
                "weapons" -> listOf("ID", "名称", "品质", "类型", "攻击", "防御", "重量", "价格")
                "steeds" -> listOf("ID", "名称", "品质", "马力", "机动恢复", "价格")
                "suits" -> listOf("ID", "名称", "品质", "防御", "重量", "价格")
                else -> listOf("ID", "名称", "品质", "类型", "射程", "价格")
            }
            table.addView(headerRow(headers))
            for (i in 0 until data.size()) {
                val item = data.get(i).asJsonObject
                val cells = when (type) {
                    "weapons" -> listOf(s(item, "ID"), s(item, "名称"), s(item, "品质"), s(item, "类型"),
                        s(item, "攻击力"), s(item, "防御力"), s(item, "重量"), s(item, "价格"))
                    "steeds" -> listOf(s(item, "ID"), s(item, "名称"), s(item, "品质"),
                        s(item, "马力"), s(item, "机动力恢复"), s(item, "价格"))
                    "suits" -> listOf(s(item, "ID"), s(item, "名称"), s(item, "品质"),
                        s(item, "防御力"), s(item, "重量"), s(item, "价格"))
                    else -> listOf(s(item, "ID"), s(item, "名称"), s(item, "品质"), s(item, "类型"),
                        s(item, "射程"), s(item, "价格"))
                }
                table.addView(dataRow(cells))
            }
        } else {
            // Minimal format (2.sav): just ID + 数量
            table.addView(headerRow(listOf("ID", "数量")))
            for (i in 0 until data.size()) {
                val item = data.get(i).asJsonObject
                table.addView(dataRow(listOf(s(item, "ID"), s(item, "数量"))))
            }
        }

        container.addView(table)
    }

    private fun s(obj: JsonObject, key: String): String {
        val el = obj.get(key) ?: return ""
        return el.asJsonPrimitive.asString
    }

    private fun headerRow(headers: List<String>): TableRow {
        val row = TableRow(requireContext())
        for (h in headers) {
            row.addView(TextView(requireContext()).apply {
                text = h; textSize = 11f
                setTextColor(resources.getColor(android.R.color.holo_orange_light, null))
                setPadding(4, 8, 4, 8)
            })
        }
        return row
    }

    private fun dataRow(cells: List<String>): TableRow {
        val row = TableRow(requireContext())
        for (c in cells) {
            row.addView(TextView(requireContext()).apply {
                text = c; textSize = 11f
                setTextColor(resources.getColor(android.R.color.primary_text_dark, null))
                setPadding(4, 6, 4, 6)
            })
        }
        return row
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }

        val tabs = TabLayout(requireContext()).apply {
            addTab(newTab().setText("武器")); addTab(newTab().setText("坐骑"))
            addTab(newTab().setText("防具")); addTab(newTab().setText("道具"))
            setTabTextColors(
                resources.getColor(android.R.color.secondary_text_dark, null),
                resources.getColor(android.R.color.primary_text_dark, null)
            )
            setSelectedTabIndicatorColor(resources.getColor(android.R.color.holo_orange_light, null))
        }
        root.addView(tabs)

        val scroll = ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        val inner = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; setPadding(8, 8, 8, 8)
        }
        scroll.addView(inner)
        root.addView(scroll)
        tableContainer = inner

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> render("weapons"); 1 -> render("steeds")
                    2 -> render("suits"); 3 -> render("jewelrys")
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewReady = true
        render(currentType)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewReady = false
        tableContainer = null
    }
}

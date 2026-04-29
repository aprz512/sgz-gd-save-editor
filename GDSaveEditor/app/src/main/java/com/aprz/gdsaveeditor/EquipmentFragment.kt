package com.aprz.gdsaveeditor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.gson.JsonArray

class EquipmentFragment : Fragment() {

    var weapons: JsonArray? = null
    var steeds: JsonArray? = null
    var suits: JsonArray? = null
    var jewelrys: JsonArray? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        val tabs = TabLayout(requireContext()).apply {
            addTab(newTab().setText("武器"))
            addTab(newTab().setText("坐骑"))
            addTab(newTab().setText("防具"))
            addTab(newTab().setText("道具"))
            setTabTextColors(
                resources.getColor(android.R.color.secondary_text_dark, null),
                resources.getColor(android.R.color.primary_text_dark, null)
            )
            setSelectedTabIndicatorColor(resources.getColor(android.R.color.holo_orange_light, null))
        }
        root.addView(tabs)

        val scroll = ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        val tableContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
        }
        scroll.addView(tableContainer)
        root.addView(scroll)

        fun renderEquip(data: JsonArray?, type: String) {
            tableContainer.removeAllViews()
            if (data == null || data.size() == 0) return

            val table = TableLayout(requireContext()).apply {
                isStretchAllColumns = true
            }

            val headers = when (type) {
                "weapons" -> listOf("ID", "名称", "品质", "类型", "攻击", "防御", "重量", "价格")
                "steeds" -> listOf("ID", "名称", "品质", "马力", "机动恢复", "价格")
                "suits" -> listOf("ID", "名称", "品质", "防御", "重量", "价格")
                else -> listOf("ID", "名称", "品质", "类型", "射程", "价格")
            }

            val headerRow = TableRow(requireContext())
            for (h in headers) {
                headerRow.addView(TextView(requireContext()).apply {
                    text = h; textSize = 11f
                    setTextColor(resources.getColor(android.R.color.holo_orange_light, null))
                    setPadding(4, 8, 4, 8)
                })
            }
            table.addView(headerRow)

            for (i in 0 until data.size()) {
                val item = data.get(i).asJsonObject
                val cells = when (type) {
                    "weapons" -> listOf(
                        item.get("ID")?.asString ?: "",
                        item.get("名称")?.asString ?: "",
                        item.get("品质")?.asString ?: "",
                        item.get("类型")?.asString ?: "",
                        item.get("攻击力")?.asString ?: "",
                        item.get("防御力")?.asString ?: "",
                        item.get("重量")?.asString ?: "",
                        item.get("价格")?.asString ?: ""
                    )
                    "steeds" -> listOf(
                        item.get("ID")?.asString ?: "",
                        item.get("名称")?.asString ?: "",
                        item.get("品质")?.asString ?: "",
                        item.get("马力")?.asString ?: "",
                        item.get("机动力恢复")?.asString ?: "",
                        item.get("价格")?.asString ?: ""
                    )
                    "suits" -> listOf(
                        item.get("ID")?.asString ?: "",
                        item.get("名称")?.asString ?: "",
                        item.get("品质")?.asString ?: "",
                        item.get("防御力")?.asString ?: "",
                        item.get("重量")?.asString ?: "",
                        item.get("价格")?.asString ?: ""
                    )
                    else -> listOf(
                        item.get("ID")?.asString ?: "",
                        item.get("名称")?.asString ?: "",
                        item.get("品质")?.asString ?: "",
                        item.get("类型")?.asString ?: "",
                        item.get("射程")?.asString ?: "",
                        item.get("价格")?.asString ?: ""
                    )
                }

                val row = TableRow(requireContext())
                for (c in cells) {
                    row.addView(TextView(requireContext()).apply {
                        text = c; textSize = 11f
                        setTextColor(resources.getColor(android.R.color.primary_text_dark, null))
                        setPadding(4, 6, 4, 6)
                    })
                }
                table.addView(row)
            }

            tableContainer.addView(table)
        }

        renderEquip(weapons, "weapons")

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> renderEquip(weapons, "weapons")
                    1 -> renderEquip(steeds, "steeds")
                    2 -> renderEquip(suits, "suits")
                    3 -> renderEquip(jewelrys, "jewelrys")
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        return root
    }
}

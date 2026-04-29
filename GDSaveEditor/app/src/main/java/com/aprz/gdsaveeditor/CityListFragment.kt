package com.aprz.gdsaveeditor

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.aprz.gdsaveeditor.databinding.FragmentCityListBinding
import com.aprz.gdsaveeditor.databinding.ItemCityBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class CityListFragment : Fragment() {

    private var _binding: FragmentCityListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CityAdapter

    var citys: JsonArray? = null
        set(value) {
            field = value
            if (::adapter.isInitialized && value != null) {
                val list = (0 until value.size()).map { value.get(it).asJsonObject }
                binding.tvCityCount.text = "${list.size}"
                adapter.submitList(list)
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCityListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = CityAdapter { showEditCityDialog(it) }
        binding.rvCities.adapter = adapter
        citys?.let { citys = it } // trigger setter
    }

    private fun showEditCityDialog(city: JsonObject) {
        val fields = listOf(
            "ID" to "readonly", "城池名" to "readonly",
            "金" to "number", "米" to "number", "人口" to "number",
            "土地" to "number", "产业" to "number", "统治度" to "number",
            "后备兵" to "number", "防灾" to "number",
            "买米价" to "number", "卖米价" to "number",
            "归属" to "number"
        )

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
                setText(city.get(key)?.asString ?: "")
                isEnabled = type != "readonly"
                if (type == "number") inputType = InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(edit)
            layout.addView(row)
            if (type != "readonly") inputs[key] = edit
        }

        val sv = ScrollView(requireContext()).apply { addView(layout) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("编辑城池 - ${city.get("城池名")?.asString ?: ""}")
            .setView(sv)
            .setPositiveButton("保存") { _, _ ->
                for ((key, edit) in inputs) {
                    val value = edit.text.toString().trim()
                    if (value.isEmpty()) city.remove(key)
                    else {
                        val num = value.toIntOrNull()
                        if (num != null) city.addProperty(key, num)
                        else city.addProperty(key, value)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class CityAdapter(private val onItemClick: (JsonObject) -> Unit) :
    RecyclerView.Adapter<CityAdapter.ViewHolder>() {

    private var items: List<JsonObject> = emptyList()

    fun submitList(list: List<JsonObject>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemCityBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(city: JsonObject) {
            binding.tvCityId.text = city.get("ID")?.asString ?: ""
            binding.tvCityName.text = city.get("城池名")?.asString ?: ""
            binding.tvCityInfo.text = "${city.get("行政区")?.asString ?: ""} · ${city.get("地域")?.asString ?: ""}"
            binding.tvCityResources.text = "金${city.get("金")?.asString ?: ""} 米${city.get("米")?.asString ?: ""}\n人口${city.get("人口")?.asString ?: ""}"
            binding.root.setOnClickListener { onItemClick(city) }
        }
    }
}

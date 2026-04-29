package com.aprz.gdsaveeditor

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.aprz.gdsaveeditor.databinding.FragmentActorListBinding
import com.aprz.gdsaveeditor.databinding.ItemActorBinding
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class ActorListFragment : Fragment() {

    private var _binding: FragmentActorListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ActorAdapter

    var actors: JsonArray? = null
    var citys: JsonArray? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActorListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ActorAdapter { actor -> showEditActorDialog(actor) }
        binding.rvActors.adapter = adapter

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { filterActors() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
            listOf("全部") + listOf("在野", "出仕", "死亡"))
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spStatus.adapter = statusAdapter
        binding.spStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterActors()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        filterActors()
    }

    fun filterActors() {
        val all = actors ?: run { binding.tvCount.text = "0/0"; return }
        val search = binding.etSearch.text.toString().lowercase()
        val statusPos = binding.spStatus.selectedItemPosition
        val statusFilter = if (statusPos > 0) listOf("在野", "出仕", "死亡")[statusPos - 1] else null

        val filtered = (0 until all.size()).map { all.get(it).asJsonObject }.filter { actor ->
            val name = actor.get("姓名")?.asString ?: ""
            val zi = actor.get("字")?.asString ?: ""
            val status = actor.get("状态")?.asString ?: ""
            val matchSearch = search.isEmpty() || name.lowercase().contains(search) || zi.lowercase().contains(search)
            val matchStatus = statusFilter == null || status == statusFilter
            matchSearch && matchStatus
        }

        binding.tvCount.text = "${filtered.size}/${all.size()}"
        adapter.submitList(filtered)
    }

    private fun showEditActorDialog(actor: JsonObject) {
        val fields = listOf(
            "ID" to "readonly",
            "姓名" to "text", "姓" to "text", "字" to "text", "性别" to "text",
            "体" to "number", "武" to "number", "知" to "number", "政" to "number",
            "统" to "number", "德" to "number", "忠" to "number", "胆" to "number",
            "等级" to "number", "经验" to "number", "兵力" to "number",
            "军种" to "text", "状态" to "text", "所在城" to "number", "流放地" to "number",
            "坐骑" to "number", "武器" to "number", "防具" to "number", "道具" to "number",
            "大限" to "number", "相性" to "number", "面" to "text", "标签" to "text"
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
            val label = TextView(requireContext()).apply {
                text = key
                width = 160
                setTextColor(resources.getColor(android.R.color.primary_text_dark, null))
            }
            row.addView(label)
            val rawVal = actor.get(key)
            val displayVal = when {
                rawVal == null -> ""
                rawVal.isJsonArray -> rawVal.asJsonArray.joinToString(",") { it.asString }
                else -> rawVal.asString
            }
            val edit = EditText(requireContext()).apply {
                setText(displayVal)
                isEnabled = type != "readonly"
                if (type == "number") inputType = InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(edit)
            layout.addView(row)
            if (type != "readonly") inputs[key] = edit
        }

        val scrollView = ScrollView(requireContext()).apply {
            addView(layout)
            isFillViewport = true
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("编辑武将 - ${actor.get("姓名")?.asString ?: ""}")
            .setView(scrollView)
            .setPositiveButton("保存") { _, _ ->
                for ((key, edit) in inputs) {
                    val value = edit.text.toString().trim()
                    if (key == "标签") {
                        val arr = JsonArray()
                        value.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }.forEach { arr.add(it) }
                        actor.add(key, arr)
                    } else if (value.isEmpty()) {
                        // Preserve key with empty string, don't remove
                        actor.addProperty(key, "")
                    } else {
                        val num = value.toIntOrNull()
                        if (num != null) actor.addProperty(key, num)
                        else actor.addProperty(key, value)
                    }
                }
                filterActors()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ActorAdapter(private val onItemClick: (JsonObject) -> Unit) :
    RecyclerView.Adapter<ActorAdapter.ViewHolder>() {

    private var items: List<JsonObject> = emptyList()

    fun submitList(list: List<JsonObject>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemActorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemActorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(actor: JsonObject) {
            binding.tvId.text = actor.get("ID")?.asString ?: ""
            binding.tvName.text = actor.get("姓名")?.asString ?: ""
            val status = actor.get("状态")?.asString ?: ""
            binding.tvStatus.text = status
            binding.tvCity.text = "城:${actor.get("所在城")?.asString ?: ""}"
            binding.tvStats.text = "武${actor.get("武")?.asString ?: ""} 知${actor.get("知")?.asString ?: ""} 统${actor.get("统")?.asString ?: ""}\n政${actor.get("政")?.asString ?: ""} 德${actor.get("德")?.asString ?: ""} 体${actor.get("体")?.asString ?: ""}"
            binding.root.setOnClickListener { onItemClick(actor) }
        }
    }
}

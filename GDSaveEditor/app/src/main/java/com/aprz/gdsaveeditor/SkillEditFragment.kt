package com.aprz.gdsaveeditor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.gson.JsonArray

class SkillEditFragment : Fragment() {

    var actors: JsonArray? = null
    private val skillEdits = mutableListOf<EditText>()

    fun getSkills(): Skills {
        return Skills(
            skill1 = skillEdits.getOrNull(0)?.text?.toString() ?: "",
            skill2 = skillEdits.getOrNull(1)?.text?.toString() ?: "",
            skill3 = skillEdits.getOrNull(2)?.text?.toString() ?: "",
            skill4 = skillEdits.getOrNull(3)?.text?.toString() ?: "",
            skill5 = skillEdits.getOrNull(4)?.text?.toString() ?: "",
            skill6 = skillEdits.getOrNull(5)?.text?.toString() ?: "",
            skill7 = skillEdits.getOrNull(6)?.text?.toString() ?: "",
            skill8 = skillEdits.getOrNull(7)?.text?.toString() ?: ""
        )
    }

    fun loadSkills() {
        val actors = this.actors ?: return
        val actor = (0 until actors.size())
            .map { actors.get(it).asJsonObject }
            .find { it.has("diy_skills") } ?: return

        val encrypted = actor.get("diy_skills")?.asString ?: return
        try {
            val skills = DiySkills.decryptSkill(encrypted)
            skillEdits.getOrNull(0)?.setText(skills.skill1)
            skillEdits.getOrNull(1)?.setText(skills.skill2)
            skillEdits.getOrNull(2)?.setText(skills.skill3)
            skillEdits.getOrNull(3)?.setText(skills.skill4)
            skillEdits.getOrNull(4)?.setText(skills.skill5)
            skillEdits.getOrNull(5)?.setText(skills.skill6)
            skillEdits.getOrNull(6)?.setText(skills.skill7)
            skillEdits.getOrNull(7)?.setText(skills.skill8)
        } catch (e: Exception) {
            // ignore decrypt errors
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = android.widget.ScrollView(requireContext()).apply {
            setPadding(16, 16, 16, 16)
        }
        val linear = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
        }

        val title = android.widget.TextView(requireContext()).apply {
            text = "自定义技能编辑"
            textSize = 24f
            setTextColor(resources.getColor(android.R.color.primary_text_dark, null))
        }
        linear.addView(title)

        val desc = android.widget.TextView(requireContext()).apply {
            text = "此功能用于编辑自定义武将（diy_skills）的8个技能位。\n非自定义武将不包含此数据，修改无效。\n\n每个技能位可填写技能名称，留空表示不使用该技能位。"
            textSize = 13f
            setTextColor(resources.getColor(android.R.color.secondary_text_dark, null))
            setPadding(0, 8, 0, 20)
            setLineSpacing(0f, 1.3f)
        }
        linear.addView(desc)

        for (i in 1..8) {
            val inputLayout = com.google.android.material.textfield.TextInputLayout(requireContext()).apply {
                hint = "技能 $i"
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 10 }
            }
            val edit = EditText(requireContext()).apply {
                maxLines = 1
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                filters = arrayOf(android.text.InputFilter.LengthFilter(4))
            }
            inputLayout.addView(edit)
            linear.addView(inputLayout)
            skillEdits.add(edit)
        }
        layout.addView(linear)
        return layout
    }
}

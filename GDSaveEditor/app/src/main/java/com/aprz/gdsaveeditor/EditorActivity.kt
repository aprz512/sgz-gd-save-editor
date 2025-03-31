package com.aprz.gdsaveeditor

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.aprz.gdsaveeditor.databinding.ActivityEditorBinding
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import kotlin.concurrent.thread

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding

    private lateinit var loading: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.updatePadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        loading = LoadingDialog(this)

        binding.btnReplaceSkills.setOnClickListener {
            replaceSkills()
        }

        loadSkills()
    }

    private fun loadSkills() {
        loading.show()
        thread {
            val fileUri = intent.getParcelableExtra<Uri>("uri") ?: return@thread
            val jsonObject = getFileAsJsonObject(fileUri) ?: return@thread
            val actors = jsonObject.get("actors") as JsonArray
            val diySkills = actors.filter {
                val item = it.asJsonObject
                item.has("diy_skills")
            }[0].asJsonObject.get("diy_skills").asString

            val skills = DiySkills.decryptSkill(diySkills)
            Handler(Looper.getMainLooper()).post {
                loading.dismiss()
                fillSkillText(skills)
            }
        }

    }

    private fun fillSkillText(skills: Skills) {
        binding.etSkill1.setText(skills.skill1)
        binding.etSkill2.setText(skills.skill2)
        binding.etSkill3.setText(skills.skill3)
        binding.etSkill4.setText(skills.skill4)
        binding.etSkill5.setText(skills.skill5)
        binding.etSkill6.setText(skills.skill6)
        binding.etSkill7.setText(skills.skill7)
        binding.etSkill8.setText(skills.skill8)
    }

    private fun replaceSkills() {
        loading.show()
        thread {
            val fileUri = intent.getParcelableExtra<Uri>("uri") ?: return@thread
            val jsonObject = getFileAsJsonObject(fileUri) ?: return@thread
            Handler(Looper.getMainLooper()).post {
                loading.dismiss()
                saveSkill(jsonObject, fileUri)
            }
        }
    }

    private fun getSkills(): Skills {
        return Skills(
            skill1 = binding.etSkill1.text.toString(),
            skill2 = binding.etSkill2.text.toString(),
            skill3 = binding.etSkill3.text.toString(),
            skill4 = binding.etSkill4.text.toString(),
            skill5 = binding.etSkill5.text.toString(),
            skill6 = binding.etSkill6.text.toString(),
            skill7 = binding.etSkill7.text.toString(),
            skill8 = binding.etSkill8.text.toString(),
        )
    }

    private fun saveSkill(jsonObject: JsonObject, fileUri: Uri) {
        val actors = jsonObject.get("actors") as JsonArray
        val actor = actors.filter {
            val item = it.asJsonObject
            item.has("diy_skills")
        }[0]
        actor.asJsonObject.addProperty("diy_skills", DiySkills.getEncryptSkillString(getSkills()))
        patchSaveFile(fileUri, jsonObject.toString())
        Toast.makeText(this, "替换完成", Toast.LENGTH_SHORT).show()
    }

    private fun patchSaveFile(fileUri: Uri, save: String) {
        val pfd: ParcelFileDescriptor = contentResolver.openFileDescriptor(fileUri, "w") ?: return
        pfd.use out@{ pfd ->
            val bw = BufferedWriter(FileWriter(pfd.fileDescriptor))
            bw.use inner@{ bw ->
                bw.write(save)
                return@inner
            }
            return@out
        }
    }

    private fun bakSaveFile(filePath: String) {
        val saveFile = File(filePath)
        saveFile.renameTo(File(saveFile.name.plus(".bak")))
    }

    private fun getFileAsJsonObject(fileUri: Uri): JsonObject? {
        return contentResolver.openFileDescriptor(fileUri, "r")?.use { pfd ->
            val bw = BufferedReader(FileReader(pfd.fileDescriptor))
            bw.use inner@{ bw ->
                val json = StringBuilder()
                bw.readLines().forEach { line ->
                    json.append(line)
                }
                return JsonParser.parseString(json.toString()).asJsonObject
            }
        }
    }

}
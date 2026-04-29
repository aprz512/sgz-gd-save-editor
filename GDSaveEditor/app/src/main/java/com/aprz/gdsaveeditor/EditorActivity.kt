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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aprz.gdsaveeditor.databinding.ActivityEditorBinding
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileReader
import java.io.FileWriter
import kotlin.concurrent.thread

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private lateinit var loading: LoadingDialog
    private lateinit var fileUri: Uri

    private lateinit var actorFragment: ActorListFragment
    private lateinit var cityFragment: CityListFragment
    private lateinit var skillFragment: SkillEditFragment
    private lateinit var settingsFragment: GameSettingsFragment

    private var jsonObject: JsonObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.updatePadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loading = LoadingDialog(this)
        fileUri = intent.getParcelableExtra<Uri>("uri") ?: run { finish(); return }

        actorFragment = ActorListFragment()
        cityFragment = CityListFragment()
        skillFragment = SkillEditFragment()
        settingsFragment = GameSettingsFragment()

        binding.viewPager.adapter = EditorPagerAdapter(supportFragmentManager, lifecycle,
            listOf(actorFragment, cityFragment, skillFragment, settingsFragment))

        val tabTitles = listOf("武将", "城池", "技能", "设置")
        tabTitles.forEach { binding.tabLayout.addTab(binding.tabLayout.newTab().setText(it)) }

        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                tab?.let { binding.viewPager.currentItem = it.position }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(position))
            }
        })

        binding.btnSave.setOnClickListener { saveFile() }

        loadFile()
    }

    private fun loadFile() {
        loading.show()
        thread {
            try {
                val json = getFileAsJsonObject(fileUri)
                jsonObject = json
                Handler(Looper.getMainLooper()).post {
                    val actors = json.get("actors")?.asJsonArray ?: JsonArray()
                    val citys = json.get("citys")?.asJsonArray ?: JsonArray()

                    actorFragment.actors = actors
                    actorFragment.filterActors()

                    cityFragment.citys = citys

                    skillFragment.actors = actors
                    skillFragment.loadSkills()

                    settingsFragment.loadSettings(json)

                    loading.dismiss()
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    loading.dismiss()
                    Toast.makeText(this, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveFile() {
        val json = jsonObject ?: return
        loading.show()
        thread {
            try {
                // Save skills
                val actors = json.get("actors")?.asJsonArray ?: JsonArray()
                val skillActor = (0 until actors.size())
                    .map { actors.get(it).asJsonObject }
                    .find { it.has("diy_skills") }
                if (skillActor != null) {
                    val skills = skillFragment.getSkills()
                    skillActor.addProperty("diy_skills", DiySkills.getEncryptSkillString(skills))
                }

                // Collect settings
                settingsFragment.collectSettings()

                patchSaveFile(fileUri, json.toString())
                Handler(Looper.getMainLooper()).post {
                    loading.dismiss()
                    Toast.makeText(this, "保存完成", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    loading.dismiss()
                    Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getFileAsJsonObject(fileUri: Uri): JsonObject {
        return contentResolver.openFileDescriptor(fileUri, "r")?.use { pfd ->
            val br = BufferedReader(FileReader(pfd.fileDescriptor))
            val json = StringBuilder()
            br.use { it.readLines().forEach { line -> json.append(line) } }
            JsonParser.parseString(json.toString()).asJsonObject
        } ?: throw Exception("无法读取文件")
    }

    private fun patchSaveFile(fileUri: Uri, save: String) {
        val pfd: ParcelFileDescriptor = contentResolver.openFileDescriptor(fileUri, "w") ?: return
        pfd.use { pfd ->
            val bw = BufferedWriter(FileWriter(pfd.fileDescriptor))
            bw.use { bw -> bw.write(save) }
        }
    }

    class EditorPagerAdapter(
        fm: FragmentManager,
        lifecycle: Lifecycle,
        private val fragments: List<Fragment>
    ) : FragmentStateAdapter(fm, lifecycle) {
        override fun createFragment(position: Int) = fragments[position]
        override fun getItemCount() = fragments.size
    }
}

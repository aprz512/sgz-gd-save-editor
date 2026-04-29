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
import java.io.FileReader
import kotlin.concurrent.thread

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private lateinit var loading: LoadingDialog
    private lateinit var fileUri: Uri

    private lateinit var actorFragment: ActorListFragment
    private lateinit var cityFragment: CityListFragment
    private lateinit var factionFragment: FactionListFragment
    private lateinit var skillFragment: SkillEditFragment
    private lateinit var settingsFragment: GameSettingsFragment
    private lateinit var equipFragment: EquipmentFragment

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
        factionFragment = FactionListFragment()
        skillFragment = SkillEditFragment()
        settingsFragment = GameSettingsFragment()
        equipFragment = EquipmentFragment()

        binding.viewPager.adapter = EditorPagerAdapter(supportFragmentManager, lifecycle,
            listOf(actorFragment, cityFragment, factionFragment, skillFragment, settingsFragment, equipFragment))
        binding.viewPager.offscreenPageLimit = 2

        val tabTitles = listOf("武将", "城池", "势力", "技能", "设置", "装备")
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
                    val vstates = json.get("vstates")?.asJsonArray ?: JsonArray()

                    actorFragment.actors = actors
                    actorFragment.filterActors()

                    cityFragment.citys = citys

                    factionFragment.vstates = vstates
                    factionFragment.actors = actors
                    factionFragment.citys = citys
                    factionFragment.render()

                    skillFragment.actors = actors
                    skillFragment.loadSkills()

                    settingsFragment.loadSettings(json)

                    equipFragment.weapons = json.get("weapons")?.asJsonArray
                    equipFragment.steeds = json.get("steeds")?.asJsonArray
                    equipFragment.suits = json.get("suits")?.asJsonArray
                    equipFragment.jewelrys = json.get("jewelrys")?.asJsonArray
                    equipFragment.refresh()

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
                if (skillFragment.skillsLoaded) {
                    val actors = json.get("actors")?.asJsonArray ?: JsonArray()
                    val skillActor = (0 until actors.size())
                        .map { actors.get(it).asJsonObject }
                        .find { it.has("diy_skills") }
                    if (skillActor != null) {
                        skillActor.addProperty("diy_skills", DiySkills.getEncryptSkillString(skillFragment.getSkills()))
                    }
                }

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
        val bytes = save.toByteArray(Charsets.UTF_8)
        // 使用 "wt" 模式确保截断旧内容，再写入正确长度
        contentResolver.openFileDescriptor(fileUri, "wt")?.use { pfd ->
            java.io.FileOutputStream(pfd.fileDescriptor).use { fos ->
                fos.write(bytes)
            }
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

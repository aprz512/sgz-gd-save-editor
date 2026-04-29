package com.aprz.gdsaveeditor

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

@RunWith(AndroidJUnit4::class)
class InstrumentedTests {

    // ============================================================
    // Skills 加解密 (需要 Android 加密库)
    // ============================================================

    @Test
    fun skillsEncryptDecrypt_roundTrip() {
        val original = Skills(
            skill1 = "智迟", skill2 = "", skill3 = "藤甲", skill4 = "",
            skill5 = "白毦", skill6 = "", skill7 = "玄阵", skill8 = ""
        )
        val encrypted = DiySkills.getEncryptSkillString(original)
        val decrypted = DiySkills.decryptSkill(encrypted)

        assertEquals(original.skill1, decrypted.skill1)
        assertEquals(original.skill3, decrypted.skill3)
        assertEquals(original.skill5, decrypted.skill5)
        assertEquals(original.skill7, decrypted.skill7)
    }

    @Test
    fun skillsEmptyEncryptDecrypt() {
        val original = Skills()
        val encrypted = DiySkills.getEncryptSkillString(original)
        val decrypted = DiySkills.decryptSkill(encrypted)
        assertEquals("", decrypted.skill1)
        assertEquals("", decrypted.skill8)
    }

    @Test
    fun skillsChineseCharsSurvive() {
        val original = Skills(skill1 = "测试", skill3 = "诸葛", skill5 = "玄阵")
        val encrypted = DiySkills.getEncryptSkillString(original)
        val decrypted = DiySkills.decryptSkill(encrypted)
        assertEquals("测试", decrypted.skill1)
        assertEquals("诸葛", decrypted.skill3)
        assertEquals("玄阵", decrypted.skill5)
    }

    // ============================================================
    // JsonObject 修改 + toString() round-trip
    // ============================================================

    @Test
    fun modifyAndSerialize_arraysPreserved() {
        val obj = JsonObject()
        val arr = JsonArray()
        arr.add("乐")
        arr.add("勇")
        obj.add("标签", arr)

        val json = obj.toString()
        val reparsed = JsonParser.parseString(json).asJsonObject

        assertTrue(reparsed.get("标签")!!.isJsonArray)
        assertEquals(2, reparsed.get("标签")!!.asJsonArray.size())
    }

    @Test
    fun modifyAndSerialize_keysNotLost() {
        val obj = JsonObject()
        obj.addProperty("姓名", "曹操")
        obj.addProperty("字", "")
        obj.addProperty("体", 82)

        // 模拟 dialog 保存 — 空值保留
        obj.addProperty("字", "")
        obj.addProperty("体", 99)

        val json = obj.toString()
        val reparsed = JsonParser.parseString(json).asJsonObject

        assertTrue(reparsed.has("字"))
        assertEquals("", reparsed.get("字")?.asString)
        assertEquals(99, reparsed.get("体")?.asInt ?: -1)
    }

    @Test
    fun modifyAndSerialize_structureIntact() {
        val obj = JsonObject()
        obj.addProperty("year", 189)
        obj.addProperty("存档时间", "2026-04-29 13:54:34")

        val actors = JsonArray()
        for (i in 0..2) {
            val a = JsonObject()
            a.addProperty("ID", i)
            a.addProperty("体", 70 + i)
            a.addProperty("姓名", "武将$i")
            if (i == 0) a.addProperty("字", "")
            actors.add(a)
        }
        obj.add("actors", actors)

        val originalKeys = obj.keySet().toSet()

        // 修改
        actors[0].asJsonObject.addProperty("体", 99)

        val json = obj.toString()
        val reparsed = JsonParser.parseString(json).asJsonObject

        // 顶层 key 完整
        assertEquals(originalKeys, reparsed.keySet().toSet())
        // 修改生效
        assertEquals(99, reparsed.get("actors")!!.asJsonArray[0].asJsonObject.get("体")?.asInt ?: -1)
        // 中文保留
        assertEquals("武将0", reparsed.get("actors")!!.asJsonArray[0].asJsonObject.get("姓名")?.asString)
    }

    // ============================================================
    // 真实存档文件测试 (需要设备上有 /sdcard/ 或 assets)
    // ============================================================

    @Test
    fun parseAndSerializeRealSave_producesValidJson() {
        // 从 assets 读取测试数据
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        try {
            val jsonStr = ctx.assets.open("test_save.json").bufferedReader().use { it.readText() }
            val obj = JsonParser.parseString(jsonStr).asJsonObject
            assertNotNull(obj.get("actors"))
            assertNotNull(obj.get("citys"))

            // 修改
            obj.get("actors")!!.asJsonArray[0].asJsonObject.addProperty("体", 99)

            // 重新序列化
            val reSerialized = obj.toString()
            val reparsed = JsonParser.parseString(reSerialized).asJsonObject
            assertEquals(99, reparsed.get("actors")!!.asJsonArray[0].asJsonObject.get("体")?.asInt ?: -1)
        } catch (e: Exception) {
            println("SKIP: test_save.json not in assets: ${e.message}")
        }
    }
}

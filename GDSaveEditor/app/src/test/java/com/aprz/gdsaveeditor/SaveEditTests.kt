package com.aprz.gdsaveeditor

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import java.io.BufferedReader
import java.io.FileReader

class SaveEditTests {

    // ============================================================
    // Skills 加解密 round-trip
    // ============================================================

    @Test @Ignore("需要 Android 加密库 — 在 androidTest 中运行")
    fun skillsEncryptDecrypt_roundTrip() {
        val original = Skills(
            skill1 = "智迟", skill2 = "", skill3 = "藤甲", skill4 = "",
            skill5 = "白毦", skill6 = "", skill7 = "玄阵", skill8 = ""
        )
        val encrypted = DiySkills.getEncryptSkillString(original)
        val decrypted = DiySkills.decryptSkill(encrypted)

        assertEquals(original.skill1, decrypted.skill1)
        assertEquals(original.skill2, decrypted.skill2)
        assertEquals(original.skill3, decrypted.skill3)
        assertEquals(original.skill5, decrypted.skill5)
        assertEquals(original.skill7, decrypted.skill7)
    }

    @Test @Ignore("需要 Android 加密库 — 在 androidTest 中运行")
    fun skillsEmptyEncryptDecrypt() {
        val original = Skills()
        val encrypted = DiySkills.getEncryptSkillString(original)
        val decrypted = DiySkills.decryptSkill(encrypted)

        assertEquals("", decrypted.skill1)
        assertEquals("", decrypted.skill8)
    }

    @Test @Ignore("需要 Android 加密库 — 在 androidTest 中运行")
    fun skillsChineseCharsSurviveRoundTrip() {
        val original = Skills(skill1 = "测试", skill3 = "诸葛")
        val encrypted = DiySkills.getEncryptSkillString(original)
        val decrypted = DiySkills.decryptSkill(encrypted)

        assertEquals("测试", decrypted.skill1)
        assertEquals("", decrypted.skill2)
        assertEquals("诸葛", decrypted.skill3)
    }

    // ============================================================
    // JsonObject 修改不破坏结构
    // ============================================================

    @Test
    fun jsonObject_addProperty_number_preservesType() {
        val obj = JsonObject()
        obj.addProperty("体", 71)

        val json = obj.toString()
        val reparsed = JsonParser.parseString(json).asJsonObject

        assertEquals(71, reparsed.get("体")?.asInt ?: -1)
        assertTrue(reparsed.get("体")!!.isJsonPrimitive)
        assertTrue(reparsed.get("体")!!.asJsonPrimitive.isNumber)
    }

    @Test
    fun jsonObject_addProperty_string_preservesType() {
        val obj = JsonObject()
        obj.addProperty("姓名", "曹操")

        val json = obj.toString()
        val reparsed = JsonParser.parseString(json).asJsonObject

        assertEquals("曹操", reparsed.get("姓名")?.asString)
        assertTrue(reparsed.get("姓名")!!.asJsonPrimitive.isString)
    }

    @Test
    fun jsonObject_add_array_preservesType() {
        val obj = JsonObject()
        obj.addProperty("姓名", "曹操")
        val arr = JsonArray()
        arr.add("乐")
        obj.add("标签", arr)

        val json = obj.toString()
        val reparsed = JsonParser.parseString(json).asJsonObject

        assertNotNull(reparsed.get("标签"))
        assertTrue(reparsed.get("标签")!!.isJsonArray)
        assertEquals(1, reparsed.get("标签")!!.asJsonArray.size())
        assertEquals("乐", reparsed.get("标签")!!.asJsonArray[0].asString)
    }

    @Test
    fun jsonObject_remove_deletesKey() {
        val obj = JsonObject()
        obj.addProperty("字", "")
        obj.addProperty("姓名", "测试")
        obj.remove("字") // BUG: 删除字段

        val json = obj.toString()
        val reparsed = JsonParser.parseString(json).asJsonObject

        assertFalse(reparsed.has("字")) // 字段丢失！
        assertTrue(reparsed.has("姓名"))
    }

    @Test
    fun jsonObject_keepEmpty_preservesKey() {
        val obj = JsonObject()
        obj.addProperty("字", "")
        obj.addProperty("姓名", "测试")
        obj.addProperty("字", "") // FIX: 保留空值

        val json = obj.toString()
        val reparsed = JsonParser.parseString(json).asJsonObject

        assertTrue(reparsed.has("字")) // 字段保留！
        assertEquals("", reparsed.get("字")?.asString)
        assertTrue(reparsed.has("姓名"))
    }

    @Test
    fun jsonObject_toString_validJson() {
        val obj = JsonObject()
        obj.addProperty("COMPABILITY", 2)
        obj.addProperty("VERSION", "2.510")
        obj.addProperty("year", 189)
        obj.addProperty("存档时间", "2026-04-29 13:54:34")

        val actors = JsonArray()
        val actor = JsonObject()
        actor.addProperty("ID", 0)
        actor.addProperty("体", 71)
        actor.addProperty("姓名", "鲍信")
        actor.addProperty("字", "")
        actors.add(actor)
        obj.add("actors", actors)

        val json = obj.toString()
        // 验证可以重新解析
        val reparsed = JsonParser.parseString(json)
        assertTrue(reparsed.isJsonObject)
        assertEquals(2, reparsed.asJsonObject.get("COMPABILITY")?.asInt ?: -1)
        assertEquals("鲍信", reparsed.asJsonObject
            .get("actors")!!.asJsonArray[0].asJsonObject
            .get("姓名")?.asString)
    }

    @Test
    fun jsonObject_modifyThenToString_allKeysPreserved() {
        // 模拟真实存档结构
        val obj = JsonObject()
        obj.addProperty("COMPABILITY", 2)
        obj.addProperty("VERSION", "2.510")
        obj.addProperty("year", 189)
        obj.addProperty("month", 1)
        obj.addProperty("存档时间", "2026-04-29 13:54:34")

        val actors = JsonArray()
        for (i in 0..2) {
            val a = JsonObject()
            a.addProperty("ID", i)
            a.addProperty("体", 70 + i)
            a.addProperty("姓名", "武将$i")
            if (i == 0) a.addProperty("字", "")
            if (i == 2) {
                val tags = JsonArray()
                tags.add("乐")
                a.add("标签", tags)
            }
            actors.add(a)
        }
        obj.add("actors", actors)

        val gameSet = JsonObject()
        gameSet.addProperty("人物大限", "有大限")
        gameSet.addProperty("技能系统", "否")
        obj.add("game_set", gameSet)

        // 记录原始 key 集合
        val topKeys = obj.keySet().toSet()
        val actor0Keys = actors[0].asJsonObject.keySet().toSet()
        val actor2Keys = actors[2].asJsonObject.keySet().toSet()

        // 模拟修改
        actors[0].asJsonObject.addProperty("体", 99) // number
        actors[0].asJsonObject.addProperty("字", "") // 空字符串保留
        actors[2].asJsonObject.addProperty("武", 88) // 新增字段
        gameSet.addProperty("技能系统", "是") // 修改字符串

        // 序列化 & 重新解析
        val json = obj.toString()
        val reparsed = JsonParser.parseString(json).asJsonObject

        // 验证顶层 key
        assertEquals(topKeys, reparsed.keySet().toSet())

        // 验证武将 key 全部保留
        val reparsedActors = reparsed.get("actors")!!.asJsonArray
        assertEquals(actor0Keys, reparsedActors[0].asJsonObject.keySet().toSet())

        // 验证修改生效
        assertEquals(99, reparsedActors[0].asJsonObject.get("体")?.asInt ?: -1)
        assertEquals("", reparsedActors[0].asJsonObject.get("字")?.asString)

        // 验证标签还是数组
        assertTrue(reparsedActors[2].asJsonObject.get("标签")!!.isJsonArray)

        // 验证 game_set
        assertEquals("是", reparsed.get("game_set")!!.asJsonObject.get("技能系统")?.asString)
    }

    // ============================================================
    // 真实文件测试
    // ============================================================

    @Test
    fun realSaveFile_canBeParsedAndReSerialized() {
        val file = java.io.File("../../../7.sav")
        if (!file.exists()) {
            println("SKIP: 7.sav not found")
            return
        }
        val json = BufferedReader(FileReader(file, Charsets.UTF_8)).use { it.readText() }

        // 可以解析
        val obj = JsonParser.parseString(json).asJsonObject
        assertNotNull(obj.get("actors"))
        assertNotNull(obj.get("citys"))
        assertNotNull(obj.get("vstates"))

        // 序列化后可重新解析
        val reSerialized = obj.toString()
        val reparsed = JsonParser.parseString(reSerialized).asJsonObject

        assertEquals(
            obj.get("actors")!!.asJsonArray.size(),
            reparsed.get("actors")!!.asJsonArray.size()
        )
        assertEquals(
            obj.get("citys")!!.asJsonArray.size(),
            reparsed.get("citys")!!.asJsonArray.size()
        )
    }

    @Test
    fun realSaveFile_modifyActor_keysPreserved() {
        val file = java.io.File("../../../7.sav")
        if (!file.exists()) {
            println("SKIP: 7.sav not found")
            return
        }
        val json = BufferedReader(FileReader(file, Charsets.UTF_8)).use { it.readText() }
        val obj = JsonParser.parseString(json).asJsonObject

        val actors = obj.get("actors")!!.asJsonArray

        // 收集所有武将的原始 key 集合
        val originalKeys = (0 until actors.size()).map {
            actors.get(it).asJsonObject.keySet().toSet()
        }

        // 修改第 0 个武将
        val actor0 = actors[0].asJsonObject
        actor0.addProperty("体", 99)
        actor0.addProperty("武", 88)

        // 如果有标签，修改标签
        val actor2 = actors[2].asJsonObject
        if (actor2.has("标签")) {
            val newTags = JsonArray()
            newTags.add("新标签")
            actor2.add("标签", newTags)
        }

        // 序列化 & 重新解析
        val reSerialized = obj.toString()
        val reparsed = JsonParser.parseString(reSerialized).asJsonObject
        val reparsedActors = reparsed.get("actors")!!.asJsonArray

        // 验证 key 一致
        for (i in originalKeys.indices) {
            val newKeys = reparsedActors[i].asJsonObject.keySet().toSet()
            if (originalKeys[i] != newKeys) {
                val missing = originalKeys[i] - newKeys
                val extra = newKeys - originalKeys[i]
                fail("武将 $i key 不一致: missing=$missing, extra=$extra")
            }
        }

        // 验证修改生效
        assertEquals(99, reparsedActors[0].asJsonObject.get("体")?.asInt ?: -1)
        assertEquals(88, reparsedActors[0].asJsonObject.get("武")?.asInt ?: -1)

        println("PASS: ${actors.size()} 个武将 key 全部保留")
    }

    // ============================================================
    // 边界情况
    // ============================================================

    @Test
    fun emptyJsonObject_toString_isValid() {
        val obj = JsonObject()
        val json = obj.toString()
        assertEquals("{}", json)
        val reparsed = JsonParser.parseString(json)
        assertTrue(reparsed.isJsonObject)
    }

    @Test
    fun chineseCharacters_toString_notEscaped() {
        val obj = JsonObject()
        obj.addProperty("姓名", "诸葛亮")
        val json = obj.toString()
        // 中文不应该被转义成 \uXXXX
        assertTrue(json.contains("诸葛亮"))
    }

    @Test
    fun skills_gsonSerialization() {
        val gson = com.google.gson.Gson()
        val skills = Skills(skill1 = "智迟", skill3 = "藤甲")
        val json = gson.toJson(skills)
        assertTrue(json.contains("LV1"))
        assertTrue(json.contains("智迟"))
        assertTrue(json.contains("LV3"))
        assertTrue(json.contains("藤甲"))

        val parsed = gson.fromJson(json, Skills::class.java)
        assertEquals("智迟", parsed.skill1)
        assertEquals("", parsed.skill2)
        assertEquals("藤甲", parsed.skill3)
    }
}

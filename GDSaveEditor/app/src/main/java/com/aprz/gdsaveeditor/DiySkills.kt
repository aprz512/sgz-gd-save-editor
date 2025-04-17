package com.aprz.gdsaveeditor

import android.annotation.SuppressLint
import android.util.Base64
import android.util.Base64.DEFAULT
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.Signature
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


object DiySkills {

    @JvmStatic
    fun main(args: Array<String>) {
        decryptSkill("65373064653430313837633639313636326233396661626462373861303264383637343932353334316263626465623237326562343137323165626138626131303565343936326534323563366565333738303534633635373064333736633533356233383565306435333862643237353331666161353966343931643930643338343636383762393935333461333866303164373636386333626239326162303633396266336263623634613263356334313164663238613936313761636461636332333364363638366538396365633263366263383936623065633336308b7acd5e20c46faab697945a814c06cb59c56b3024bf401a933e1daf8ee0188a7c491e2a9a37bbe527df30c46bc4a836246a8ed33f131016f23260a95dae2e1b4002")
//        decryptSkill("353039656134666264393838663635333161383436653632336265323163363331313032303065623836656161313664363963616238363564353335613739363032363037646665343666326130663962303563356234633937623239373261353464343235323435646662616561353330383832393564643366373465316361386164643165313365386136666534303465393334316335386162636538366461333438353238633532373937376665613863343963303335643339396135653438383834383863633634303333623533326562366439663434323561356131346138313234633239636432623036613865333135313930313132666336632252ddd14d4b03ee0a8dab8ad8aa8b59e2ac18e8198ddface37139ec62cd54884793ece3916cd223d6da46a92a360e616e9566871ad903875734bb6da5f695d34002")
//        Log.e(
//            "aprz",
//            getEncryptSkillString(
//                Skills(
//                    skill1 = "智迟",
//                    skill3 = "藤甲",
//                    skill5 = "白毦",
//                    skill7 = "玄阵",
//                )
//            )
//        )
    }

    fun decryptSkill(data: String): Skills {
        val bytesData = decodeHex(data)
        Log.e("aprz", "bytesData = ${bytesData.contentToString()}")
        val signVersion = bytesData[bytesData.size - 1]
        Log.e("aprz", "signVersion = $signVersion")
        val signLength = bytesData[bytesData.size - 2]
        Log.e("aprz", "signLength = $signLength")
        val skillData = bytesData.sliceArray(IntRange(0, bytesData.size - signLength - 3))
        Log.e("aprz", "skillData = ${skillData.contentToString()}")
        Log.e("aprz", "skillData = ${String(skillData)}")
        val hexDecode = decodeHex(String(skillData))
        Log.e("aprz", "hexDecode = ${hexDecode.contentToString()}")
        Log.e("aprz", "decryptWithECB = ${encryptWithECB(hexDecode).contentToString()}")
        val skillString = String(decryptWithECB(hexDecode))
        Log.e("aprz", "skillString = $skillString")
        return Gson().fromJson(skillString, Skills::class.java)
    }

    private fun decodeHex(data: String): ByteArray {
        return data.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    fun getEncryptSkillString(skills: Skills): String {
        val skillJson = generateSkillsJson(skills)
        val skillByteArray = skillJson.toByteArray()
//        Log.e("aprz", "skillByteArray = ${skillByteArray.contentToString()}")
        val ecbEncryptByteArray = encryptWithECB(skillByteArray)
//        Log.e("aprz", "ecbEncryptByteArray = ${ecbEncryptByteArray.contentToString()}")
        val hexString = bytesToHex(ecbEncryptByteArray)
//        Log.e("aprz", "hexString = $hexString")
        val asciiString = toAsciiByteArray(hexString)
//        Log.e("aprz", "asciiString = ${asciiString.contentToString()}")
        val sign = getSign(asciiString)
//        Log.e("aprz", "sign = ${sign.contentToString()}")
        val data = asciiString + sign + byteArrayOf(64, 2)
//        Log.e("aprz", "data = ${data.contentToString()}")
        return bytesToHex(data)
    }

    fun getSign(data: ByteArray): ByteArray {
        val privateKey = "MIIBOwIBAAJBAOTW56JN2BCV/G//PQn4/Kz06h92jmdbUIM+KmzQrbvNVwiobwEd\n" +
                "3VvEsmDa6pQ0JFgVY8dr66Hc18HLShwJEq8CAwEAAQJADMHUQO6RBH+wBnhWqUcp\n" +
                "ouS2ZpGf57AmAWMGT3GktcrmOR+W4vjS9B2iFH/JhJDBMkQ+5py9+fMCE5gc0gMS\n" +
                "RQIhAPZUCEJAAl6y1FggoiVpaSUT9g9TdBYJfr/6wOPfqXebAiEA7dMVioDfqQ5t\n" +
                "zH2KySLtEVe2ANWroJLwL8Ts3vUVxH0CIQC7hlWTOe+T8Eg/nvhRyuHE3GFiYYHq\n" +
                "lOftdxQJZmg5KQIgWg+fjq2zBSAzsEaycezJ/dFLWRGRRuOeFVjropsJPTkCIQDt\n" +
                "p9I6LkIJqfid8y4YC1mSFF0g4ClEoAIv918R47hAEA==\n"


        val keySpec = PKCS8EncodedKeySpec(Base64.decode(privateKey, DEFAULT))
        val keyFactory = KeyFactory.getInstance("RSA")
        val pKey = keyFactory.generatePrivate(keySpec) as RSAPrivateKey
        val signature: Signature = Signature.getInstance("MD5withRSA")
        signature.initSign(pKey)
        signature.update(data)
        return signature.sign()
    }

    private fun fillDataWithZero(data: ByteArray): ByteArray {
        val x = data.size % 16
        if (x == 0) {
            return data
        } else {
            val zeros = ByteArray(16 - x) {
                0
            }
            return data + zeros
        }
    }

    private fun toAsciiByteArray(data: String): ByteArray {
        return data.toByteArray(StandardCharsets.US_ASCII)
    }

    @SuppressLint("GetInstance")
    private fun encryptWithECB(bytes: ByteArray): ByteArray {
        val key = SecretKeySpec("gd secret key!!!".toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/ECB/ZeroBytePadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(bytes)
        return encrypted
    }

    @SuppressLint("GetInstance")
    private fun decryptWithECB(bytes: ByteArray): ByteArray {
        val key = SecretKeySpec("gd secret key!!!".toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/ECB/ZeroBytePadding")
        cipher.init(Cipher.DECRYPT_MODE, key)
        val decrypted = cipher.doFinal(bytes)
        return decrypted
    }

    private fun generateSkillsJson(skills: Skills): String {
        return Gson().toJson(skills)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val chars = "0123456789abcdef".toCharArray()

        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = chars[v ushr 4]
            hexChars[j * 2 + 1] = chars[v and 0x0F]
        }
        return String(hexChars)
    }

}

data class Skills(
    @SerializedName("LV1") val skill1: String = "",
    @SerializedName("LV2") val skill2: String = "",
    @SerializedName("LV3") val skill3: String = "",
    @SerializedName("LV4") val skill4: String = "",
    @SerializedName("LV5") val skill5: String = "",
    @SerializedName("LV6") val skill6: String = "",
    @SerializedName("LV7") val skill7: String = "",
    @SerializedName("LV8") val skill8: String = "",
)


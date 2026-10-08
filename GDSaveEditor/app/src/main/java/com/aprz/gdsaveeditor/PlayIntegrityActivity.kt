package com.aprz.gdsaveeditor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.aprz.gdsaveeditor.databinding.ActivityPlayIntegrityBinding
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityServiceException
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.google.android.play.core.integrity.model.IntegrityErrorCode
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlayIntegrityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayIntegrityBinding
    private var standardTokenProvider: StandardIntegrityTokenProvider? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayIntegrityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.updatePadding(
                systemBars.left, systemBars.top, systemBars.right, systemBars.bottom
            )
            insets
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnPrepare.setOnClickListener { prepareStandardTokenProvider() }
        binding.btnStandard.setOnClickListener { requestStandardToken() }
        binding.btnClassic.setOnClickListener { requestClassicToken() }
        binding.btnCopy.setOnClickListener { copyToken() }
    }

    private fun prepareStandardTokenProvider() {
        val projectNumber = cloudProjectNumber() ?: return
        setBusy(true)
        log("正在准备标准 Token Provider（后台预热，可能需要几秒）…")
        IntegrityManagerFactory.createStandard(applicationContext)
            .prepareIntegrityToken(
                PrepareIntegrityTokenRequest.builder()
                    .setCloudProjectNumber(projectNumber)
                    .build()
            )
            .addOnSuccessListener { provider ->
                setBusy(false)
                standardTokenProvider = provider
                binding.btnStandard.isEnabled = true
                binding.btnPrepare.text = "标准 Provider 已就绪 ✓"
                log("标准 Token Provider 准备完成，可以发起低延迟请求了")
            }
            .addOnFailureListener { e ->
                setBusy(false)
                standardTokenProvider = null
                log("准备标准 Provider 失败: ${describeError(e)}")
            }
    }

    private fun requestStandardToken() {
        val provider = standardTokenProvider
        if (provider == null) {
            log("请先点击「准备标准 Provider」")
            return
        }
        val requestHash = computeRequestHash("gd-content-${System.currentTimeMillis()}")
        setBusy(true)
        log("正在请求标准 Token，requestHash=$requestHash")
        provider.request(
            StandardIntegrityTokenRequest.builder()
                .setRequestHash(requestHash)
                .build()
        )
            .addOnSuccessListener { response -> onTokenReceived("标准", response.token()) }
            .addOnFailureListener { e ->
                setBusy(false)
                log("请求标准 Token 失败: ${describeError(e)}")
            }
    }

    private fun requestClassicToken() {
        val projectNumber = cloudProjectNumber() ?: return
        val nonce = generateNonce()
        setBusy(true)
        log("正在请求经典 Token，nonce=$nonce")
        IntegrityManagerFactory.create(applicationContext)
            .requestIntegrityToken(
                IntegrityTokenRequest.builder()
                    .setCloudProjectNumber(projectNumber)
                    .setNonce(nonce)
                    .build()
            )
            .addOnSuccessListener { response -> onTokenReceived("经典", response.token()) }
            .addOnFailureListener { e ->
                setBusy(false)
                log("请求经典 Token 失败: ${describeError(e)}")
            }
    }

    private fun onTokenReceived(type: String, token: String) {
        setBusy(false)
        binding.tvToken.text = token
        log("获取$type Token 成功（长度 ${token.length}），请发送到服务器调用 decodeIntegrityToken 解密验证")
        logTokenToLogcat(type, token)
    }

    private fun logTokenToLogcat(type: String, token: String) {
        Log.d(TAG, "$type Token（共 ${token.length} 字符，分段打印）:")
        token.chunked(500).forEachIndexed { index, chunk ->
            Log.d(TAG, "token[$index]=$chunk")
        }
    }

    private fun copyToken() {
        val token = binding.tvToken.text?.toString().orEmpty()
        if (token.isEmpty() || token.startsWith("（")) return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("integrity_token", token))
        Toast.makeText(this, "Token 已复制", Toast.LENGTH_SHORT).show()
    }

    private fun cloudProjectNumber(): Long? =
        binding.etCloudProject.text?.toString()?.trim()?.toLongOrNull().also {
            if (it == null) {
                Toast.makeText(this, "Cloud 项目编号无效", Toast.LENGTH_SHORT).show()
            }
        }

    private fun setBusy(busy: Boolean) {
        binding.btnPrepare.isEnabled = !busy
        binding.btnClassic.isEnabled = !busy
        binding.btnStandard.isEnabled = !busy && standardTokenProvider != null
    }

    private fun describeError(e: Throwable): String = when (e) {
        is StandardIntegrityException ->
            "错误码 ${e.errorCode} (${standardErrorCodeName(e.errorCode)}): ${e.message}"

        is IntegrityServiceException ->
            "错误码 ${e.errorCode} (${classicErrorCodeName(e.errorCode)}): ${e.message}"

        else -> "${e.javaClass.simpleName}: ${e.message}"
    }

    private fun standardErrorCodeName(code: Int): String =
        StandardIntegrityErrorCode::class.java.fields
            .firstOrNull { it.type == Int::class.javaPrimitiveType && it.getInt(null) == code }
            ?.name ?: "UNKNOWN"

    private fun classicErrorCodeName(code: Int): String =
        IntegrityErrorCode::class.java.fields
            .firstOrNull { it.type == Int::class.javaPrimitiveType && it.getInt(null) == code }
            ?.name ?: "UNKNOWN"

    private fun log(message: String) {
        val time = timeFormat.format(Date())
        val old = binding.tvLog.text?.toString().orEmpty()
        binding.tvLog.text = if (old.isEmpty()) "[$time] $message" else "$old\n[$time] $message"
        Log.d(TAG, message)
    }

    private companion object {
        const val TAG = "PlayIntegrity"
    }
}

private fun generateNonce(): String {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP)
}

private fun computeRequestHash(content: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
    return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP)
}

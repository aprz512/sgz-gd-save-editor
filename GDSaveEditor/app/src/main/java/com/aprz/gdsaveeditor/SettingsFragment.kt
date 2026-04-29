package com.aprz.gdsaveeditor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aprz.gdsaveeditor.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvVersion.text = BuildConfig.VERSION_NAME

        binding.btnQq.setOnClickListener {
            if (!joinQQGroup()) {
                Toast.makeText(context, "拉起QQ失败", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGithub.setOnClickListener {
            val uri = Uri.parse("https://github.com/aprz512/sgz-gd-save-editor")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun joinQQGroup(): Boolean {
        val key = "6q88cDfKNP7kFuNzU-cPRvSbzdpM2f23"
        val intent = Intent()
        intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D$key"))
        return try {
            startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}

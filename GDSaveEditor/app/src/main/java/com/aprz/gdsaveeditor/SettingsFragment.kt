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


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
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
        binding.buttonJoin.setOnClickListener {
            val result = joinQQGroup()
            if (!result) {
                Toast.makeText(context, "拉起QQ失败", Toast.LENGTH_SHORT).show()
            }
        }
        binding.buttonGithub.setOnClickListener {
            val uri = Uri.parse("https://github.com/aprz512/sgz-gd-save-editor")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /****************
     *
     * 发起添加群流程。群号：［霸王的大陆GD］存档(674950185) 的 key 为： 6q88cDfKNP7kFuNzU-cPRvSbzdpM2f23
     * 调用 joinQQGroup(6q88cDfKNP7kFuNzU-cPRvSbzdpM2f23) 即可发起手Q客户端申请加群 ［霸王的大陆GD］存档(674950185)
     *
     * @param key 由官网生成的key
     * @return 返回true表示呼起手Q成功，返回false表示呼起失败
     */
    private fun joinQQGroup(): Boolean {
        val key = "6q88cDfKNP7kFuNzU-cPRvSbzdpM2f23"
        val intent = Intent()
        intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D$key"))
        // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
            return true
        } catch (e: Exception) {
            // 未安装手Q或安装的版本不支持
            return false
        }
    }

}
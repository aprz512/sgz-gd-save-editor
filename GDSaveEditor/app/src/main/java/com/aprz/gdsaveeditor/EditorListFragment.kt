package com.aprz.gdsaveeditor

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.aprz.gdsaveeditor.databinding.FragmentEditorListBinding
import com.aprz.gdsaveeditor.databinding.ItemEditorListBinding
import java.io.File
import kotlin.concurrent.thread


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class EditorListFragment : Fragment() {

    private var _binding: FragmentEditorListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var isQuerying = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditorListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.srlPullToRefresh.isRefreshing = true
        binding.srlPullToRefresh.setOnRefreshListener {
            queryFiles()
        }

        queryFiles()
    }

    private fun queryFiles() {
        if (isQuerying) {
            return
        }
        isQuerying = true
        thread {
            val rootFile = File("${Environment.getExternalStorageDirectory()}/sgz/save")
            val regex = Regex("\\d+")
            val fileList = rootFile.walk()
                .filter {
//                    regex.matches(it.nameWithoutExtension)
                    it.name.endsWith(".sav")
                }
                .toList().sorted()
            mainHandler.post {
                binding.srlPullToRefresh.isRefreshing = false
                showList(fileList)
            }
            isQuerying = false
        }
    }

    private fun showList(files: List<File>) {
        binding.rvList.adapter = EditorListAdapter(files)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class EditorListAdapter(private val list: List<File>) :
    RecyclerView.Adapter<EditorListAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding =
            ItemEditorListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val myViewHolder = MyViewHolder(binding)
        return myViewHolder
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class MyViewHolder(private val binding: ItemEditorListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(file: File) {
            binding.tvFileName.text = file.name
            binding.root.setOnClickListener {
                binding.root.context.apply {
                    startActivity(Intent(this, EditorActivity::class.java).apply {
                        putExtra("path", file.absolutePath)
                    })
                }
            }
        }
    }
}
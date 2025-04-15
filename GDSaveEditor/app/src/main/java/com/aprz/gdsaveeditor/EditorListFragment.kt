package com.aprz.gdsaveeditor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aprz.gdsaveeditor.databinding.FragmentEditorListBinding
import com.aprz.gdsaveeditor.databinding.ItemEditorListBinding
import com.aprz.gdsaveeditor.document.CachingDocumentFile
import com.aprz.gdsaveeditor.document.DirectoryFragmentViewModel


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class EditorListFragment : Fragment() {

    private var _binding: FragmentEditorListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val viewModel: DirectoryFragmentViewModel by viewModels()
    private val adapter: EditorListAdapter = EditorListAdapter()
    private var cacheUri: Uri? = null

    private val openDocumentTreeLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri ?: return@registerForActivityResult
            context?.contentResolver?.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            showDirectoryContents(uri)
            this.cacheUri = uri
        }


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
            if (cacheUri == null) {
                binding.srlPullToRefresh.isRefreshing = false
                openDirectory()
            } else {
                showDirectoryContents(cacheUri!!)
            }
        }
        binding.rvList.adapter = adapter

        showOpenDocumentTreeButton()

        observerDocument()
    }

    private fun observerDocument() {
        viewModel.documents.observe(viewLifecycleOwner, Observer { documents ->
            documents?.let {
                binding.srlPullToRefresh.isRefreshing = false
                adapter.submitList(documents)
            }
        })
    }

    private fun showOpenDocumentTreeButton() {
        binding.openDocumentTree.visibility = View.VISIBLE
        binding.srlPullToRefresh.visibility = View.INVISIBLE

        binding.openDocumentTree.setOnClickListener {
            openDirectory()
        }
    }

    private fun openDirectory() {
        openDocumentTreeLauncher.launch(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showDirectoryContents(uri: Uri) {
        binding.openDocumentTree.visibility = View.INVISIBLE
        binding.srlPullToRefresh.visibility = View.VISIBLE

        viewModel.loadDirectory(uri)
    }
}

class EditorListAdapter() :
    ListAdapter<CachingDocumentFile, MyViewHolder>(object :
        DiffUtil.ItemCallback<CachingDocumentFile>() {
        override fun areItemsTheSame(
            oldItem: CachingDocumentFile,
            newItem: CachingDocumentFile
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: CachingDocumentFile,
            newItem: CachingDocumentFile
        ): Boolean {
            return oldItem.uri == newItem.uri
        }
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding =
            ItemEditorListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val myViewHolder = MyViewHolder(binding)
        return myViewHolder
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class MyViewHolder(private val binding: ItemEditorListBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(file: CachingDocumentFile) {
        binding.tvFileName.text = file.name
        binding.root.setOnClickListener {
            binding.root.context.apply {
                startActivity(Intent(this, EditorActivity::class.java).apply {
                    putExtra("uri", file.uri)
                })
            }
        }
    }
}
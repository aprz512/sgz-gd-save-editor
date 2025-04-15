package com.aprz.gdsaveeditor.document

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DirectoryFragmentViewModel(application: Application) : AndroidViewModel(application) {
    private val _documents = MutableLiveData<List<CachingDocumentFile>>()
    val documents = _documents

    fun loadDirectory(directoryUri: Uri) {
        viewModelScope.launch(context = Dispatchers.IO) {
            val documentsTree =
                DocumentFile.fromTreeUri(getApplication(), directoryUri) ?: return@launch
            val childDocuments = documentsTree.listFiles().toCachingList()

            // It's much nicer when the documents are sorted by something, so we'll sort the documents
            // we got by name. Unfortunate there may be quite a few documents, and sorting can take
            // some time, so we'll take advantage of coroutines to take this work off the main thread.
            val sortList = childDocuments.toMutableList()
                .filter {
                    it.name?.endsWith(".sav") == true
                }.sortedBy {
                    it.name?.replace(".sav", "")
                }
            _documents.postValue(sortList)
        }
    }

}
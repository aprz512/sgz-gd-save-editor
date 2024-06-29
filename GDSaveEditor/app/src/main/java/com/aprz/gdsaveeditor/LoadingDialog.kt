package com.aprz.gdsaveeditor

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.aprz.gdsaveeditor.databinding.DialogLoadingBinding

class LoadingDialog(context: Context) : AlertDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inflate = DialogLoadingBinding.inflate(layoutInflater)
        setContentView(inflate.root)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }

}
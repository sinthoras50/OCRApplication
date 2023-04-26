package com.example.templateeditorapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.templateeditorapp.ui.editor.EditorFragment
import com.example.templateeditorapp.ui.overview.OverviewFragment
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import com.example.templateeditorapp.utils.Assets
import com.example.templateeditorapp.utils.TransactionValidationUtils
import java.io.File


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
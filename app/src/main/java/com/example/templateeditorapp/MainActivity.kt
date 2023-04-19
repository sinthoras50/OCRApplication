package com.example.templateeditorapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.templateeditorapp.ui.editor.EditorFragment
import com.example.templateeditorapp.ui.overview.OverviewFragment
import androidx.activity.viewModels
import androidx.fragment.app.viewModels


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        if (savedInstanceState == null) {
//            supportFragmentManager.beginTransaction()
//                .replace(R.id.container, OverviewFragment.newInstance())
//                .commitNow()
//        }
    }
}
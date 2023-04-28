package com.example.templateeditorapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.templateeditorapp.db.ImageDatabase
import com.example.templateeditorapp.ui.opencv.CameraViewModel
import java.lang.IllegalArgumentException


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: SharedViewModel

    private val db: ImageDatabase by lazy {
        (application as OcrApp).db
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                if (modelClass.isAssignableFrom(SharedViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return SharedViewModel(db) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }).get(SharedViewModel::class.java)

        val reqWidth = resources.displayMetrics.widthPixels
        val reqHeight = resources.displayMetrics.heightPixels

        viewModel.loadImages(this, reqWidth, reqHeight)

        installSplashScreen().apply {
            setKeepOnScreenCondition {
                viewModel.isLoading.value!!
            }
        }

        setContentView(R.layout.activity_main)
    }
}
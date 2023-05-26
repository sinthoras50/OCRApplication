package com.example.templateeditorapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.templateeditorapp.db.ImageDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: SharedImageViewModel
    private val db: ImageDatabase by lazy {
        (application as OcrApp).db
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                if (modelClass.isAssignableFrom(SharedImageViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return SharedImageViewModel(db) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }).get(SharedImageViewModel::class.java)

        val reqWidth = resources.displayMetrics.widthPixels
        val reqHeight = resources.displayMetrics.heightPixels


        // the database callback is called when the dao interacts with the DB for the first time thus we call a dummy
        // SELECT method to prepopulate the DB with the default data
        // delay to let the default postal cheque get copied from the assets into the devices memory before loading
        viewModel.viewModelScope.launch {
            db.annotatedImageDao().onCreate()
            Log.d("DBTEST", "DB before delay")
            delay(250)
            Log.d("DBTEST", "DB after delay")
            viewModel.loadImages(this@MainActivity, reqWidth, reqHeight)
        }

        installSplashScreen().apply {
            setKeepOnScreenCondition {
                viewModel.isLoading.value!!
            }
        }

        setContentView(R.layout.activity_main)
    }
}
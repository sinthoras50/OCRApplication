package com.example.templateeditorapp

import android.app.Application
import com.example.templateeditorapp.db.ImageDatabase

class OcrApp : Application() {

    val db: ImageDatabase by lazy {
        ImageDatabase.getInstance(applicationContext)
    }

}
package com.example.templateeditorapp

import android.app.Application
import com.example.templateeditorapp.db.ImageDatabase
import com.jakewharton.threetenabp.AndroidThreeTen

class OcrApp : Application() {

    val db: ImageDatabase by lazy {
        ImageDatabase.getInstance(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }

}
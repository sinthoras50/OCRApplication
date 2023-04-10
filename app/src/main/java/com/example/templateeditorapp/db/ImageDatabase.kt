package com.example.templateeditorapp.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [AnnotatedImage::class], version = 2, exportSchema = false)
@TypeConverters(DataConverter::class)
abstract class ImageDatabase : RoomDatabase() {

    abstract fun annotatedImageDao(): AnnotatedImageDao

    companion object {
        private const val DB_NAME = "image_database"

        @Volatile
        private var INSTANCE: ImageDatabase? = null

        fun getInstance(context: Context): ImageDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ImageDatabase::class.java,
                    DB_NAME
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }

    }


}
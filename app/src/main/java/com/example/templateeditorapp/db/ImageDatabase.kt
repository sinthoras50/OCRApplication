package com.example.templateeditorapp.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.templateeditorapp.utils.Assets

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
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): ImageDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ImageDatabase::class.java,
                DB_NAME
            )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        Log.d("DBTEST", "DB callback")
                        Assets.createDefaultChequeEntry(context, "", getInstance(context))
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
        }
    }


}
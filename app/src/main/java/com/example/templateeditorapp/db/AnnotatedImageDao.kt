package com.example.templateeditorapp.db

import androidx.room.*


@Dao
interface AnnotatedImageDao {

    @Upsert
    suspend fun insertImage(annotatedImage: AnnotatedImage)

    @Query("SELECT * FROM annotated_images WHERE imageName = :imageName")
    suspend fun getImage(imageName: String): AnnotatedImage?

    @Query("SELECT * FROM annotated_images")
    suspend fun getAllImages(): List<AnnotatedImage>

    @Query("DELETE FROM annotated_images WHERE imageName = :imageName")
    suspend fun deleteImage(imageName: String)
}
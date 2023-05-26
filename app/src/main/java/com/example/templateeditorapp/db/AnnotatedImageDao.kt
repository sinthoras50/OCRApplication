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

    // notify the db that we want to use the addCallback method - useful for pre-population of the db with the default cheque
    @Query("SELECT * FROM annotated_images LIMIT 0")
    suspend fun onCreate(): AnnotatedImage?
}
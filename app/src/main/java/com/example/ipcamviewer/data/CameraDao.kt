package com.example.ipcamviewer.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CameraDao {
    @Query("SELECT * FROM cameras ORDER BY id ASC")
    fun observeAll(): LiveData<List<CameraEntity>>

    @Query("SELECT * FROM cameras ORDER BY id ASC")
    suspend fun getAll(): List<CameraEntity>

    @Query("SELECT * FROM cameras WHERE id = :id")
    suspend fun getById(id: Long): CameraEntity?

    @Insert
    suspend fun insert(camera: CameraEntity): Long

    @Update
    suspend fun update(camera: CameraEntity)

    @Delete
    suspend fun delete(camera: CameraEntity)

    @Query("SELECT COUNT(*) FROM cameras WHERE host = :host AND port = :port")
    suspend fun countByHostPort(host: String, port: Int): Int
}

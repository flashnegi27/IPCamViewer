package com.example.ipcamviewer.data

import androidx.lifecycle.LiveData

class CameraRepository(private val dao: CameraDao) {
    val cameras: LiveData<List<CameraEntity>> = dao.observeAll()

    suspend fun addCamera(camera: CameraEntity): Long = dao.insert(camera)
    suspend fun updateCamera(camera: CameraEntity) = dao.update(camera)
    suspend fun deleteCamera(camera: CameraEntity) = dao.delete(camera)
    suspend fun getCamera(id: Long): CameraEntity? = dao.getById(id)
    suspend fun exists(host: String, port: Int): Boolean = dao.countByHostPort(host, port) > 0
}

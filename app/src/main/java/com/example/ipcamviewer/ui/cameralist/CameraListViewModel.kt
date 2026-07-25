package com.example.ipcamviewer.ui.cameralist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.ipcamviewer.data.AppDatabase
import com.example.ipcamviewer.data.CameraEntity
import com.example.ipcamviewer.data.CameraRepository
import kotlinx.coroutines.launch

class CameraListViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CameraRepository(AppDatabase.getInstance(app).cameraDao())
    val cameras: LiveData<List<CameraEntity>> = repo.cameras

    fun delete(camera: CameraEntity) = viewModelScope.launch { repo.deleteCamera(camera) }
}

package com.example.ipcamviewer.ui.addcamera

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ipcamviewer.data.AppDatabase
import com.example.ipcamviewer.data.CameraEntity
import com.example.ipcamviewer.data.CameraRepository
import com.example.ipcamviewer.data.CameraType
import com.example.ipcamviewer.discovery.DiscoveredDevice
import com.example.ipcamviewer.discovery.OnvifClient
import com.example.ipcamviewer.discovery.WsDiscoveryClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class SaveResult {
    object Success : SaveResult()
    data class Error(val message: String) : SaveResult()
}

class AddCameraViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CameraRepository(AppDatabase.getInstance(app).cameraDao())

    val scanning = MutableLiveData(false)
    val discovered = MutableLiveData<List<DiscoveredDevice>>(emptyList())
    val saveResult = MutableLiveData<SaveResult?>()

    fun scanNetwork() {
        scanning.value = true
        viewModelScope.launch {
            val devices = WsDiscoveryClient.discover(getApplication())
            discovered.value = devices
            scanning.value = false
        }
    }

    fun saveManualCamera(
        name: String, host: String, port: Int, path: String,
        username: String, password: String, useTcp: Boolean
    ) {
        viewModelScope.launch {
            try {
                repo.addCamera(
                    CameraEntity(
                        name = name.ifBlank { host },
                        host = host, port = port, streamPath = path,
                        username = username, password = password,
                        type = CameraType.MANUAL,
                        transport = if (useTcp) "tcp" else "udp"
                    )
                )
                saveResult.value = SaveResult.Success
            } catch (e: Exception) {
                saveResult.value = SaveResult.Error(e.message ?: "Could not save camera")
            }
        }
    }

    fun addDiscoveredCamera(device: DiscoveredDevice, username: String, password: String) {
        viewModelScope.launch {
            try {
                data class Resolved(val host: String, val port: Int, val path: String)

                val resolved = withContext(Dispatchers.IO) {
                    val client = OnvifClient(username, password)
                    val uri = client.resolveStreamUri(device.xAddr)
                        ?: throw IllegalStateException("Camera did not return a stream URI. Check credentials.")
                    val regex = Regex("rtsp://(?:[^@]+@)?([^:/]+):?(\\d*)(/.*)?")
                    val match = regex.find(uri)
                        ?: throw IllegalStateException("Unrecognized stream URI: $uri")
                    Resolved(
                        host = match.groupValues[1],
                        port = match.groupValues[2].toIntOrNull() ?: 554,
                        path = match.groupValues[3].ifBlank { "/" }
                    )
                }

                repo.addCamera(
                    CameraEntity(
                        name = device.name.ifBlank { device.host },
                        host = resolved.host, port = resolved.port, streamPath = resolved.path,
                        username = username, password = password,
                        type = CameraType.ONVIF,
                        onvifXAddr = device.xAddr
                    )
                )
                saveResult.value = SaveResult.Success
            } catch (e: Exception) {
                saveResult.value = SaveResult.Error(e.message ?: "Could not add camera")
            }
        }
    }
}

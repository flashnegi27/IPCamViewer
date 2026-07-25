package com.example.ipcamviewer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cameras")
data class CameraEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 554,
    val streamPath: String = "",
    val username: String = "",
    val password: String = "",
    val type: CameraType = CameraType.MANUAL,
    val onvifXAddr: String? = null,
    val transport: String = "tcp" // rtsp transport: tcp or udp
) {
    /** Builds the full RTSP URL used for playback. */
    fun buildRtspUrl(): String {
        val auth = if (username.isNotBlank()) "$username:$password@" else ""
        val path = if (streamPath.startsWith("/")) streamPath else "/$streamPath"
        return "rtsp://$auth$host:$port$path"
    }
}

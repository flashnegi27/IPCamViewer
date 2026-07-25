package com.example.ipcamviewer.discovery

data class DiscoveredDevice(
    val xAddr: String,       // device service URL, e.g. http://192.168.1.50/onvif/device_service
    val host: String,
    val types: List<String> = emptyList(),
    var name: String = ""
)

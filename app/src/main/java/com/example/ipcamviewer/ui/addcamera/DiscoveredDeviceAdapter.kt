package com.example.ipcamviewer.ui.addcamera

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ipcamviewer.databinding.ItemDiscoveredDeviceBinding
import com.example.ipcamviewer.discovery.DiscoveredDevice

class DiscoveredDeviceAdapter(
    private val onAdd: (DiscoveredDevice) -> Unit
) : ListAdapter<DiscoveredDevice, DiscoveredDeviceAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemDiscoveredDeviceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDiscoveredDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val device = getItem(position)
        holder.binding.textDeviceName.text = device.name
        holder.binding.textDeviceHost.text = device.host
        holder.binding.buttonAddDevice.setOnClickListener { onAdd(device) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<DiscoveredDevice>() {
            override fun areItemsTheSame(a: DiscoveredDevice, b: DiscoveredDevice) = a.xAddr == b.xAddr
            override fun areContentsTheSame(a: DiscoveredDevice, b: DiscoveredDevice) = a == b
        }
    }
}

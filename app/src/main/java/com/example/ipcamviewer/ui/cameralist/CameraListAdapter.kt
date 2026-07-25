package com.example.ipcamviewer.ui.cameralist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ipcamviewer.data.CameraEntity
import com.example.ipcamviewer.databinding.ItemCameraBinding

class CameraListAdapter(
    private val onClick: (CameraEntity) -> Unit,
    private val onLongClick: (CameraEntity) -> Unit
) : ListAdapter<CameraEntity, CameraListAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemCameraBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCameraBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val camera = getItem(position)
        holder.binding.textCameraName.text = camera.name
        holder.binding.textCameraAddress.text = "${camera.host}:${camera.port}"
        holder.binding.root.setOnClickListener { onClick(camera) }
        holder.binding.root.setOnLongClickListener {
            onLongClick(camera)
            true
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CameraEntity>() {
            override fun areItemsTheSame(a: CameraEntity, b: CameraEntity) = a.id == b.id
            override fun areContentsTheSame(a: CameraEntity, b: CameraEntity) = a == b
        }
    }
}

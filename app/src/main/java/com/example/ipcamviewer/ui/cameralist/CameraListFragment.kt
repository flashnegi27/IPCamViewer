package com.example.ipcamviewer.ui.cameralist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ipcamviewer.R
import com.example.ipcamviewer.data.CameraEntity
import com.example.ipcamviewer.databinding.FragmentCameraListBinding
import com.example.ipcamviewer.ui.liveview.LiveViewActivity

class CameraListFragment : Fragment() {
    private var _binding: FragmentCameraListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CameraListViewModel by viewModels()
    private lateinit var adapter: CameraListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CameraListAdapter(
            onClick = { camera -> openLiveView(camera) },
            onLongClick = { camera -> showDeleteDialog(camera) }
        )
        binding.recyclerCameras.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerCameras.adapter = adapter

        viewModel.cameras.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAddCamera.setOnClickListener {
            findNavController().navigate(R.id.action_cameraListFragment_to_addCameraFragment)
        }
    }

    private fun openLiveView(camera: CameraEntity) {
        val intent = Intent(requireContext(), LiveViewActivity::class.java)
        intent.putExtra(LiveViewActivity.EXTRA_CAMERA_ID, camera.id)
        startActivity(intent)
    }

    private fun showDeleteDialog(camera: CameraEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(camera.name)
            .setItems(arrayOf("Remove camera")) { _, _ -> viewModel.delete(camera) }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

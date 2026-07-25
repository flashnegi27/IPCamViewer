package com.example.ipcamviewer.ui.addcamera

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ipcamviewer.databinding.DialogCredentialsBinding
import com.example.ipcamviewer.databinding.FragmentDiscoverBinding
import com.example.ipcamviewer.discovery.DiscoveredDevice

class DiscoverFragment : Fragment() {
    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddCameraViewModel by activityViewModels()
    private lateinit var adapter: DiscoveredDeviceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DiscoveredDeviceAdapter { device -> promptCredentials(device) }
        binding.recyclerDiscovered.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerDiscovered.adapter = adapter

        binding.buttonScan.setOnClickListener { viewModel.scanNetwork() }

        viewModel.scanning.observe(viewLifecycleOwner) { scanning ->
            binding.progressScan.visibility = if (scanning) View.VISIBLE else View.GONE
            binding.buttonScan.isEnabled = !scanning
            binding.buttonScan.text = if (scanning) "Scanning…" else "Scan Network"
            binding.textNoDevices.visibility =
                if (!scanning && adapter.itemCount == 0) View.VISIBLE else View.GONE
        }
        viewModel.discovered.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.textNoDevices.visibility =
                if (list.isEmpty() && viewModel.scanning.value == false) View.VISIBLE else View.GONE
        }
        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is SaveResult.Success -> {
                    Toast.makeText(requireContext(), "Camera added", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is SaveResult.Error -> Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                null -> {}
            }
        }

        // Kick off a scan automatically when the tab opens.
        if (viewModel.discovered.value.isNullOrEmpty()) viewModel.scanNetwork()
    }

    private fun promptCredentials(device: DiscoveredDevice) {
        val dialogBinding = DialogCredentialsBinding.inflate(LayoutInflater.from(requireContext()))
        AlertDialog.Builder(requireContext())
            .setTitle("Add ${device.name}")
            .setMessage(device.host)
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                viewModel.addDiscoveredCamera(
                    device,
                    dialogBinding.editUsername.text.toString().trim(),
                    dialogBinding.editPassword.text.toString()
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

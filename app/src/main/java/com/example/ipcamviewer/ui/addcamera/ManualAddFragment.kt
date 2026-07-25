package com.example.ipcamviewer.ui.addcamera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.ipcamviewer.databinding.FragmentManualAddBinding

class ManualAddFragment : Fragment() {
    private var _binding: FragmentManualAddBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddCameraViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManualAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.editPort.setText("554")
        binding.editPath.setText("/")

        binding.buttonSave.setOnClickListener {
            val host = binding.editHost.text.toString().trim()
            if (host.isEmpty()) {
                binding.editHost.error = "Required"
                return@setOnClickListener
            }
            viewModel.saveManualCamera(
                name = binding.editName.text.toString().trim(),
                host = host,
                port = binding.editPort.text.toString().toIntOrNull() ?: 554,
                path = binding.editPath.text.toString().trim().ifBlank { "/" },
                username = binding.editUsername.text.toString().trim(),
                password = binding.editPassword.text.toString(),
                useTcp = binding.radioTcp.isChecked
            )
        }

        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is SaveResult.Success -> findNavController().popBackStack()
                is SaveResult.Error -> Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                null -> {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

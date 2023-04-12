package com.example.templateeditorapp.ui.qrgen

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.templateeditorapp.R
import com.example.templateeditorapp.databinding.FragmentQrGeneratorBinding
import com.example.templateeditorapp.utils.OCR_MAP_KEY

class QrGeneratorFragment : Fragment() {

    companion object {
        fun newInstance() = QrGeneratorFragment()
    }

    private lateinit var viewModel: QrGeneratorViewModel
    private lateinit var binding: FragmentQrGeneratorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(QrGeneratorViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentQrGeneratorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // application logic

        val resultMap = arguments?.getSerializable(OCR_MAP_KEY) as? HashMap<String, String> ?: return

        viewModel.generateQrCode(resultMap, requireContext())

        viewModel.bitmap.observe(viewLifecycleOwner) { bitmap ->
            binding.qrCodeImageView.setImageBitmap(bitmap)
        }

    }

}
package com.example.templateeditorapp.ui.tesseract

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.fragment.findNavController
import com.example.templateeditorapp.OcrApp
import com.example.templateeditorapp.R
import com.example.templateeditorapp.databinding.FragmentTesseractBinding
import com.example.templateeditorapp.db.ImageDatabase
import com.example.templateeditorapp.utils.*
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.*
import java.lang.IllegalArgumentException

class TesseractFragment : Fragment() {

    companion object {
        fun newInstance() = TesseractFragment()
    }

    private val db: ImageDatabase by lazy {
        (requireActivity().application as OcrApp).db
    }

    private lateinit var binding: FragmentTesseractBinding
    private lateinit var viewModel: TesseractViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                if (modelClass.isAssignableFrom(TesseractViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return TesseractViewModel(db) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }).get(TesseractViewModel::class.java)

        Assets.extractAssets(requireContext())

        if (!viewModel.isInitialized()) {
            val dataPath = Assets.getTessDataPath(requireContext())
            val language = Assets.language
            viewModel.initTesseract(dataPath, language, TessBaseAPI.OEM_LSTM_ONLY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentTesseractBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.resultMap.observe(viewLifecycleOwner) { map ->
            updateUi(map)
        }

        viewModel.progress.observe(viewLifecycleOwner) { progress ->
            Log.d(TAG_IMAGE, "progress = $progress")
            binding.transactionProgress.setProgress(progress, true)
        }

        binding.btnConfirmForm.setOnClickListener {

            val args = Bundle()
            args.putSerializable(OCR_MAP_KEY,
                viewModel.resultMap.value?.let { map -> HashMap<String, String>(map) })

            findNavController().navigate(R.id.action_tesseractFragment_to_qrGeneratorFragment, args)
        }

        val imagePath = arguments?.getString(TEMP_PHOTO_KEY)
        val templatePath = arguments?.getString(TEMPLATE_KEY)
        val scalingFactor = arguments?.getDouble("scalingFactor")

        Log.d(TAG_IMAGE, "fp = $imagePath tp = $templatePath factor = $scalingFactor")
        viewModel.recognizeImage(imagePath!!, templatePath!!, scalingFactor!!, requireContext())

    }

    private fun updateUi(map: MutableMap<String, String>) {
        for (key in map.keys) {
            when(key) {
                "amount" -> {
                    binding.amountEditText.setText(map[key])
                }
                "variable symbol" -> {
                    binding.variableSymbolEditText.setText(map[key])
                }
                "constant symbol" -> {
                    binding.constantSymbolEditText.setText(map[key])
                }
                "specific symbol" -> {
                    binding.specificSymbolEditText.setText(map[key])
                }
                "note" -> {
                    binding.noteEditText.setText(map[key])
                }
                "iban" -> {
                    binding.ibanEditText.setText(map[key])
                }
                "swift" -> {}
                "beneficiary name" -> {
                    binding.recipientNameEditText.setText(map[key])
                }
                "beneficiary address 1" -> {}
                "beneficiary address 2" -> {}
            }
        }
    }



}
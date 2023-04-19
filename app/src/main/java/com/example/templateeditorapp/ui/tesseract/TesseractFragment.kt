package com.example.templateeditorapp.ui.tesseract

import android.app.Activity
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.toRect
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.fragment.findNavController
import com.example.templateeditorapp.OcrApp
import com.example.templateeditorapp.R
import com.example.templateeditorapp.databinding.FragmentTesseractBinding
import com.example.templateeditorapp.db.ImageDatabase
import com.example.templateeditorapp.ui.qrgen.Currency
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
            viewModel.initTesseract(dataPath, language, TessBaseAPI.OEM_TESSERACT_LSTM_COMBINED)
        }

        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.isProcessing()) {
                    viewModel.stop()
                }

                lifecycleScope.launch {
                    delay(100)
                    findNavController().popBackStack()
                }
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

//        binding = DataBindingUtil.setContentView(requireActivity(), R.layout.fragment_tesseract)
        binding = FragmentTesseractBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeCurrencyDropdown()

        viewModel.progress.observe(viewLifecycleOwner) { progress ->
            Log.d(TAG_IMAGE, "progress = $progress")
            binding.transactionProgress.setProgress(progress, true)
        }

        binding.btnConfirmForm.setOnClickListener {

            val args = Bundle()
            val currency = binding.amountCurrencySpinner.selectedItem.toString()
            args.putSerializable(OCR_MAP_KEY, viewModel.prepareResultMap(currency))

            findNavController().navigate(R.id.action_tesseractFragment_to_qrGeneratorFragment, args)
        }

        val imagePath = arguments?.getString(TEMP_PHOTO_KEY)
        val templatePath = arguments?.getString(TEMPLATE_KEY)
        val scalingFactor = arguments?.getDouble("scalingFactor")
        val cropRect = arguments?.getParcelable(CROP_RECT_KEY) as? RectF?

        Log.d(TAG_IMAGE, "fp = $imagePath tp = $templatePath factor = $scalingFactor")
        viewModel.recognizeImage(imagePath!!, templatePath!!, scalingFactor!!, requireContext(), cropRect?.toRect())

        binding.btnDebugForm.setOnClickListener {
            viewModel.recognizeImage(imagePath!!, templatePath!!, scalingFactor!!, requireContext(), cropRect?.toRect())
        }

    }

    private fun initializeCurrencyDropdown() {
        val spinner = binding.amountCurrencySpinner
//        val items = listOf(*resources.getStringArray(R.array.formFieldsCurrency))
        val items = Currency.values().toList()
        val adapter = CurrencySpinnerAdapter(requireContext(), items)
//        adapter.setDropDownViewResource(R.layout.currency_spinner_item)
        spinner.adapter = adapter

        spinner.setOnTouchListener { view, _ ->
            val inputManager = requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(view.windowToken, 0)
            view.performClick()
            true
        }
    }
}
package com.example.templateeditorapp.ui.overview

import android.os.Bundle
import android.util.DisplayMetrics
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
import com.example.templateeditorapp.databinding.FragmentOverviewBinding
import com.example.templateeditorapp.db.ImageDatabase
import com.example.templateeditorapp.utils.OVERVIEW_KEY
import com.example.templateeditorapp.utils.TAG_IMAGE
import com.example.templateeditorapp.utils.TEMPLATE_KEY


class OverviewFragment : Fragment() {

    companion object {
        fun newInstance() = OverviewFragment()
    }

    private val db: ImageDatabase by lazy {
        (requireActivity().application as OcrApp).db
    }

    private lateinit var viewModel: OverviewViewModel
    private lateinit var binding: FragmentOverviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                if (modelClass.isAssignableFrom(OverviewViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return OverviewViewModel(db) as T
                }
                throw java.lang.IllegalArgumentException("Unknown ViewModel class")
            }
        }).get(OverviewViewModel::class.java)

        val files = requireContext().fileList()
        Log.d(TAG_IMAGE, "dir contents = ${files.joinToString(separator = ", ")}")

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.currentImage.observe(viewLifecycleOwner) { bitmap ->
            binding.overviewImageView.setImageBitmap(bitmap)
        }

        viewModel.currentImageName.observe(viewLifecycleOwner) { name ->
            binding.templateNameTextView.text = name
        }

        val reqWidth = resources.displayMetrics.widthPixels
        val reqHeight = resources.displayMetrics.heightPixels

        Log.d(TAG_IMAGE, "photoview w = $reqWidth h = $reqHeight")

        val args = arguments
        val currentImage: String? = args?.getString(OVERVIEW_KEY)


        if (currentImage != null) {
            viewModel.loadImages(requireContext(), currentImage, reqWidth, reqHeight)
        } else {
            viewModel.loadImages(requireContext(), reqWidth, reqHeight)
        }

        binding.btnPreviousTemplate.setOnClickListener {
            viewModel.loadPreviousPhoto(requireContext(), reqWidth, reqHeight)
        }

        binding.btnNextTemplate.setOnClickListener {
            viewModel.loadNextPhoto(requireContext(), reqWidth, reqHeight)
        }

        binding.btnDeleteTemplate.setOnClickListener {
            viewModel.deleteCurrentTemplate(requireContext(), reqWidth, reqHeight)
        }

        binding.btnCreateTemplate.setOnClickListener {
            findNavController().navigate(R.id.action_overviewFragment_to_templateSelectionFragment)
        }

        binding.btnEditTemplate.setOnClickListener {
            val args = Bundle()
            if (viewModel.currentImageName.value.isNullOrEmpty().not()) {
                args.putString(TEMPLATE_KEY, viewModel.currentImageName.value)
            }
            findNavController().navigate(R.id.action_overviewFragment_to_editorFragment, args)
        }

        binding.btnConfirmSelection.setOnClickListener {
            val args = Bundle()
            if (viewModel.currentImageName.value.isNullOrEmpty().not()) {
                args.putString(TEMPLATE_KEY, viewModel.currentImageName.value)
            }
            findNavController().navigate(R.id.action_overviewFragment_to_cameraFragment, args)
//            findNavController().navigate(R.id.action_overviewFragment_to_cvCameraFragment, args)
        }

    }

}
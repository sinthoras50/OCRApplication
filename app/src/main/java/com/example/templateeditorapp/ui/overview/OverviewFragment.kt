package com.example.templateeditorapp.ui.overview

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.text.method.MovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.fragment.findNavController
import com.example.templateeditorapp.OcrApp
import com.example.templateeditorapp.R
import com.example.templateeditorapp.databinding.FragmentOverviewBinding
import com.example.templateeditorapp.db.ImageDatabase
import com.example.templateeditorapp.ui.editor.DEBUG
import com.example.templateeditorapp.utils.CROP_RECT_KEY
import com.example.templateeditorapp.utils.OVERVIEW_KEY
import com.example.templateeditorapp.utils.TAG_IMAGE
import com.example.templateeditorapp.utils.TEMPLATE_KEY
import com.google.android.material.button.MaterialButton


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
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val images = mutableListOf<Bitmap>()

        val adapter = ViewPagerAdapter(images)
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false

//        viewModel.currentImage.observe(viewLifecycleOwner) { bitmap ->
//            binding.overviewImageView.setImageBitmap(bitmap)
//            adapter.notifyDataSetChanged()
//        } )

        viewModel.imageSet.observe(viewLifecycleOwner) { imageSet ->
            images.clear()
            images.addAll(imageSet)
            adapter.notifyDataSetChanged()
            binding.overviewLoadingPanel.visibility = View.GONE
            binding.viewPager.setCurrentItem(viewModel.currentIdx, false)
        }

//        viewModel.currentImageName.observe(viewLifecycleOwner) { name ->
//            binding.templateNameTextView.text = name
//        }

        val reqWidth = resources.displayMetrics.widthPixels
        val reqHeight = resources.displayMetrics.heightPixels

        Log.d(TAG_IMAGE, "photoview w = $reqWidth h = $reqHeight")

        val args = arguments
        val currentImage: String? = args?.getString(OVERVIEW_KEY)


        if (currentImage != null) {
//            viewModel.upsertImage(currentImage, requireContext(), reqWidth, reqHeight)
//            viewModel.loadImages(requireContext(), reqWidth, reqHeight)
            viewModel.loadImages(currentImage, requireContext(), reqWidth, reqHeight)
            binding.viewPager.doOnPreDraw {
                binding.viewPager.setCurrentItem(viewModel.currentIdx, false)
            }
            Log.d(TAG_IMAGE, "currentTemplate != null currIdx = ${viewModel.currentIdx}")
        } else {
            viewModel.loadImages(requireContext(), reqWidth, reqHeight)
            binding.viewPager.doOnPreDraw {
                binding.viewPager.setCurrentItem(viewModel.currentIdx, false)
            }
            Log.d(TAG_IMAGE, "currentTemplate == null currIdx = ${viewModel.currentIdx}")
        }

        binding.btnPreviousTemplate.setOnClickListener {
            if (viewModel.loadPreviousPhoto()) {
                binding.viewPager.setCurrentItem(viewModel.currentIdx, true)
                Log.d(TAG_IMAGE, "current index = ${viewModel.currentIdx}")
            }

//            viewModel.loadPreviousPhoto(requireContext(), reqWidth, reqHeight)

        }


        binding.btnNextTemplate.setOnClickListener {
            if (viewModel.loadNextPhoto()) {
                binding.viewPager.setCurrentItem(viewModel.currentIdx, true)
                Log.d(TAG_IMAGE, "current index = ${viewModel.currentIdx}")
            }

//            viewModel.loadNextPhoto(requireContext(), reqWidth, reqHeight)
//            viewModel.loadNextPhoto()
        }

        binding.btnDeleteTemplate.setOnClickListener {

            Log.d(DEBUG, "opening dialog")

            val builder = AlertDialog.Builder(requireContext())
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.delete_template_dialog, null)
            builder.setView(dialogView)
            builder.setCancelable(true)
            val dialog = builder.create()
            val background = ColorDrawable(Color.TRANSPARENT)
            val marginBg = InsetDrawable(background, 100)
            dialog.window?.setBackgroundDrawable(marginBg)

            dialogView.findViewById<MaterialButton>(R.id.btnDeleteTemplate).setOnClickListener {

                if (viewModel.deleteCurrentTemplate(requireContext())) {
                    binding.viewPager.setCurrentItem(viewModel.currentIdx, false)
                    Log.d(TAG_IMAGE, "current index = ${viewModel.currentIdx}")
                    Toast.makeText(requireContext(), "Template deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Could not delete the template", Toast.LENGTH_SHORT).show()
                }

                dialog.cancel()
            }

            dialogView.findViewById<MaterialButton>(R.id.btnCancelDeleteTemplate).setOnClickListener {
                dialog.cancel()
            }

            dialog.show()


        }

        binding.btnCreateTemplate.setOnClickListener {
            findNavController().navigate(R.id.action_overviewFragment_to_templateSelectionFragment)
            Log.d(TAG_IMAGE, "current index = ${viewModel.currentIdx}")
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
                args.putParcelable(CROP_RECT_KEY, viewModel.currentImageBoundingBox)
            }
            findNavController().navigate(R.id.action_overviewFragment_to_cameraFragment, args)
        }

    }

}
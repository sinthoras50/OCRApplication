package com.example.templateeditorapp.ui.overview

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.templateeditorapp.R
import com.example.templateeditorapp.databinding.FragmentTemplateSelectionBinding
import com.example.templateeditorapp.ui.editor.ImageData
import com.example.templateeditorapp.ui.editor.PICK_IMAGE
import com.example.templateeditorapp.utils.ImageUtils
import com.example.templateeditorapp.utils.TAG_IMAGE


class TemplateSelectionFragment : Fragment() {

    companion object {
        fun newInstance() = TemplateSelectionFragment()
    }

    private lateinit var binding: FragmentTemplateSelectionBinding
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                pickImage()
            } else {
                // handle not granted
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTemplateSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.clickableGalleryLayout.setOnClickListener {
            pickImage()
        }

        binding.clickableCameraLayout.setOnClickListener {
            findNavController().navigate(R.id.action_templateSelectionFragment_to_templateCameraFragment)
        }

    }

    fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val ctx = requireContext()

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data
            val imageStream = imageUri?.let { ctx.contentResolver.openInputStream(it) }
            val selectedBitmap = BitmapFactory.decodeStream(imageStream)
            val result = ImageUtils.savePhoto(TEMP_PHOTO_PATH, selectedBitmap, ctx)
            Log.d(TAG_IMAGE, "result = $result")
            val args = Bundle()
            args.putString(TEMP_PHOTO_KEY, TEMP_PHOTO_PATH)

            findNavController().navigate(R.id.action_templateSelectionFragment_to_editorFragment, args)

        }
    }


}
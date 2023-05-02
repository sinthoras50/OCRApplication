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
import com.example.templateeditorapp.ui.editor.EditorFragment
import com.example.templateeditorapp.ui.editor.ImageData
import com.example.templateeditorapp.ui.editor.PICK_IMAGE
import com.example.templateeditorapp.utils.ImageUtils
import com.example.templateeditorapp.utils.TAG_IMAGE
import com.example.templateeditorapp.utils.TEMP_PHOTO_KEY
import com.example.templateeditorapp.utils.TEMP_PHOTO_PATH

/**
 * A fragment to allow the user to select an image for template creation in [EditorFragment]
 */
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

    /**
     * Initializes view listeners within this [Fragment]
     * @param view
     * @param savedInstanceState
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.clickableGalleryLayout.setOnClickListener {
            pickImage()
        }

        binding.clickableCameraLayout.setOnClickListener {
            findNavController().navigate(R.id.action_templateSelectionFragment_to_templateCameraFragment)
        }

    }

    /**
     * Start an image picker activity to allow the user to select an image
     */
    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE)
    }

    /**
     * Handles the result of the image picker activity. If the user has selected an image,
     * it is decoded into a bitmap and saved to a temporary file. Then, the user is navigated
     * to the editor fragment with the path to the temporary file passed as an argument.
     * @param requestCode The code that was used to start the activity.
     * @param resultCode The result code returned by the activity.
     * @param data The result data returned by the activity.
     */
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
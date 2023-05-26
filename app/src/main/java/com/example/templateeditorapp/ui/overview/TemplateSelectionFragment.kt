package com.example.templateeditorapp.ui.overview

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
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
import java.io.File
import java.net.URI


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
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf"))
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
            val uri = data?.data
            val mimeType = uri?.let { ctx.contentResolver.getType(it) }
            val args = Bundle()
            Log.d("IMAGE", "data type = $mimeType")

            if (mimeType == "application/pdf") {
                val fileDescriptor = uri.let { ctx.contentResolver.openFileDescriptor(it, "r")}
                val doc = PdfRenderer(fileDescriptor!!)

                val page = doc.openPage(0)
                val width = page.width * 2
                val height = page.height * 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                Log.d("PDF", "page count = ${doc.pageCount}")
                page.close()
                doc.close()

                ImageUtils.savePhoto(TEMP_PHOTO_PATH, bitmap, ctx)
            } else if (mimeType?.startsWith("image/") == true) {
                val imageStream = uri.let { ctx.contentResolver.openInputStream(it) }
                val selectedBitmap = BitmapFactory.decodeStream(imageStream)

                ImageUtils.savePhoto(TEMP_PHOTO_PATH, selectedBitmap, ctx)
            }

            args.putString(TEMP_PHOTO_KEY, TEMP_PHOTO_PATH)


            findNavController().navigate(R.id.action_templateSelectionFragment_to_editorFragment, args)

        }
    }


}
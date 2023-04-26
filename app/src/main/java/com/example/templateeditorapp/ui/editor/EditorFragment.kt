package com.example.templateeditorapp.ui.editor

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import com.example.templateeditorapp.OcrApp
import com.example.templateeditorapp.R
import com.example.templateeditorapp.databinding.FragmentEditorBinding
import com.example.templateeditorapp.db.ImageDatabase
import com.example.templateeditorapp.utils.OVERVIEW_KEY
import com.example.templateeditorapp.utils.TAG_IMAGE
import com.example.templateeditorapp.utils.TEMPLATE_KEY
import com.example.templateeditorapp.utils.TEMP_PHOTO_KEY
import com.google.android.material.button.MaterialButton

const val PICK_IMAGE = 1
const val DEBUG = "DEBUG"

/**
 * A [Fragment] subclass that helps the user create new Cheque/Invoice templates.
 */
class EditorFragment : Fragment() {

    private val minImageScale = 1.0f
    private val mediumImageScale = 5.0f
    private val maxImageScale = 10.0f

    companion object {
        fun newInstance() = EditorFragment()
    }

    private val db: ImageDatabase by lazy {
        (requireActivity().application as OcrApp).db
    }

    private lateinit var viewModel: EditorViewModel
    private lateinit var binding: FragmentEditorBinding
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<String>


    /**
     * Called when the activity is starting or restarting.
     * Initializes the [viewModel] by creating a [ViewModelProvider] and passing a [ViewModelProvider.Factory]
     * that creates an instance of [EditorViewModel] class using the [db] database.
     * Registers an activity result callback to request permission to access the device's image gallery.
     * @param savedInstanceState a Bundle object containing previously saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(EditorViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return EditorViewModel(db) as T
                }
                throw java.lang.IllegalArgumentException("Unknown ViewModel class")
            }
        }).get(EditorViewModel::class.java)

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
        binding = FragmentEditorBinding.inflate(inflater, container, false)
        return binding.root
    }


    /**
     * Called when the view hierarchy associated with this fragment is created.
     * Sets up the image attacher, initializes the dropdown, and loads a template if it exists.
     * If an image result from the gallery exists, the result is handled, and the spinner items are updated.
     * Observes the [EditorViewModel.imageData] and updates the [MyPhotoView] accordingly.
     *
     * @param view The root view of the fragment.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val attacher = binding.loadedImage.attacher
        val spinner = binding.spinnerFormFields

        binding.loadedImage.maximumScale = maxImageScale
        binding.loadedImage.mediumScale = mediumImageScale
        binding.loadedImage.minimumScale = minImageScale

        initializeDropdown()

        val currentTemplate = arguments?.getString(TEMPLATE_KEY)

        if (currentTemplate != null) {
            viewModel.loadTemplate(currentTemplate, requireContext(), spinner)
        }

        val galleryResult = arguments?.getString(TEMP_PHOTO_KEY)

        if (galleryResult != null) {
            viewModel.handleImageResult(galleryResult, requireContext())
            viewModel.updateSpinnerItems(binding.spinnerFormFields)
        }

        viewModel.imageData.observe(viewLifecycleOwner) { imageData ->
            binding.loadedImage.setImageBitmap(imageData.bitmap)
            binding.loadedImage.attacher.setDisplayMatrix(imageData.imageMatrix)
            binding.radioBtnMove.isEnabled = true
            binding.radioBtnEdit.isEnabled = true
            binding.radioBtnSelect.isEnabled = true

            if (imageData.isUpdate.not()) {
                Handler().postDelayed({
                    val matrix = binding.loadedImage.attacher.imageMatrix
                    val values = FloatArray(9)
                    matrix.getValues(values)
                    val scale = values[Matrix.MSCALE_X]
                    viewModel.scaleMult = 1f / scale
                }, 50)
            }

        }



        binding.btnLoadImage.setOnClickListener {

            if (checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                pickImage()
            } else {
                requestPermissionsLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        binding.editorRadioGroup.setOnCheckedChangeListener { _, i ->
            when(i) {
                binding.radioBtnEdit.id -> {
                    binding.loadedImage.setOnTouchListener { view, event ->
                        viewModel.onTouchListenerEdit(view, event, spinner)
                    }
                }
                binding.radioBtnMove.id -> {
                    binding.loadedImage.setOnTouchListener { view, event ->
                        attacher.onTouch(view, event)
                    }
                }
                binding.radioBtnSelect.id -> {
                    binding.loadedImage.setOnTouchListener {view, event ->
                        viewModel.onTouchListenerSelect(view, event)
                    }
                }
            }
        }

        binding.radioBtnSelect.setOnLongClickListener {
            Log.d(TAG_IMAGE, "long click")
            if (binding.radioBtnSelect.isChecked) {
                Log.d(TAG_IMAGE, "is selected")
                viewModel.clearSelectionRect(binding.loadedImage)
            }
            true
        }

        binding.btnClearBoundingBox.setOnClickListener {
            val matrix = Matrix()
            binding.loadedImage.getSuppMatrix(matrix)
            viewModel.removeLastBoundingBox(spinner, matrix)
        }

        binding.btnRotate90DegRight.setOnClickListener {
            viewModel.rotateRight90Deg(spinner)
        }

        binding.btnConfirmImage.setOnClickListener {
            Log.d(DEBUG, "opening dialog")

            val builder = AlertDialog.Builder(requireContext())
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.save_template_dialog, null)
            builder.setView(dialogView)
            builder.setCancelable(true)
            val dialog = builder.create()
            val background = ColorDrawable(Color.TRANSPARENT)
            val marginBg = InsetDrawable(background, 100)
            dialog.window?.setBackgroundDrawable(marginBg)

            val editText = dialogView.findViewById<AppCompatEditText>(R.id.templateNameEditText)
            editText.setText(currentTemplate ?: "")
            Log.d(TAG_IMAGE, "Current template = $currentTemplate")

            dialogView.findViewById<MaterialButton>(R.id.btnSaveTemplate).setOnClickListener {

                val filename = editText.text.toString()

                if (filename.isNotBlank()) {
                    viewModel.saveTemplate(filename, requireContext())
                    dialog.dismiss()

                    val args = Bundle()
                    args.putString(OVERVIEW_KEY, filename)

                    Toast.makeText(requireContext(), "Template saved", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_editorFragment_to_overviewFragment, args)
                }
            }

            dialogView.findViewById<MaterialButton>(R.id.btnCancelSaveTemplate).setOnClickListener {
                dialog.cancel()
            }

            dialog.show()
        }

    }

    /**
     * Initializes the [EditorSpinnerAdapter] dropdown menu.
     */
    private fun initializeDropdown() {
        val spinner = binding.spinnerFormFields
        val items = listOf(*resources.getStringArray(R.array.formFields))
        Log.d(DEBUG, "items = ${items.joinToString(separator=",")}")
        val adapter = EditorSpinnerAdapter(requireContext(), R.layout.editor_spinner_item, items)
        adapter.setDropDownViewResource(R.layout.editor_spinner_item)
        spinner.adapter = adapter
    }


    /**
     * Starts an intent to pick an image from the gallery.
     */
    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE)
    }


    /**
     * Handles the result of the activity started by startActivityForResult().
     * If the request code is PICK_IMAGE and the result code is RESULT_OK, calls the
     * [EditorViewModel.handleImageResult] function to handle the image result and update the spinner items.
     * @param requestCode The request code passed to startActivityForResult().
     * @param resultCode The result code returned from the activity.
     * @param data The intent returned from the activity.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            viewModel.handleImageResult(data, requireContext().contentResolver)
            viewModel.updateSpinnerItems(binding.spinnerFormFields)
        }
    }

}
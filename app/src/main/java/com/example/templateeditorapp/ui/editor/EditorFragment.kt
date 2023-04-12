package com.example.templateeditorapp.ui.editor

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import carbon.widget.Button
import carbon.widget.EditText
import com.example.templateeditorapp.OcrApp
import com.example.templateeditorapp.R
import com.example.templateeditorapp.databinding.FragmentEditorBinding
import com.example.templateeditorapp.db.ImageDatabase
import com.example.templateeditorapp.utils.OVERVIEW_KEY
import com.example.templateeditorapp.utils.TAG_IMAGE
import com.example.templateeditorapp.utils.TEMPLATE_KEY
import com.example.templateeditorapp.utils.TEMP_PHOTO_KEY

const val PICK_IMAGE = 1
const val DEBUG = "DEBUG"

class EditorFragment : Fragment() {

    companion object {
        fun newInstance() = EditorFragment()
    }

    private val db: ImageDatabase by lazy {
        (requireActivity().application as OcrApp).db
    }

    private lateinit var viewModel: EditorViewModel
    private lateinit var binding: FragmentEditorBinding
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<String>

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

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val attacher = binding.loadedImage.attacher
        val spinner = binding.spinnerFormFields

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

            dialogView.findViewById<Button>(R.id.btnSaveTemplate).setOnClickListener {
                val filename = dialogView.findViewById<EditText>(R.id.templateNameEditText).text.toString()

                if (filename.isNotBlank()) {
                    viewModel.saveTemplate(filename, requireContext())
                    dialog.dismiss()

                    val args = Bundle()
                    args.putString(OVERVIEW_KEY, filename)

                    findNavController().navigate(R.id.action_editorFragment_to_overviewFragment, args)
                }
            }

            dialog.show()
        }

    }

    private fun initializeDropdown() {
        val spinner = binding.spinnerFormFields
        val items = listOf(*resources.getStringArray(R.array.formFields))
        Log.d(DEBUG, "items = ${items.joinToString(separator=",")}")
        val adapter = MySpinnerAdapter(requireContext(), R.layout.my_spinner_item, items)
        adapter.setDropDownViewResource(R.layout.my_spinner_item)
        spinner.adapter = adapter
    }


    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            viewModel.handleImageResult(data, requireContext().contentResolver)
            viewModel.updateSpinnerItems(binding.spinnerFormFields)
        }
    }

}
package com.example.templateeditorapp.ui.qrgen

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.templateeditorapp.R
import com.example.templateeditorapp.databinding.FragmentQrGeneratorBinding
import com.example.templateeditorapp.utils.OCR_MAP_KEY
import com.example.templateeditorapp.utils.TAG_IMAGE
import java.io.File
import java.io.FileOutputStream

class QrGeneratorFragment : Fragment() {

    companion object {
        fun newInstance() = QrGeneratorFragment()
    }

    private lateinit var viewModel: QrGeneratorViewModel
    private lateinit var binding: FragmentQrGeneratorBinding
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(QrGeneratorViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentQrGeneratorBinding.inflate(inflater, container, false)

        requestPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                if (viewModel.saveBitmap(requireContext())) {
                    Toast.makeText(requireContext(), "QR code saved to media directory.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "QR code could not be saved.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Cannot save QR code without write permission.", Toast.LENGTH_SHORT).show()
            }
        }


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        // application logic

        binding.qrCodeDescriptionTextView.movementMethod = LinkMovementMethod.getInstance()

        val resultMap = arguments?.getSerializable(OCR_MAP_KEY) as? HashMap<String, String> ?: return

        viewModel.generateQrCode(resultMap, requireContext())

        viewModel.paymentLink.observe(viewLifecycleOwner) { url ->
            val link = "<a href='$url'>payme link</a>"
            val description = getString(R.string.qr_code_description, link)
            val htmlDescription = Html.fromHtml(description, Html.FROM_HTML_MODE_COMPACT)
            binding.qrCodeDescriptionTextView.text = htmlDescription
        }

        viewModel.bitmap.observe(viewLifecycleOwner) { bitmap ->
            binding.qrCodeImageView.setImageBitmap(bitmap)
        }

        binding.btnSaveQr.setOnClickListener {
            saveQrCodeToExternalStorage()
        }

        binding.btnShareQr.setOnClickListener {
            val context = requireContext()
            val imagePath = File(context.filesDir, "external_files")
            imagePath.mkdir()
            val imageFile = File(imagePath.path, "temp_qr.jpg")

            val fos = FileOutputStream(imageFile)
            viewModel.bitmap.value!!.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            fos.flush()
            fos.close()

            val uri = FileProvider.getUriForFile(context, "com.example.templateeditorapp", imageFile)

            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            startActivity(Intent.createChooser(intent, "Share QR code"))
        }

        binding.btnCopyUrl.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("payme_link", viewModel.paymentLink.value)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(requireContext(), "Payme link copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveQrCodeToExternalStorage() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (viewModel.saveBitmap(requireContext())) {
                Toast.makeText(requireContext(), "QR code saved to media directory.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "QR code could not be saved.", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestPermissionsLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

}
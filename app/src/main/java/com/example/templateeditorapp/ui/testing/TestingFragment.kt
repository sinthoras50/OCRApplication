package com.example.templateeditorapp.ui.testing

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.templateeditorapp.databinding.FragmentTestingBinding
import com.example.templateeditorapp.ui.opencv.PictureTakenListener
import com.example.templateeditorapp.utils.ImageUtils
import com.example.templateeditorapp.utils.TEMP_PHOTO_KEY

class TestingFragment : Fragment() {

    companion object {
        fun newInstance() = TestingFragment()
    }

    lateinit var binding: FragmentTestingBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTestingBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imagePath = arguments?.getString(TEMP_PHOTO_KEY)
        val bitmap = ImageUtils.loadPhoto(imagePath!!, requireContext())
        binding.testingImageView.setImageBitmap(bitmap)
    }
}
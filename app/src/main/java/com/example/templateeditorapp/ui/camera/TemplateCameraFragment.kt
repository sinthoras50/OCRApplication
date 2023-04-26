package com.example.templateeditorapp.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.fragment.findNavController
import com.example.templateeditorapp.OcrApp
import com.example.templateeditorapp.R
import com.example.templateeditorapp.databinding.FragmentCameraBinding
import com.example.templateeditorapp.db.ImageDatabase
import com.example.templateeditorapp.ui.editor.EditorFragment
import com.example.templateeditorapp.ui.opencv.*
import com.example.templateeditorapp.utils.ImageUtils
import com.example.templateeditorapp.utils.TAG_IMAGE
import com.example.templateeditorapp.utils.TEMP_PHOTO_KEY
import com.example.templateeditorapp.utils.TEMP_PHOTO_PATH
import com.google.android.material.button.MaterialButton
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.lang.IllegalArgumentException


/**
 * A [Fragment] subclass that handles the camera functionality and takes pictures.
 * Implements [CameraBridgeViewBase.CvCameraViewListener2] and [PictureTakenListener] interfaces.
 */
class TemplateCameraFragment : Fragment(), CameraBridgeViewBase.CvCameraViewListener2, PictureTakenListener {

    companion object {
        fun newInstance() = CameraFragment()
    }

    private lateinit var viewModel: TemplateCameraViewModel
    private lateinit var cameraBridgeViewBase: CameraBridgeViewBase
    private lateinit var binding: FragmentCameraBinding
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<String>
    private lateinit var baseLoaderCallback: BaseLoaderCallback

    private lateinit var mRGBA: Mat
    private lateinit var mRGBAT: Mat
    private lateinit var mRotatedFrame: Mat


    /**
     * Called when the fragment is being created.
     * Initializes [viewModel] and [baseLoaderCallback] variables.
     *
     * @param savedInstanceState The saved instance state bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(TemplateCameraViewModel::class.java)

        baseLoaderCallback = object : BaseLoaderCallback(requireContext()) {
            override fun onManagerConnected(status: Int) {
                Log.d(TAG_CAM, "onManagerconnected: $status")
                when (status) {
                    LoaderCallbackInterface.SUCCESS -> {
                        Log.d(TAG_CAM, "onManagerConnected: OpenCV loaded")
                        cameraBridgeViewBase.enableView()
                    }
                    else -> {
                        Log.d(TAG_CAM, "onManagerConnected: Failed to load OpenCV")
                        super.onManagerConnected(status)
                    }
                }
            }
        }
    }


    /**
     * Called when the fragment view is being created.
     * Initializes [binding], [cameraBridgeViewBase], and [requestPermissionsLauncher] variables.
     * Also requests camera permission if it is not granted.
     *
     * @param inflater The LayoutInflater object that can be used to inflate views in the fragment.
     * @param container The parent view that the fragment's UI should be attached to.
     * @param savedInstanceState The saved instance state bundle.
     * @return The root view of the fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraBinding.inflate(inflater, container, false)

        cameraBridgeViewBase = binding.cameraSurface
//        cameraBridgeViewBase.setMaxFrameSize(1920, 1080)
        cameraBridgeViewBase.visibility = SurfaceView.VISIBLE
        cameraBridgeViewBase.setCvCameraViewListener(this)
        cameraBridgeViewBase.enableFpsMeter()
        (cameraBridgeViewBase as MyCameraView).setOnPictureTakenListener(this)

        requestPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                cameraBridgeViewBase.setCameraPermissionGranted()
            } else {
                // handle not granted
            }
        }

        return binding.root
    }


    /**
     * Called when the view hierarchy is created and the fragment view is bound to it.
     * Initializes UI components, handles UI events, and sets observers for [viewModel] properties.
     *
     * @param view The fragment's root view.
     * @param savedInstanceState The saved instance state bundle.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            cameraBridgeViewBase.setCameraPermissionGranted()
        } else {
            requestPermissionsLauncher.launch(Manifest.permission.CAMERA)
        }


        viewModel.focusMode.observe(viewLifecycleOwner) { focusMode ->
            val btn = binding.btnFocus as MaterialButton
            when(focusMode) {
                FocusMode.FIXED.value -> {
                    btn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_do_disturb_24)
                }
                FocusMode.AUTO.value -> {
                    btn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_center_focus_weak_24)
                }
            }
        }

        viewModel.flashMode.observe(viewLifecycleOwner) { flashMode ->
            val btn = binding.btnFlash as MaterialButton
            when(flashMode) {
                FlashMode.OFF.value -> {
                    btn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_flash_off_24)
                }
                FlashMode.ON.value -> {
                    btn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_flash_on_24)
                }
                FlashMode.AUTO.value -> {
                    btn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_flash_auto_24)
                }
            }
        }

        binding.btnCapture.setOnClickListener {

            viewModel.takePicture(TEMP_PHOTO_PATH, cameraBridgeViewBase)
            binding.loadingPanel.visibility = View.VISIBLE
            binding.btnCapture.visibility = View.GONE
            binding.btnFlash.visibility = View.GONE
            binding.btnFocus.visibility = View.GONE

        }

        binding.btnFlash.setOnClickListener {
            viewModel.onClickFlash()
        }

        binding.btnFocus.setOnClickListener {
            viewModel.onClickFocus()
        }

    }

    /**
     * Called when the fragment is resumed.
     */
    override fun onResume() {
        super.onResume()

        if (OpenCVLoader.initDebug()) {
            Log.d(TAG_CAM, "onResume: OpenCV initialized")
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        } else {
            Log.d(TAG_CAM, "onResume: OpenCV not initialized")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, requireContext(), baseLoaderCallback)
        }
    }


    /**
     * Called when the camera view is started.
     * @param width the width of the camera view.
     * @param height the height of the camera view.
     */
    override fun onCameraViewStarted(width: Int, height: Int) {
        mRGBA = Mat(height, width, CvType.CV_8UC4)
        mRGBAT = Mat(height, width, CvType.CV_8UC1)
        mRotatedFrame = Mat(width, height, CvType.CV_8UC4)
    }

    /**
     * Called when the camera view is stopped.
     * Releases the memory allocated to the Mats.
     */
    override fun onCameraViewStopped() {
        mRGBA.release()
        mRGBAT.release()
        mRotatedFrame.release()
    }

    /**
     * Called when a camera frame is captured.
     * Rotates the frame 90 degrees clockwise and returns a new Mat with a grid overlay.
     * @param inputFrame the current camera frame.
     * @return the processed frame with grid overlay.
     */
    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        if (inputFrame != null) {
            mRGBA = inputFrame.rgba()

            Core.rotate(mRGBA, mRotatedFrame, Core.ROTATE_90_CLOCKWISE)
            Log.d(TAG_IMAGE, "width = ${mRotatedFrame.width()} height = ${mRotatedFrame.height()}")
        }

        return viewModel.drawGrid(mRotatedFrame, 2)
    }


    /**
     * Called when a picture is taken.
     * Navigates to the [EditorFragment] with the temporary photo path as an argument.
     */
    override fun onPictureTaken() {
        binding.loadingPanel.visibility = View.GONE
        val args = Bundle()
        args.putString(TEMP_PHOTO_KEY, TEMP_PHOTO_PATH)
        findNavController().navigate(R.id.action_templateCameraFragment_to_editorFragment, args)
    }
}
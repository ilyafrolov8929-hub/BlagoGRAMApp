package com.kia.blagogramapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Bundle
import android.util.Range
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ExperimentalCamera2Interop
class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private var imageCapture: ImageCapture? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var cameraExecutor: ExecutorService
    
    private var camera: Camera? = null
    private var flashState = 0 // 0: off, 1: on, 2: auto, 3: torch
    private lateinit var flashBtn: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        DesignEngine.initializeScreen(this)

        val root = DesignEngine.initializeScreenWithFrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
        }

        previewView = PreviewView(this).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        root.addView(previewView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        val cameraPrefs = getSharedPreferences("camera_settings", Context.MODE_PRIVATE)
        val cameraType = cameraPrefs.getString("camera_type", "Camera 2") ?: "Camera 2"

        val toolbar = DesignEngine.createToolBar(
            context = this,
            title = "Камера ($cameraType)",
            leftButtonIconRes = R.drawable.ic_back,
            isDark = true,
            leftButtonClickListener = { finish() },
            rightButtonIconRes = R.drawable.ic_flash_off,
            rightButtonClickListener = { toggleFlash() }
        )
        // Инициализируем flashBtn, извлекая ImageView из контейнера правой кнопки тулбара
        val rightBtnContainer = toolbar.findViewWithTag<ViewGroup>("toolbar_right_icon")
        flashBtn = rightBtnContainer?.getChildAt(0) as ImageView

        root.addView(toolbar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val captureBtn = View(this).apply {
            background = ContextCompat.getDrawable(this@CameraActivity, R.drawable.bg_camera_capture)
            setOnClickListener { takePhoto() }
        }
        root.addView(captureBtn, LayoutHelper.createFrame(80, 80, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 0, 0, 40))

        val switchBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_camera_switch)
            setColorFilter(android.graphics.Color.WHITE)
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
            setOnClickListener {
                cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                    CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                startCamera()
            }
        }
        root.addView(switchBtn, LayoutHelper.createFrame(48, 48, Gravity.BOTTOM or Gravity.END, 0, 0, 40, 56))

        setContentView(root)
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        if (cameraPrefs.getBoolean("remember_camera", false)) {
            val lastCamera = cameraPrefs.getString("last_camera_selector", "back")
            cameraSelector = if (lastCamera == "front") CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        }
        
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraPrefs = getSharedPreferences("camera_settings", Context.MODE_PRIVATE)
            
            val isCamera2 = cameraPrefs.getString("camera_type", "Camera 2") == "Camera 2"
            val isStabilization = cameraPrefs.getBoolean("stabilization", false)
            val isExtendedFps = cameraPrefs.getBoolean("extended_fps", false)
            val isStaticZoom = cameraPrefs.getBoolean("static_zoom", false)

            // 1. Настройка Preview
            val previewBuilder = Preview.Builder()
            if (isCamera2) {
                val extender = Camera2Interop.Extender(previewBuilder)
                if (isStabilization) {
                    extender.setCaptureRequestOption(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON)
                    extender.setCaptureRequestOption(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON)
                }
                if (isExtendedFps) {
                    extender.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(15, 60))
                }
            }
            
            val preview = previewBuilder.build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // 2. Настройка ImageCapture
            val captureBuilder = ImageCapture.Builder()
                .setCaptureMode(if (isStaticZoom) ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(getFlashModeForCapture())
            
            if (isCamera2) {
                val extender = Camera2Interop.Extender(captureBuilder)
                if (isStabilization) {
                    extender.setCaptureRequestOption(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON)
                }
            }
            
            imageCapture = captureBuilder.build()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                
                if (isStaticZoom) {
                    camera?.cameraControl?.setZoomRatio(1.0f)
                }
                
                updateTorchState()
                
                if (cameraPrefs.getBoolean("remember_camera", false)) {
                    val side = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) "front" else "back"
                    cameraPrefs.edit().putString("last_camera_selector", side).apply()
                }
            } catch (exc: Exception) { exc.printStackTrace() }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleFlash() {
        flashState = (flashState + 1) % 4
        val iconRes = when (flashState) {
            0 -> R.drawable.ic_flash_off
            1 -> R.drawable.ic_flash_on
            2 -> R.drawable.ic_flash_auto
            else -> R.drawable.ic_flash_torch
        }
        flashBtn.setImageResource(iconRes)
        // Принудительно применяем цвет из темы, если нужно, так как setImageResource может сбросить фильтры в некоторых случаях
        flashBtn.setColorFilter(DesignEngine.Theme.colorIconPrimary, android.graphics.PorterDuff.Mode.SRC_IN)

        imageCapture?.flashMode = getFlashModeForCapture()
        updateTorchState()
    }

    private fun getFlashModeForCapture(): Int {
        return when (flashState) {
            1 -> ImageCapture.FLASH_MODE_ON
            2 -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
    }

    private fun updateTorchState() {
        camera?.cameraControl?.enableTorch(flashState == 3)
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(cacheDir, "cam_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val intent = Intent().apply { data = Uri.fromFile(photoFile) }
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
            override fun onError(exc: ImageCaptureException) { }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

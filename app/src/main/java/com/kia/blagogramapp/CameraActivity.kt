package com.kia.blagogramapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private var imageCapture: ImageCapture? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Инициализируем настройки экрана через DesignEngine (Edge-to-Edge, Theme)
        DesignEngine.initializeScreen(this)

        // 2. Создаем корневой FrameLayout. Для камеры принудительно черный фон.
        val root = DesignEngine.initializeScreenWithFrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
        }

        previewView = PreviewView(this).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        root.addView(previewView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        // 3. Добавляем новый тулбар в стиле DesignEngine
        val toolbar = DesignEngine.createToolBar(
            context = this,
            title = "Камера",
            leftButtonIconRes = R.drawable.ic_back,
            isDark = true, // Для камеры всегда используем темный вариант тулбара
            leftButtonClickListener = { finish() }
        )
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
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) { exc.printStackTrace() }
        }, ContextCompat.getMainExecutor(this))
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

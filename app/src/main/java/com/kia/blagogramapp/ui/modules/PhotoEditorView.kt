package com.kia.blagogramapp.ui.modules

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import com.kia.blagogramapp.LayoutHelper
import kotlin.math.max
import kotlin.math.min

class PhotoEditorView(context: Context) : View(context) {

    private var sourceBitmap: Bitmap? = null
    private val matrix = Matrix()
    private val inverseMatrix = Matrix()
    
    private val cropRect = RectF()
    private val imageRect = RectF()
    
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val overlayPaint = Paint().apply { color = 0x88000000.toInt(); style = Paint.Style.FILL }
    private val borderPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = LayoutHelper.dp(2).toFloat() }
    private val cornerPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }

    private var lastX = 0f
    private var lastY = 0f
    private var touchMode = 0 // 0: none, 1: drag, 2: zoom

    private var currentRotation = 0f

    fun setImageUri(uri: Uri) {
        val stream = context.contentResolver.openInputStream(uri)
        sourceBitmap = BitmapFactory.decodeStream(stream)
        stream?.close()
        reset()
        invalidate()
    }

    private fun reset() {
        val bitmap = sourceBitmap ?: return
        imageRect.set(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        
        post {
            val viewW = width.toFloat()
            val viewH = height.toFloat()
            val margin = LayoutHelper.dp(40).toFloat()
            
            cropRect.set(margin, margin, viewW - margin, viewH - margin)
            
            // Center inside crop
            matrix.setRectToRect(imageRect, cropRect, Matrix.ScaleToFit.CENTER)
            invalidate()
        }
    }

    fun rotate90() {
        currentRotation += 90f
        matrix.postRotate(90f, width / 2f, height / 2f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val bitmap = sourceBitmap ?: return
        
        // Draw bitmap
        canvas.drawBitmap(bitmap, matrix, paint)
        
        // Draw overlay
        val path = Path()
        path.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        path.addRect(cropRect, Path.Direction.CCW)
        canvas.drawPath(path, overlayPaint)
        
        // Draw crop border
        canvas.drawRect(cropRect, borderPaint)
        
        // Draw corners
        val l = LayoutHelper.dp(20).toFloat()
        val t = LayoutHelper.dp(3).toFloat()
        // Top-Left
        canvas.drawRect(cropRect.left - t, cropRect.top - t, cropRect.left + l, cropRect.top + t, cornerPaint)
        canvas.drawRect(cropRect.left - t, cropRect.top - t, cropRect.left + t, cropRect.top + l, cornerPaint)
        // Top-Right
        canvas.drawRect(cropRect.right - l, cropRect.top - t, cropRect.right + t, cropRect.top + t, cornerPaint)
        canvas.drawRect(cropRect.right - t, cropRect.top - t, cropRect.right + t, cropRect.top + l, cornerPaint)
        // Bottom-Left
        canvas.drawRect(cropRect.left - t, cropRect.bottom - t, cropRect.left + l, cropRect.bottom + t, cornerPaint)
        canvas.drawRect(cropRect.left - t, cropRect.bottom - l, cropRect.left + t, cropRect.bottom + t, cornerPaint)
        // Bottom-Right
        canvas.drawRect(cropRect.right - l, cropRect.bottom - t, cropRect.right + t, cropRect.bottom + t, cornerPaint)
        canvas.drawRect(cropRect.right - t, cropRect.bottom - l, cropRect.right + t, cropRect.bottom + t, cornerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x; lastY = event.y
                touchMode = 1
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchMode == 1) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    matrix.postTranslate(dx, dy)
                    lastX = event.x; lastY = event.y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> touchMode = 0
        }
        return true
    }

    fun getCroppedBitmap(): Bitmap? {
        val bitmap = sourceBitmap ?: return null
        
        matrix.invert(inverseMatrix)
        val mappedCrop = RectF()
        inverseMatrix.mapRect(mappedCrop, cropRect)
        
        val left = max(0f, mappedCrop.left).toInt()
        val top = max(0f, mappedCrop.top).toInt()
        val width = min(bitmap.width - left, mappedCrop.width().toInt())
        val height = min(bitmap.height - top, mappedCrop.height().toInt())
        
        if (width <= 0 || height <= 0) return null
        
        val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)
        
        // Apply rotation if needed
        if (currentRotation % 360 != 0f) {
            val m = Matrix()
            m.postRotate(currentRotation)
            return Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, m, true)
        }
        
        return cropped
    }
}

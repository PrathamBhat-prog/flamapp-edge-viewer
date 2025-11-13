package com.example.edgeviewer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {

    // Load native library
    companion object {
        init {
            System.loadLibrary("c++_shared")
            System.loadLibrary("native-lib")
        }
    }

    // JNI functions
    external fun stringFromJNI(): String
    external fun processFrame(bytes: ByteArray, width: Int, height: Int): ByteArray

    private val CAMERA_REQUEST_CODE = 100
    private lateinit var textureView: TextureView
    private lateinit var processedView: ImageView
    private lateinit var captureButton: Button
    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var previewSession: CameraCaptureSession? = null
    private lateinit var previewRequestBuilder: CaptureRequest.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Test JNI call
        val nativeMessage = stringFromJNI()
        Log.i("JNI_TEST", "Native says: $nativeMessage")

        // Initialize views
        textureView = findViewById(R.id.textureView)
        processedView = findViewById(R.id.processedView)
        captureButton = findViewById(R.id.captureButton)

        // Setup camera manager
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        textureView.surfaceTextureListener = textureListener

        // Ask for camera permission
        checkCameraPermission()

        // Set up the button's onClick listener
        captureButton.setOnClickListener {
            val capturedBitmap = captureFrame()
            if (capturedBitmap != null) {
                val byteArray = bitmapToByteArray(capturedBitmap)
                if (byteArray != null) {
                    val processedBytes = processFrame(byteArray, capturedBitmap.width, capturedBitmap.height)
                    val processedBitmap = byteArrayToBitmap(processedBytes, capturedBitmap.width, capturedBitmap.height)
                    if (processedBitmap != null) {
                        showProcessedFrame(processedBitmap)
                    }
                }
            }
        }
    }

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            startCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

        // We no longer do real-time processing here
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    private fun checkCameraPermission() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), CAMERA_REQUEST_CODE)
        } else {
            startCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }

    private fun startCamera() {
        try {
            val cameraId = cameraManager.cameraIdList[0]
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return
            cameraManager.openCamera(cameraId, stateCallback, null)
        } catch (e: Exception) {
            Log.e("Camera", "Error starting camera: ${e.message}")
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            startPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            cameraDevice = null
        }
    }

    private fun startPreview() {
        try {
            val surfaceTexture = textureView.surfaceTexture!!
            surfaceTexture.setDefaultBufferSize(textureView.width, textureView.height)
            val surface = Surface(surfaceTexture)

            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(surface)

            cameraDevice!!.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    previewSession = session
                    previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                    session.setRepeatingRequest(previewRequestBuilder.build(), null, null)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }, null)
        } catch (e: Exception) {
            Log.e("Camera", "Preview error: ${e.message}")
        }
    }

    fun captureFrame(): Bitmap? {
        if (!textureView.isAvailable) {
            return null
        }
        return textureView.bitmap
    }

    fun bitmapToByteArray(bitmap: Bitmap): ByteArray? {
        if (bitmap.config != Bitmap.Config.ARGB_8888) {
            Log.e("JNI_ERROR", "Bitmap format is not ARGB_8888")
            return null
        }
        val byteBuffer = ByteBuffer.allocate(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(byteBuffer)
        return byteBuffer.array()
    }

    fun byteArrayToBitmap(bytes: ByteArray, width: Int, height: Int): Bitmap? {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val byteBuffer = ByteBuffer.wrap(bytes)
        bitmap.copyPixelsFromBuffer(byteBuffer)
        return bitmap
    }

    fun showProcessedFrame(bitmap: Bitmap) {
        runOnUiThread {
            processedView.setImageBitmap(bitmap)
        }
    }
}

package exam.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CaptureActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var txtStatus: TextView
    private lateinit var editSbd: EditText
    private lateinit var editPage: EditText
    private lateinit var cameraExecutor: ExecutorService

    private var imageCapture: ImageCapture? = null
    private val capturedUris = mutableListOf<Uri>()

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            txtStatus.text = "Camera permission denied"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)

        previewView = findViewById(R.id.previewView)
        txtStatus = findViewById(R.id.txtCaptureStatus)
        editSbd = findViewById(R.id.editSbd)
        editPage = findViewById(R.id.editPage)
        val btnCapture = findViewById<Button>(R.id.btnCapture)
        val btnClear = findViewById<Button>(R.id.btnClear)
        val btnDone = findViewById<Button>(R.id.btnDone)

        cameraExecutor = Executors.newSingleThreadExecutor()

        btnCapture.setOnClickListener { capturePhoto() }
        btnClear.setOnClickListener {
            capturedUris.clear()
            updateStatus()
        }
        btnDone.setOnClickListener { finishWithResult() }

        requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturePhoto() {
        val capture = imageCapture ?: return
        val sbdRaw = editSbd.text.toString().trim().ifEmpty { "SBD" }
        val page = editPage.text.toString().trim().toIntOrNull() ?: 1
        val sbd = sanitizeId(sbdRaw)
        val suffix = System.currentTimeMillis().toString().takeLast(4)
        val fileName = "${sbd}_cap${suffix}_${page}.jpg"
        val photoFile = File(cacheDir, fileName)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    txtStatus.text = "Capture error: ${exception.message}"
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val uri = FileProvider.getUriForFile(
                        this@CaptureActivity,
                        "${applicationContext.packageName}.fileprovider",
                        photoFile
                    )
                    capturedUris.add(uri)
                    updateStatus()
                }
            }
        )
    }

    private fun sanitizeId(value: String): String {
        val filtered = value.filter { it.isLetterOrDigit() }
        return if (filtered.isEmpty()) "SBD" else filtered
    }

    private fun updateStatus() {
        txtStatus.text = "Captured: ${capturedUris.size}"
    }

    private fun finishWithResult() {
        val intent = Intent().apply {
            putStringArrayListExtra(
                "capturedUris",
                ArrayList(capturedUris.map { it.toString() })
            )
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

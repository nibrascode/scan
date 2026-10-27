package com.nibras.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Size
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.nibras.scan.databinding.ActivityScanCameraBinding
import com.nibras.scan.repo.ScanSession
import com.nibras.scan.util.FileUtils
import com.nibras.scan.util.InsetsUtil
import com.nibras.scan.util.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ScanCameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanCameraBinding
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var torchOn = false
    private var capturing = false
    private var askedForCamera = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show()
            if (askedForCamera && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                )
            }
        }
    }

    private val pickImages = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris -> importUris(uris) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        InsetsUtil.apply(binding.root)

        binding.btnClose.setOnClickListener { confirmExit() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { confirmExit() }
        })
        binding.btnCapture.setOnClickListener { capturePhoto() }
        binding.btnFlash.setOnClickListener { toggleTorch() }
        binding.btnGallery.setOnClickListener {
            pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnDone.setOnClickListener {
            startActivity(Intent(this, EditPagesActivity::class.java))
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            askedForCamera = true
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()
        updatePageCount()
    }

    private fun confirmExit() {
        if (ScanSession.isEmpty()) {
            finish()
            return
        }
        AlertDialog.Builder(this)
            .setMessage(R.string.discard_scan_confirm)
            .setPositiveButton(R.string.discard) { _, _ ->
                ScanSession.getPages().toList().forEach { it.originalFile.delete() }
                ScanSession.clear()
                FileUtils.clearPagesDir(this)
                finish()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updatePageCount() {
        val count = ScanSession.size()
        binding.tvPageCount.text = count.toString()
        binding.btnDone.visibility = if (count > 0) View.VISIBLE else View.GONE
        binding.btnDone.text = getString(R.string.finish_scan_format, count)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = try {
                cameraProviderFuture.get()
            } catch (e: Exception) {
                Toast.makeText(this, R.string.camera_failed, Toast.LENGTH_LONG).show()
                return@addListener
            }

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val maxDim = SettingsManager.getPdfQuality(this).maxDimensionPx
            val jpeg = SettingsManager.getPdfQuality(this).jpegQuality
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(Size(maxDim, maxDim))
                .setJpegQuality(jpeg)
                .build()
            imageCapture = capture

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA, preview, capture
                )
                if (torchOn) camera?.cameraControl?.enableTorch(true)
            } catch (_: Exception) {
                Toast.makeText(this, R.string.camera_failed, Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleTorch() {
        val control = camera?.cameraControl ?: return
        torchOn = !torchOn
        control.enableTorch(torchOn)
        binding.btnFlash.alpha = if (torchOn) 1f else 0.55f
    }

    private fun capturePhoto() {
        val capture = imageCapture ?: return
        if (capturing) return
        capturing = true
        binding.btnCapture.isEnabled = false
        binding.btnCapture.alpha = 0.45f
        capture.targetRotation = binding.previewView.display?.rotation ?: Surface.ROTATION_0

        val rawFile = File(cacheDir, "raw_capture_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(rawFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    capturing = false
                    binding.btnCapture.isEnabled = true
                    binding.btnCapture.alpha = 1f
                    val intent = Intent(this@ScanCameraActivity, CropActivity::class.java)
                    intent.putExtra(CropActivity.EXTRA_RAW_PATH, rawFile.absolutePath)
                    startActivity(intent)
                }

                override fun onError(exception: ImageCaptureException) {
                    capturing = false
                    binding.btnCapture.isEnabled = true
                    binding.btnCapture.alpha = 1f
                    rawFile.delete()
                    Toast.makeText(this@ScanCameraActivity, R.string.capture_failed, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        Toast.makeText(this, R.string.processing, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val paths = withContext(Dispatchers.IO) {
                uris.mapNotNull { FileUtils.copyUriToCache(this@ScanCameraActivity, it)?.absolutePath }
            }
            if (paths.isEmpty()) {
                Toast.makeText(this@ScanCameraActivity, R.string.import_failed, Toast.LENGTH_LONG).show()
                return@launch
            }
            if (paths.size > 1) ScanSession.enqueueRaw(paths.drop(1))
            val intent = Intent(this@ScanCameraActivity, CropActivity::class.java)
            intent.putExtra(CropActivity.EXTRA_RAW_PATH, paths.first())
            intent.putExtra(CropActivity.EXTRA_FROM_IMPORT, true)
            startActivity(intent)
        }
    }
}

package com.nibras.scan.tools

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.nibras.scan.R
import com.nibras.scan.util.InsetsUtil
import java.util.concurrent.Executors

/** Live QR / barcode scanner (ML Kit, bundled, offline) + scan from an image + history. */
class QrScanActivity : AppCompatActivity() {

    private val scanner by lazy { BarcodeScanning.getClient() }
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var paused = false
    private lateinit var preview: PreviewView

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) startCamera() else Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show()
    }
    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            scanner.process(InputImage.fromFilePath(this, uri))
                .addOnSuccessListener { list -> if (list.isEmpty()) Toast.makeText(this, R.string.qr_none, Toast.LENGTH_LONG).show() else found(list.first()) }
                .addOnFailureListener { Toast.makeText(this, R.string.qr_none, Toast.LENGTH_LONG).show() }
        } catch (_: Exception) { Toast.makeText(this, R.string.import_failed, Toast.LENGTH_SHORT).show() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val (root, header) = Ui.screen(this, getString(R.string.tool_qr))
        val m = Ui.dp(this, 12)
        preview = PreviewView(this)
        val frame = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        frame.addView(preview, FrameLayout.LayoutParams(-1, -1))
        frame.addView(android.view.View(this).apply { setBackgroundResource(R.drawable.bg_scan_frame) },
            FrameLayout.LayoutParams(Ui.dp(this, 240), Ui.dp(this, 240), Gravity.CENTER))
        frame.addView(TextView(this).apply {
            text = getString(R.string.qr_hint); setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding(m, m, m, m)
        }, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        root.addView(frame, LinearLayout.LayoutParams(-1, 0, 1f))
        val row = LinearLayout(this).apply { setPadding(m, m, m, m) }
        row.addView(Ui.secondaryButton(this, getString(R.string.import_gallery)) {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }, LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f).apply { marginEnd = m })
        row.addView(Ui.primaryButton(this, getString(R.string.qr_history)) {
            startActivity(Intent(this, QrHistoryActivity::class.java))
        }, LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f))
        root.addView(row, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
        InsetsUtil.apply(root, header)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
        else permission.launch(Manifest.permission.CAMERA)
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get()
                val p = Preview.Builder().build().also { it.setSurfaceProvider(preview.surfaceProvider) }
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(executor) { proxy ->
                    val media = proxy.image
                    if (paused || media == null) { proxy.close(); return@setAnalyzer }
                    scanner.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                        .addOnSuccessListener { list -> if (list.isNotEmpty() && !paused) found(list.first()) }
                        .addOnCompleteListener { proxy.close() }
                }
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, p, analysis)
            } catch (_: Exception) {
                Toast.makeText(this, R.string.camera_failed, Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun found(b: Barcode) {
        val value = b.rawValue ?: b.displayValue ?: return
        paused = true
        QrHistory.add(this, QrHistory.Entry(value, formatName(b.format), System.currentTimeMillis()))
        QrActions.show(this, value) { paused = false }
    }

    private fun formatName(f: Int) = when (f) {
        Barcode.FORMAT_QR_CODE -> "QR"; Barcode.FORMAT_EAN_13 -> "EAN-13"; Barcode.FORMAT_EAN_8 -> "EAN-8"
        Barcode.FORMAT_CODE_128 -> "Code 128"; Barcode.FORMAT_CODE_39 -> "Code 39"; Barcode.FORMAT_UPC_A -> "UPC-A"
        Barcode.FORMAT_UPC_E -> "UPC-E"; Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"; Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_AZTEC -> "Aztec"; else -> "Barcode"
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
        scanner.close()
    }
}

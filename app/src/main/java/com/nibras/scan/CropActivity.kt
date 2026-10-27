package com.nibras.scan

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.nibras.scan.databinding.ActivityCropBinding
import com.nibras.scan.model.Page
import com.nibras.scan.repo.ScanSession
import com.nibras.scan.util.DocumentDetector
import com.nibras.scan.util.FileUtils
import com.nibras.scan.util.InsetsUtil
import com.nibras.scan.util.PerspectiveUtils
import com.nibras.scan.util.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

class CropActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RAW_PATH = "extra_raw_path"
        const val EXTRA_EXISTING_PAGE_ID = "extra_existing_page_id"
        const val EXTRA_FROM_IMPORT = "extra_from_import"
        private const val DISPLAY_MAX_DIM = 1280
    }

    private lateinit var binding: ActivityCropBinding
    private var displayBitmap: Bitmap? = null
    private var existingPageId: String? = null
    private var sourcePath: String? = null
    private var fromImport = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropBinding.inflate(layoutInflater)
        setContentView(binding.root)
        InsetsUtil.apply(binding.root)

        sourcePath = intent.getStringExtra(EXTRA_RAW_PATH)
        existingPageId = intent.getStringExtra(EXTRA_EXISTING_PAGE_ID)
        fromImport = intent.getBooleanExtra(EXTRA_FROM_IMPORT, false)
        if (existingPageId != null && sourcePath == null) {
            sourcePath = ScanSession.getPage(existingPageId!!)?.originalFile?.absolutePath
        }
        val path = sourcePath
        if (path == null || !File(path).exists()) {
            advanceOrFinish(deleteCurrent = false)
            return
        }
        if (fromImport) binding.btnRetake.text = getString(R.string.btn_skip)

        val bitmap = loadRotatedBitmap(path, DISPLAY_MAX_DIM)
        if (bitmap == null) {
            Toast.makeText(this, R.string.import_failed, Toast.LENGTH_LONG).show()
            advanceOrFinish(deleteCurrent = true)
            return
        }
        displayBitmap = bitmap
        binding.imgToCrop.setImageBitmap(bitmap)

        binding.imgToCrop.post {
            val current = displayBitmap ?: return@post
            val rect = computeDisplayedImageRect(current)
            binding.cropOverlay.setImageRect(rect)
            detectEdgesAsync(current, rect)
        }

        binding.btnRetake.setOnClickListener { advanceOrFinish(deleteCurrent = true) }
        binding.btnConfirmCrop.setOnClickListener { confirmCrop() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { advanceOrFinish(deleteCurrent = true) }
        })
    }

    override fun onDestroy() {
        binding.imgToCrop.setImageDrawable(null)
        displayBitmap?.let { if (!it.isRecycled) it.recycle() }
        displayBitmap = null
        super.onDestroy()
    }

    private fun loadRotatedBitmap(path: String, maxDim: Int): Bitmap? {
        val bitmap = FileUtils.decodeSampled(path, maxDim) ?: return null
        val orientation = try {
            ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun detectEdgesAsync(bitmap: Bitmap, rect: RectF) {
        lifecycleScope.launch {
            val quad = withContext(Dispatchers.Default) { DocumentDetector.detect(bitmap) }
            if (quad == null || isFinishing || displayBitmap == null) return@launch
            val scale = rect.width() / bitmap.width
            val viewQuad = FloatArray(8)
            for (i in 0 until 4) {
                viewQuad[i * 2] = rect.left + quad[i * 2] * scale
                viewQuad[i * 2 + 1] = rect.top + quad[i * 2 + 1] * scale
            }
            binding.cropOverlay.setCorners(viewQuad)
        }
    }

    private fun computeDisplayedImageRect(bitmap: Bitmap): RectF {
        val viewWidth = binding.imgToCrop.width.toFloat().coerceAtLeast(1f)
        val viewHeight = binding.imgToCrop.height.toFloat().coerceAtLeast(1f)
        val scale = min(viewWidth / bitmap.width, viewHeight / bitmap.height)
        val dw = bitmap.width * scale
        val dh = bitmap.height * scale
        val left = (viewWidth - dw) / 2f
        val top = (viewHeight - dh) / 2f
        return RectF(left, top, left + dw, top + dh)
    }

    private fun confirmCrop() {
        val preview = displayBitmap ?: return
        val path = sourcePath ?: return
        val viewCorners = binding.cropOverlay.getCorners()
        val rect = computeDisplayedImageRect(preview)
        val viewScale = rect.width() / preview.width
        if (viewScale <= 0f) return

        val displayCorners = FloatArray(8)
        for (i in 0 until 4) {
            displayCorners[i * 2] = (viewCorners[i * 2] - rect.left) / viewScale
            displayCorners[i * 2 + 1] = (viewCorners[i * 2 + 1] - rect.top) / viewScale
        }

        binding.progressCrop.visibility = View.VISIBLE
        binding.btnConfirmCrop.isEnabled = false
        binding.btnRetake.isEnabled = false
        val quality = SettingsManager.getPdfQuality(this)
        val previewW = preview.width
        val previewH = preview.height

        lifecycleScope.launch {
            val outputFile: File? = withContext(Dispatchers.Default) {
                var full: Bitmap? = null
                var warped: Bitmap? = null
                try {
                    full = loadRotatedBitmap(path, quality.maxDimensionPx)
                        ?: loadRotatedBitmap(path, DISPLAY_MAX_DIM)
                    val source = full ?: return@withContext null
                    val sx = source.width.toFloat() / previewW
                    val sy = source.height.toFloat() / previewH
                    val fullCorners = FloatArray(8)
                    for (i in 0 until 4) {
                        fullCorners[i * 2] = displayCorners[i * 2] * sx
                        fullCorners[i * 2 + 1] = displayCorners[i * 2 + 1] * sy
                    }
                    warped = PerspectiveUtils.warpToRectangle(source, fullCorners)
                    val file = FileUtils.newPageFile(this@CropActivity)
                    FileUtils.saveBitmap(warped, file, quality.jpegQuality)
                    file
                } catch (_: Throwable) {
                    null
                } finally {
                    val fullBmp = full
                    val warpedBmp = warped
                    if (fullBmp != null && fullBmp !== warpedBmp && !fullBmp.isRecycled) fullBmp.recycle()
                    if (warpedBmp != null && !warpedBmp.isRecycled) warpedBmp.recycle()
                }
            }

            if (outputFile == null) {
                Toast.makeText(this@CropActivity, R.string.crop_failed, Toast.LENGTH_LONG).show()
                binding.progressCrop.visibility = View.GONE
                binding.btnConfirmCrop.isEnabled = true
                binding.btnRetake.isEnabled = true
                return@launch
            }

            val existingId = existingPageId
            if (existingId != null) {
                ScanSession.getPage(existingId)?.let { page ->
                    val old = page.originalFile
                    page.originalFile = outputFile
                    page.rotationDegrees = 0
                    if (old.absolutePath != outputFile.absolutePath) old.delete()
                    ScanSession.touch()
                }
            } else {
                ScanSession.addPage(
                    Page(
                        originalFile = outputFile,
                        filter = SettingsManager.getDefaultFilter(this@CropActivity)
                    )
                )
            }
            if (existingPageId == null) File(path).delete()
            advanceOrFinish(deleteCurrent = false)
        }
    }

    /** Deletes the current raw file when skipping, then opens the next imported image if any. */
    private fun advanceOrFinish(deleteCurrent: Boolean) {
        if (deleteCurrent) sourcePath?.let { File(it).delete() }
        val next = ScanSession.pollRaw()
        if (next != null) {
            val intent = Intent(this, CropActivity::class.java)
            intent.putExtra(EXTRA_RAW_PATH, next)
            intent.putExtra(EXTRA_FROM_IMPORT, true)
            startActivity(intent)
        }
        finish()
    }
}

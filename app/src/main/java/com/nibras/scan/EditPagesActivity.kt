package com.nibras.scan

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nibras.scan.adapter.PagePagerAdapter
import com.nibras.scan.adapter.PageThumbAdapter
import com.nibras.scan.databinding.ActivityEditPagesBinding
import com.nibras.scan.databinding.DialogSaveDocumentBinding
import com.nibras.scan.model.FilterType
import com.nibras.scan.repo.ScanSession
import com.nibras.scan.util.FileUtils
import com.nibras.scan.util.FilterUtils
import com.nibras.scan.util.InsetsUtil
import com.nibras.scan.util.PdfUtils
import com.nibras.scan.util.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditPagesActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FROM_LIBRARY = "extra_from_library"
        /** Optional: directory and default name to save into (editing an existing PDF / merge). */
        const val EXTRA_TARGET_DIR = "extra_target_dir"
        const val EXTRA_TARGET_NAME = "extra_target_name"
    }

    private lateinit var binding: ActivityEditPagesBinding
    private lateinit var pagerAdapter: PagePagerAdapter
    private lateinit var thumbAdapter: PageThumbAdapter
    private val pages get() = ScanSession.pagesMutable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPagesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        InsetsUtil.apply(binding.root)

        if (pages.isEmpty()) {
            finish()
            return
        }

        pagerAdapter = PagePagerAdapter(pages)
        binding.pagerPages.adapter = pagerAdapter

        thumbAdapter = PageThumbAdapter(pages) { position ->
            binding.pagerPages.currentItem = position
        }
        binding.rvThumbnails.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.rvThumbnails.adapter = thumbAdapter
        attachDragToReorder()

        binding.pagerPages.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicator(position)
                thumbAdapter.selectedPosition = position
                binding.rvThumbnails.scrollToPosition(position)
            }
        })

        binding.btnPrevPage.setOnClickListener {
            val current = binding.pagerPages.currentItem
            if (current > 0) binding.pagerPages.currentItem = current - 1
        }
        binding.btnNextPage.setOnClickListener {
            val current = binding.pagerPages.currentItem
            if (current < pages.size - 1) binding.pagerPages.currentItem = current + 1
        }

        binding.chipOriginal.setOnClickListener { applyFilterToCurrent(FilterType.ORIGINAL) }
        binding.chipAuto.setOnClickListener { applyFilterToCurrent(FilterType.AUTO) }
        binding.chipBw.setOnClickListener { applyFilterToCurrent(FilterType.BLACK_WHITE) }
        binding.chipGrayscale.setOnClickListener { applyFilterToCurrent(FilterType.GRAYSCALE) }

        binding.btnRotatePage.setOnClickListener { rotateCurrent() }
        binding.btnCropPage.setOnClickListener { cropCurrentAgain() }
        binding.btnDeletePage.setOnClickListener { deleteCurrent() }
        binding.btnExtractText.setOnClickListener { extractTextFromCurrent() }

        binding.btnAddPage.setOnClickListener {
            if (intent.getBooleanExtra(EXTRA_FROM_LIBRARY, false)) {
                startActivity(Intent(this, ScanCameraActivity::class.java))
            }
            finish()
        }
        binding.btnCreatePdf.setOnClickListener { showSaveDialog() }

        updateIndicator(0)
        updateFilterChips(pages[0].filter)
    }

    override fun onResume() {
        super.onResume()
        if (pages.isEmpty()) {
            finish()
            return
        }
        pagerAdapter.notifyDataSetChanged()
        thumbAdapter.notifyDataSetChanged()
    }

    private fun currentPosition() = binding.pagerPages.currentItem

    private fun updateIndicator(position: Int) {
        binding.tvPageIndicator.text = "${position + 1} / ${pages.size}"
        updateFilterChips(pages.getOrNull(position)?.filter ?: FilterType.ORIGINAL)
    }

    private fun updateFilterChips(active: FilterType) {
        binding.chipOriginal.isSelected = active == FilterType.ORIGINAL
        binding.chipAuto.isSelected = active == FilterType.AUTO
        binding.chipBw.isSelected = active == FilterType.BLACK_WHITE
        binding.chipGrayscale.isSelected = active == FilterType.GRAYSCALE
    }

    private fun applyFilterToCurrent(filter: FilterType) {
        val pos = currentPosition()
        if (pos !in pages.indices) return
        pages[pos].filter = filter
        ScanSession.touch()
        pagerAdapter.notifyItemChanged(pos)
        thumbAdapter.notifyItemChanged(pos)
        updateFilterChips(filter)
    }

    private fun rotateCurrent() {
        val pos = currentPosition()
        if (pos !in pages.indices) return
        val page = pages[pos]
        page.rotationDegrees = (page.rotationDegrees + 90) % 360
        ScanSession.touch()
        pagerAdapter.notifyItemChanged(pos)
        thumbAdapter.notifyItemChanged(pos)
    }

    private fun cropCurrentAgain() {
        val pos = currentPosition()
        if (pos !in pages.indices) return
        val page = pages[pos]
        val intent = Intent(this, CropActivity::class.java)
        intent.putExtra(CropActivity.EXTRA_EXISTING_PAGE_ID, page.id)
        startActivity(intent)
    }

    private fun deleteCurrent() {
        val pos = currentPosition()
        if (pos !in pages.indices) return
        val page = pages[pos]
        AlertDialog.Builder(this)
            .setMessage(R.string.delete_page_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                page.originalFile.delete()
                ScanSession.removePage(page.id)
                if (pages.isEmpty()) {
                    finish()
                    return@setPositiveButton
                }
                pagerAdapter.notifyDataSetChanged()
                thumbAdapter.notifyDataSetChanged()
                val newPos = pos.coerceAtMost(pages.size - 1)
                binding.pagerPages.currentItem = newPos
                updateIndicator(newPos)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun extractTextFromCurrent() {
        val pos = currentPosition()
        if (pos !in pages.indices) return
        val page = pages[pos]
        val path = page.originalFile.absolutePath
        val rotation = page.rotationDegrees
        val filter = page.filter
        binding.btnExtractText.isEnabled = false
        lifecycleScope.launch {
            val tempPath = withContext(Dispatchers.Default) {
                try {
                    val src = FileUtils.decodeSampled(path, 2000) ?: return@withContext null
                    val rendered = FilterUtils.render(
                        src,
                        com.nibras.scan.model.Page(
                            originalFile = page.originalFile,
                            rotationDegrees = rotation,
                            filter = filter
                        )
                    )
                    val tempFile = File(cacheDir, "ocr_temp_${System.currentTimeMillis()}.jpg")
                    FileUtils.saveBitmap(
                        rendered,
                        tempFile,
                        SettingsManager.getPdfQuality(this@EditPagesActivity).jpegQuality
                    )
                    if (!rendered.isRecycled) rendered.recycle()
                    tempFile.absolutePath
                } catch (_: Throwable) {
                    null
                }
            }
            binding.btnExtractText.isEnabled = true
            if (tempPath == null) {
                Toast.makeText(this@EditPagesActivity, R.string.import_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val intent = Intent(this@EditPagesActivity, OcrActivity::class.java)
            intent.putExtra(OcrActivity.EXTRA_IMAGE_PATH, tempPath)
            startActivity(intent)
        }
    }

    private fun attachDragToReorder() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from == -1 || to == -1) return false
                thumbAdapter.moveItem(from, to)
                pagerAdapter.notifyDataSetChanged()
                if (currentPosition() == from) {
                    binding.pagerPages.setCurrentItem(to, false)
                }
                thumbAdapter.selectedPosition = binding.pagerPages.currentItem
                updateIndicator(binding.pagerPages.currentItem)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Reordering only - swiping off does not delete a page.
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.rvThumbnails)
    }

    private fun showSaveDialog() {
        val dialogBinding = DialogSaveDocumentBinding.inflate(layoutInflater)
        val targetDir = intent.getStringExtra(EXTRA_TARGET_DIR)?.let { File(it) }?.takeIf { it.isDirectory }
            ?: FileUtils.documentsDir(this)
        dialogBinding.etDocName.setText(intent.getStringExtra(EXTRA_TARGET_NAME) ?: FileUtils.defaultDocumentName())
        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = FileUtils.sanitizeDocumentName(dialogBinding.etDocName.text.toString())
                    .ifEmpty { FileUtils.defaultDocumentName() }
                val outputFile = File(targetDir, "$name.pdf")
                if (outputFile.exists()) {
                    AlertDialog.Builder(this)
                        .setMessage(R.string.overwrite_confirm)
                        .setPositiveButton(R.string.save) { _, _ -> createPdf(outputFile) }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                } else {
                    createPdf(outputFile)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun createPdf(outputFile: File) {
        val pagesSnapshot = pages.toList()
        val quality = SettingsManager.getPdfQuality(this)
        val maxDim = quality.maxDimensionPx

        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    PdfUtils.deleteSidecars(outputFile)
                    PdfUtils.createPdf(pagesSnapshot.size, outputFile, quality.jpegQuality) { i ->
                        val page = pagesSnapshot[i]
                        val src = FileUtils.decodeSampled(page.originalFile.absolutePath, maxDim)
                            ?: android.graphics.Bitmap.createBitmap(32, 32, android.graphics.Bitmap.Config.ARGB_8888)
                        val limited = FilterUtils.limitSize(src, maxDim)
                        if (limited !== src && !src.isRecycled) src.recycle()
                        FilterUtils.render(limited, page)
                    }
                    true
                } catch (_: Exception) {
                    false
                }
            }

            if (success) {
                com.nibras.scan.util.OcrIndexer.indexAsync(this@EditPagesActivity, outputFile)
                ScanSession.clear()
                FileUtils.clearPagesDir(this@EditPagesActivity)
                Toast.makeText(this@EditPagesActivity, R.string.document_saved, Toast.LENGTH_SHORT).show()
                val intent = Intent(this@EditPagesActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this@EditPagesActivity, R.string.pdf_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

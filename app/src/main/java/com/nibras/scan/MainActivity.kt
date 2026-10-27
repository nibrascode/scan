package com.nibras.scan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nibras.scan.adapter.DocumentAdapter
import com.nibras.scan.databinding.ActivityMainBinding
import com.nibras.scan.databinding.DialogSaveDocumentBinding
import com.nibras.scan.model.Document
import com.nibras.scan.repo.ScanSession
import com.nibras.scan.util.FileUtils
import com.nibras.scan.util.InsetsUtil
import com.nibras.scan.util.PdfUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: DocumentAdapter
    private var allDocuments: List<Document> = emptyList()
    private var loadJob: Job? = null

    private val pickImages = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris -> importUris(uris) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        InsetsUtil.apply(binding.root, binding.headerMain)

        adapter = DocumentAdapter(
            onClick = { doc -> openDocument(doc) },
            onMoreClick = { doc, anchor -> showDocumentMenu(doc, anchor) }
        )
        binding.rvDocuments.layoutManager = LinearLayoutManager(this)
        binding.rvDocuments.adapter = adapter

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.cardScan.setOnClickListener { startScan() }
        binding.btnScan.setOnClickListener { startScan() }
        binding.btnImport.setOnClickListener { openGallery() }
        binding.btnResume.setOnClickListener { openUnfinished() }
        binding.btnDiscardSession.setOnClickListener { confirmDiscardSession() }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        refreshDocuments()
        updateResumeBanner()
    }

    private fun startScan() {
        startActivity(Intent(this, ScanCameraActivity::class.java))
    }

    private fun openGallery() {
        pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        Toast.makeText(this, R.string.processing, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val paths = withContext(Dispatchers.IO) {
                uris.mapNotNull { FileUtils.copyUriToCache(this@MainActivity, it)?.absolutePath }
            }
            if (paths.isEmpty()) {
                Toast.makeText(this@MainActivity, R.string.import_failed, Toast.LENGTH_LONG).show()
                return@launch
            }
            val first = paths.first()
            if (paths.size > 1) ScanSession.enqueueRaw(paths.drop(1))
            val intent = Intent(this@MainActivity, CropActivity::class.java)
            intent.putExtra(CropActivity.EXTRA_RAW_PATH, first)
            intent.putExtra(CropActivity.EXTRA_FROM_IMPORT, true)
            startActivity(intent)
        }
    }

    private fun updateResumeBanner() {
        val count = ScanSession.size()
        if (count > 0) {
            binding.bannerResume.visibility = View.VISIBLE
            binding.tvResume.text = getString(R.string.resume_banner, count)
        } else {
            binding.bannerResume.visibility = View.GONE
        }
    }

    private fun openUnfinished() {
        if (ScanSession.isEmpty()) return
        val intent = Intent(this, EditPagesActivity::class.java)
        intent.putExtra(EditPagesActivity.EXTRA_FROM_LIBRARY, true)
        startActivity(intent)
    }

    private fun confirmDiscardSession() {
        AlertDialog.Builder(this)
            .setMessage(R.string.discard_scan_confirm)
            .setPositiveButton(R.string.discard) { _, _ ->
                ScanSession.getPages().toList().forEach { it.originalFile.delete() }
                ScanSession.clear()
                FileUtils.clearPagesDir(this)
                updateResumeBanner()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun refreshDocuments() {
        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            val query = binding.etSearch.text?.toString().orEmpty()
            val documents = withContext(Dispatchers.IO) {
                FileUtils.listDocuments(this@MainActivity).map { file ->
                    val (count, thumb) = PdfUtils.inspect(file)
                    Document(
                        name = file.nameWithoutExtension,
                        file = file,
                        pageCount = count,
                        createdAt = file.lastModified(),
                        thumb = thumb
                    )
                }
            }
            val previous = allDocuments
            allDocuments = documents
            previous.forEach { old ->
                val thumb = old.thumb ?: return@forEach
                if (documents.none { it.thumb === thumb } && !thumb.isRecycled) thumb.recycle()
            }
            applyFilter(query)
        }
    }

    private fun applyFilter(query: String) {
        val q = query.trim()
        val shown = if (q.isEmpty()) allDocuments
        else allDocuments.filter { it.name.contains(q, ignoreCase = true) }
        adapter.submitList(shown)
        val empty = shown.isEmpty()
        binding.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.rvDocuments.visibility = if (empty) View.GONE else View.VISIBLE
        if (empty && q.isNotEmpty() && allDocuments.isNotEmpty()) {
            binding.tvEmpty.text = getString(R.string.search_empty)
        } else {
            binding.tvEmpty.text = getString(R.string.no_documents)
        }
    }

    private fun openDocument(doc: Document) {
        try {
            startActivity(Intent.createChooser(FileUtils.buildViewIntent(this, doc.file), getString(R.string.share)))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.open_pdf_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDocumentMenu(doc: Document, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(getString(R.string.share))
        popup.menu.add(getString(R.string.rename))
        popup.menu.add(getString(R.string.delete))
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                getString(R.string.share) -> shareDocument(doc)
                getString(R.string.rename) -> renameDocument(doc)
                getString(R.string.delete) -> confirmDeleteDocument(doc)
            }
            true
        }
        popup.show()
    }

    private fun shareDocument(doc: Document) {
        startActivity(Intent.createChooser(FileUtils.buildShareIntent(this, doc.file), getString(R.string.share)))
    }

    private fun renameDocument(doc: Document) {
        val dialogBinding = DialogSaveDocumentBinding.inflate(layoutInflater)
        dialogBinding.etDocName.setText(doc.name)
        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = FileUtils.sanitizeDocumentName(dialogBinding.etDocName.text.toString())
                if (newName.isEmpty()) {
                    Toast.makeText(this, R.string.name_invalid, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val newFile = File(doc.file.parentFile, "$newName.pdf")
                if (newFile.absolutePath != doc.file.absolutePath && newFile.exists()) {
                    AlertDialog.Builder(this)
                        .setMessage(R.string.overwrite_confirm)
                        .setPositiveButton(R.string.save) { _, _ ->
                            newFile.delete()
                            PdfUtils.deleteSidecars(newFile)
                            if (doc.file.renameTo(newFile)) {
                                PdfUtils.moveSidecars(doc.file, newFile)
                            }
                            refreshDocuments()
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                } else if (doc.file.renameTo(newFile)) {
                    PdfUtils.moveSidecars(doc.file, newFile)
                    refreshDocuments()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDeleteDocument(doc: Document) {
        AlertDialog.Builder(this)
            .setMessage(R.string.delete_document_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                doc.file.delete()
                PdfUtils.deleteSidecars(doc.file)
                doc.thumb?.let { if (!it.isRecycled) it.recycle() }
                refreshDocuments()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}

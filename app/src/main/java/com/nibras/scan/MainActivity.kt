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
import android.graphics.Typeface
import android.view.Gravity
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.nibras.scan.tools.PdfImport
import com.nibras.scan.tools.QrScanActivity
import com.nibras.scan.tools.SignActivity
import com.nibras.scan.tools.Ui
import com.nibras.scan.util.Library
import com.nibras.scan.util.OcrIndexer
import com.nibras.scan.util.PdfTools
import com.nibras.scan.util.SettingsManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: DocumentAdapter
    private var allDocuments: List<Document> = emptyList()
    private var loadJob: Job? = null
    /** null = all documents (root); otherwise the open folder. */
    private var currentFolder: File? = null
    private var sort = Library.Sort.DATE_DESC
    private var pendingPdfAction: ((File) -> Unit)? = null

    private val pickOcrImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val f = withContext(Dispatchers.IO) { FileUtils.copyUriToCache(this@MainActivity, uri) }
            if (f == null) { toast(R.string.import_failed); return@launch }
            startActivity(Intent(this@MainActivity, OcrActivity::class.java).putExtra(OcrActivity.EXTRA_IMAGE_PATH, f.absolutePath))
        }
    }

    private val pickPdfs = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNullOrEmpty()) return@registerForActivityResult
        lifecycleScope.launch {
            val files = withContext(Dispatchers.IO) { uris.mapNotNull { PdfImport.copyPdf(this@MainActivity, it) } }
            val action = pendingPdfAction
            pendingPdfAction = null
            if (files.isEmpty()) { toast(R.string.pdf_unreadable); return@launch }
            if (action != null) {
                // Single-PDF tools (sign): import into the library first so the result is kept.
                val dest = withContext(Dispatchers.IO) {
                    val d = Library.uniqueFile(FileUtils.documentsDir(this@MainActivity), "PDF_" + FileUtils.defaultDocumentName())
                    files.first().copyTo(d, true); d
                }
                action(dest)
            } else openInEditor(files, getString(R.string.merged_name_prefix) + "_" + FileUtils.defaultDocumentName().substringAfter('_'))
        }
    }

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

        sort = runCatching { Library.Sort.valueOf(getPreferences(MODE_PRIVATE).getString("sort", sort.name)!!) }.getOrDefault(sort)
        binding.btnSort.setOnClickListener { chooseSort() }
        buildTools()

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

    private fun toast(res: Int) = Toast.makeText(this, res, Toast.LENGTH_SHORT).show()

    // ---------------------------------------------------------------- home tools

    private data class Tool(val title: Int, val icon: Int, val color: Int, val action: () -> Unit)

    private fun buildTools() {
        val tools = listOf(
            Tool(R.string.import_gallery, R.drawable.ic_gallery, R.color.icon_blue) { openGallery() },
            Tool(R.string.tool_ocr, R.drawable.ic_text, R.color.icon_purple) {
                pickOcrImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            Tool(R.string.tool_qr, R.drawable.ic_qr, R.color.icon_green) { startActivity(Intent(this, QrScanActivity::class.java)) },
            Tool(R.string.tool_merge, R.drawable.ic_merge, R.color.icon_amber) { startMerge() },
            Tool(R.string.tool_sign, R.drawable.ic_sign, R.color.icon_red) { choosePdf { signPdf(it) } },
            Tool(R.string.tool_pdf_tools, R.drawable.ic_pdf, R.color.nibras_bright_blue) { choosePdf { showDocumentTools(it) } }
        )
        val grid = binding.toolsGrid
        grid.removeAllViews()
        val gap = Ui.dp(this, 8)
        tools.chunked(3).forEach { rowTools ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowTools.forEachIndexed { i, t ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setBackgroundResource(R.drawable.bg_settings_card)
                    setPadding(gap, Ui.dp(context, 14), gap, Ui.dp(context, 12))
                    isClickable = true; isFocusable = true
                    contentDescription = getString(t.title)
                    setOnClickListener { t.action() }
                }
                val box = android.widget.FrameLayout(this).apply {
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = Ui.dp(context, 14).toFloat(); setColor(ContextCompat.getColor(context, t.color))
                    }
                }
                box.addView(ImageView(this).apply { setImageResource(t.icon) },
                    android.widget.FrameLayout.LayoutParams(Ui.dp(this, 24), Ui.dp(this, 24), Gravity.CENTER))
                card.addView(box, LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)))
                card.addView(TextView(this).apply {
                    text = getString(t.title); setTextColor(ContextCompat.getColor(context, R.color.white))
                    textSize = 12.5f; gravity = Gravity.CENTER; maxLines = 2
                    setTypeface(typeface, Typeface.BOLD)
                }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(this@MainActivity, 8) })
                row.addView(card, LinearLayout.LayoutParams(0, Ui.dp(this, 104), 1f).apply {
                    if (i > 0) marginStart = gap
                })
            }
            repeat(3 - rowTools.size) { row.addView(android.view.View(this), LinearLayout.LayoutParams(0, 1, 1f).apply { marginStart = gap }) }
            grid.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = gap })
        }
    }

    private fun ensureEmptySession(then: () -> Unit) {
        if (ScanSession.isEmpty()) { then(); return }
        AlertDialog.Builder(this)
            .setMessage(R.string.session_replace_confirm)
            .setPositiveButton(R.string.discard) { _, _ ->
                ScanSession.getPages().toList().forEach { it.originalFile.delete() }
                ScanSession.clear(); FileUtils.clearPagesDir(this); then()
            }
            .setNegativeButton(R.string.cancel, null).show()
    }

    /** Loads PDFs as pages into the editor (reorder, rotate, delete, filters, crop) and saves as one PDF. */
    private fun openInEditor(files: List<File>, name: String, dir: File? = null) {
        ensureEmptySession {
            toast(R.string.processing)
            lifecycleScope.launch {
                val n = withContext(Dispatchers.IO) { runCatching { PdfImport.loadIntoSession(this@MainActivity, files) }.getOrDefault(0) }
                if (n == 0) { toast(R.string.pdf_unreadable); return@launch }
                startActivity(Intent(this@MainActivity, EditPagesActivity::class.java)
                    .putExtra(EditPagesActivity.EXTRA_FROM_LIBRARY, true)
                    .putExtra(EditPagesActivity.EXTRA_TARGET_NAME, name)
                    .apply { dir?.let { putExtra(EditPagesActivity.EXTRA_TARGET_DIR, it.absolutePath) } })
            }
        }
    }

    private fun startMerge() {
        val docs = Library.allPdfs(FileUtils.documentsDir(this)).sortedByDescending { it.lastModified() }
        val options = arrayOf(getString(R.string.from_library), getString(R.string.from_device))
        AlertDialog.Builder(this).setTitle(R.string.tool_merge).setItems(options) { _, which ->
            if (which == 1 || docs.isEmpty()) { pendingPdfAction = null; pickPdfs.launch(arrayOf("application/pdf")); return@setItems }
            val checked = BooleanArray(docs.size)
            AlertDialog.Builder(this).setTitle(R.string.merge_choose)
                .setMultiChoiceItems(docs.map { it.nameWithoutExtension }.toTypedArray(), checked) { _, i, c -> checked[i] = c }
                .setPositiveButton(R.string.tool_merge) { _, _ ->
                    val chosen = docs.filterIndexed { i, _ -> checked[i] }
                    if (chosen.size < 2) toast(R.string.merge_need_two)
                    else openInEditor(chosen, getString(R.string.merged_name_prefix) + "_" + FileUtils.defaultDocumentName().substringAfter('_'))
                }
                .setNegativeButton(R.string.cancel, null).show()
        }.show()
    }

    /** Pick a library PDF (or one from the device) for a single-document tool. */
    private fun choosePdf(action: (File) -> Unit) {
        val docs = Library.allPdfs(FileUtils.documentsDir(this)).sortedByDescending { it.lastModified() }
        val names = (listOf(getString(R.string.from_device)) + docs.map { it.nameWithoutExtension }).toTypedArray()
        AlertDialog.Builder(this).setTitle(R.string.choose_document).setItems(names) { _, i ->
            if (i == 0) { pendingPdfAction = action; pickPdfs.launch(arrayOf("application/pdf")) } else action(docs[i - 1])
        }.show()
    }

    private fun signPdf(file: File) {
        startActivity(Intent(this, SignActivity::class.java).putExtra(SignActivity.EXTRA_PDF, file.absolutePath))
    }

    private fun chooseSort() {
        val labels = arrayOf(R.string.sort_newest, R.string.sort_oldest, R.string.sort_name_az, R.string.sort_name_za, R.string.sort_size)
            .map { getString(it) }.toTypedArray()
        AlertDialog.Builder(this).setTitle(R.string.sort)
            .setSingleChoiceItems(labels, sort.ordinal) { d, i ->
                sort = Library.Sort.values()[i]
                getPreferences(MODE_PRIVATE).edit().putString("sort", sort.name).apply()
                d.dismiss(); applyFilter(binding.etSearch.text?.toString().orEmpty())
            }.show()
    }

    // ---------------------------------------------------------------- folders

    private fun renderFolderChips() {
        val root = FileUtils.documentsDir(this)
        val chips = binding.folderChips
        chips.removeAllViews()
        fun chip(label: String, selected: Boolean, onClick: () -> Unit, onLong: (() -> Unit)? = null) {
            val tv = TextView(this).apply {
                text = label; isSelected = selected
                setBackgroundResource(R.drawable.bg_chip)
                setTextColor(ContextCompat.getColor(context, R.color.white)); textSize = 13f
                setPadding(Ui.dp(context, 14), Ui.dp(context, 8), Ui.dp(context, 14), Ui.dp(context, 8))
                setOnClickListener { onClick() }
                onLong?.let { l -> setOnLongClickListener { l(); true } }
            }
            chips.addView(tv, LinearLayout.LayoutParams(-2, -2).apply { marginStart = Ui.dp(this@MainActivity, 6) })
        }
        chip(getString(R.string.folder_all), currentFolder == null, { currentFolder = null; refreshDocuments() })
        Library.folders(root).forEach { f ->
            chip("📁 " + f.name, currentFolder?.absolutePath == f.absolutePath,
                { currentFolder = f; refreshDocuments() }, { folderMenu(f) })
        }
        chip("+ " + getString(R.string.new_folder), false, { askName(R.string.new_folder, "") { name ->
            if (Library.createFolder(root, name) == null) toast(R.string.name_invalid) else refreshDocuments()
        } })
    }

    private fun folderMenu(folder: File) {
        val opts = arrayOf(getString(R.string.rename), getString(R.string.delete_folder))
        AlertDialog.Builder(this).setTitle(folder.name).setItems(opts) { _, i ->
            if (i == 0) askName(R.string.rename, folder.name) { n ->
                val r = Library.renameFolder(folder, n)
                if (r == null) toast(R.string.name_invalid) else { if (currentFolder == folder) currentFolder = r; refreshDocuments() }
            } else AlertDialog.Builder(this).setMessage(R.string.delete_folder_confirm)
                .setPositiveButton(R.string.delete) { _, _ ->
                    Library.deleteFolderKeepDocs(FileUtils.documentsDir(this), folder)
                    if (currentFolder == folder) currentFolder = null
                    refreshDocuments()
                }.setNegativeButton(R.string.cancel, null).show()
        }.show()
    }

    private fun askName(title: Int, initial: String, onOk: (String) -> Unit) {
        val et = EditText(this).apply { setText(initial); setSingleLine(); selectAll() }
        val box = android.widget.FrameLayout(this).apply {
            setPadding(Ui.dp(context, 20), Ui.dp(context, 8), Ui.dp(context, 20), 0); addView(et)
        }
        AlertDialog.Builder(this).setTitle(title).setView(box)
            .setPositiveButton(R.string.save) { _, _ -> onOk(et.text.toString()) }
            .setNegativeButton(R.string.cancel, null).show()
    }

    private fun moveToFolder(doc: Document) {
        val root = FileUtils.documentsDir(this)
        val folders = Library.folders(root)
        val names = (listOf(getString(R.string.folder_root)) + folders.map { it.name } + listOf("+ " + getString(R.string.new_folder))).toTypedArray()
        AlertDialog.Builder(this).setTitle(R.string.move_to_folder).setItems(names) { _, i ->
            when {
                i == 0 -> { Library.move(doc.file, root); refreshDocuments() }
                i <= folders.size -> { Library.move(doc.file, folders[i - 1]); refreshDocuments() }
                else -> askName(R.string.new_folder, "") { n ->
                    val f = Library.createFolder(root, n)
                    if (f == null) toast(R.string.name_invalid) else { Library.move(doc.file, f); refreshDocuments() }
                }
            }
        }.show()
    }

    // ---------------------------------------------------------------- list

    private fun refreshDocuments() {
        loadJob?.cancel()
        currentFolder?.let { if (!it.isDirectory) currentFolder = null }
        renderFolderChips()
        loadJob = lifecycleScope.launch {
            val query = binding.etSearch.text?.toString().orEmpty()
            val root = FileUtils.documentsDir(this@MainActivity)
            val folder = currentFolder
            val documents = withContext(Dispatchers.IO) {
                val files = if (folder == null) Library.allPdfs(root) else Library.pdfsIn(folder)
                files.map { file ->
                    val (count, thumb) = PdfUtils.inspect(file)
                    Document(
                        name = file.nameWithoutExtension,
                        file = file,
                        pageCount = count,
                        createdAt = file.lastModified(),
                        thumb = thumb,
                        folder = file.parentFile?.takeIf { it.absolutePath != root.absolutePath }?.name,
                        text = Library.ocrText(file)
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
            // Older documents (saved before 1.1.0) get their searchable text built in the background.
            documents.filter { !Library.ocrFile(it.file).exists() }.take(5).forEach {
                OcrIndexer.indexAsync(this@MainActivity, it.file)
            }
        }
    }

    private fun applyFilter(query: String) {
        val q = query.trim()
        val filtered = if (q.isEmpty()) allDocuments
        else allDocuments.filter { Library.matches(it.name, it.text, q) }
        val shown = Library.sort(filtered, sort, { it.name }, { it.createdAt }, { it.file.length() })
        adapter.submitList(shown)
        binding.tvListTitle.text = currentFolder?.name ?: getString(R.string.recent_documents)
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
        val actions = linkedMapOf<String, () -> Unit>(
            getString(R.string.share) to { shareDocument(doc) },
            getString(R.string.pdf_tools_more) to { showDocumentTools(doc.file) },
            getString(R.string.move_to_folder) to { moveToFolder(doc) },
            getString(R.string.rename) to { renameDocument(doc) },
            getString(R.string.delete) to { confirmDeleteDocument(doc) }
        )
        actions.keys.forEach { popup.menu.add(it) }
        popup.setOnMenuItemClickListener { item -> actions[item.title.toString()]?.invoke(); true }
        popup.show()
    }

    /** Every per-document tool in one sheet. */
    private fun showDocumentTools(file: File) {
        val items = listOf<Pair<Int, () -> Unit>>(
            R.string.edit_pages to { openInEditor(listOf(file), file.nameWithoutExtension, file.parentFile) },
            R.string.extract_text to {
                startActivity(Intent(this, OcrActivity::class.java).putExtra(OcrActivity.EXTRA_PDF_PATH, file.absolutePath))
            },
            R.string.tool_sign to { signPdf(file) },
            R.string.add_watermark to { askName(R.string.add_watermark, getString(R.string.watermark_default)) { t -> watermark(file, t) } },
            R.string.protect_pdf to { askPassword(file) },
            R.string.compress_pdf to { compress(file) },
            R.string.export_jpg to { exportJpg(file) },
            R.string.export_txt to { exportTxt(file) },
            R.string.share to { startActivity(Intent.createChooser(FileUtils.buildShareIntent(this, file), getString(R.string.share))) }
        )
        AlertDialog.Builder(this).setTitle(file.nameWithoutExtension)
            .setItems(items.map { getString(it.first) }.toTypedArray()) { _, i -> items[i].second() }
            .show()
    }

    private fun runTool(file: File, suffix: Int, work: (File) -> Unit) {
        toast(R.string.processing)
        lifecycleScope.launch {
            val out = withContext(Dispatchers.IO) {
                runCatching {
                    val dest = Library.uniqueFile(file.parentFile!!, file.nameWithoutExtension + "_" + getString(suffix))
                    work(dest)
                    Library.ocrFile(file).takeIf { it.exists() }?.copyTo(Library.ocrFile(dest), true)
                    dest
                }.getOrNull()
            }
            if (out == null) toast(R.string.pdf_failed)
            else { Toast.makeText(this@MainActivity, getString(R.string.saved_as, out.name), Toast.LENGTH_LONG).show(); refreshDocuments() }
        }
    }

    private fun watermark(file: File, text: String) {
        if (text.isBlank()) return
        val q = SettingsManager.getPdfQuality(this)
        runTool(file, R.string.suffix_watermark) { dest ->
            PdfTools.rebuild(file, dest, q.maxDimensionPx, q.jpegQuality) { _, c, w, h -> PdfTools.drawWatermark(c, w, h, text.trim()) }
        }
    }

    private fun askPassword(file: File) {
        val et = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = getString(R.string.password_hint)
        }
        val box = android.widget.FrameLayout(this).apply { setPadding(Ui.dp(context, 20), Ui.dp(context, 8), Ui.dp(context, 20), 0); addView(et) }
        AlertDialog.Builder(this).setTitle(R.string.protect_pdf).setMessage(R.string.password_note).setView(box)
            .setPositiveButton(R.string.save) { _, _ ->
                val pw = et.text.toString()
                if (pw.length < 4) toast(R.string.password_short)
                else runTool(file, R.string.suffix_protected) { dest -> PdfTools.protect(file, dest, pw) }
            }.setNegativeButton(R.string.cancel, null).show()
    }

    private fun compress(file: File) {
        val levels = SettingsManager.PdfQuality.values()
        val labels = arrayOf(getString(R.string.quality_low), getString(R.string.quality_medium), getString(R.string.quality_high))
        AlertDialog.Builder(this).setTitle(R.string.compress_pdf).setItems(labels) { _, i ->
            val q = levels[i]
            val jpeg = when (q) { SettingsManager.PdfQuality.LOW -> 55; SettingsManager.PdfQuality.MEDIUM -> 72; else -> 88 }
            runTool(file, R.string.suffix_compressed) { dest -> PdfTools.rebuild(file, dest, q.maxDimensionPx, jpeg) }
        }.show()
    }

    private fun exportJpg(file: File) {
        toast(R.string.processing)
        lifecycleScope.launch {
            val files = withContext(Dispatchers.IO) {
                runCatching { PdfTools.exportJpgs(file, File(cacheDir, "export_jpg"), 2400, 92) }.getOrDefault(emptyList())
            }
            if (files.isEmpty()) { toast(R.string.pdf_failed); return@launch }
            val uris = ArrayList(files.map { FileUtils.shareUriFor(this@MainActivity, it) })
            val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris[0]) else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                clipData = android.content.ClipData.newRawUri("jpg", uris[0]).apply { uris.drop(1).forEach { addItem(android.content.ClipData.Item(it)) } }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.export_jpg)))
        }
    }

    private fun exportTxt(file: File) {
        toast(R.string.processing)
        lifecycleScope.launch {
            val txt = withContext(Dispatchers.IO) {
                runCatching {
                    val text = Library.ocrText(file).ifBlank { OcrIndexer.index(this@MainActivity, file) }
                    val dir = File(cacheDir, "export_txt").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
                    File(dir, file.nameWithoutExtension + ".txt").apply { writeText(text) }
                }.getOrNull()
            }
            if (txt == null || txt.length() == 0L) { toast(R.string.no_text_found); return@launch }
            val uri = FileUtils.shareUriFor(this@MainActivity, txt)
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"; putExtra(Intent.EXTRA_STREAM, uri)
                clipData = android.content.ClipData.newRawUri("txt", uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, getString(R.string.export_txt)))
        }
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

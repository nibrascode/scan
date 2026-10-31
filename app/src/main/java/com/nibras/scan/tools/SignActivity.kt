package com.nibras.scan.tools

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.nibras.scan.R
import com.nibras.scan.util.InsetsUtil
import com.nibras.scan.util.Library
import com.nibras.scan.util.OcrIndexer
import com.nibras.scan.util.PdfTools
import com.nibras.scan.util.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Draw a signature, then drag/pinch it onto any page of a PDF and save a signed copy. */
class SignActivity : AppCompatActivity() {

    companion object { const val EXTRA_PDF = "extra_pdf" }

    private lateinit var pdf: File
    private lateinit var pad: SignaturePadView
    private lateinit var placement: PlacementView
    private lateinit var drawPanel: LinearLayout
    private lateinit var placePanel: LinearLayout
    private lateinit var tvPage: TextView
    private var pageIndex = 0
    private var pageCount = 0
    private var signature: Bitmap? = null

    private fun sigFile() = File(filesDir, "signatures/signature.png")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pdf = File(intent.getStringExtra(EXTRA_PDF) ?: run { finish(); return })
        val (root, header) = Ui.screen(this, getString(R.string.tool_sign))
        val a = this
        val m = Ui.dp(this, 12)

        // --- Draw panel
        drawPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(m, m, m, m) }
        drawPanel.addView(TextView(this).apply {
            text = getString(R.string.sign_draw_hint); setTextColor(ContextCompat.getColor(a, R.color.gray_dark)); textSize = 14f
        })
        pad = SignaturePadView(this).apply { setBackgroundResource(R.drawable.bg_card) }
        drawPanel.addView(pad, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = m })
        val colors = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        listOf("#0D1B3E", "#1565C0", "#000000").forEach { hex ->
            colors.addView(View(this).apply {
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL; setColor(Color.parseColor(hex))
                }
                setOnClickListener { pad.inkColor = Color.parseColor(hex) }
            }, LinearLayout.LayoutParams(Ui.dp(this, 30), Ui.dp(this, 30)).apply { marginEnd = m })
        }
        colors.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        colors.addView(Ui.secondaryButton(this, getString(R.string.undo)) { pad.undo() }, LinearLayout.LayoutParams(-2, Ui.dp(this, 40)).apply { marginEnd = m })
        colors.addView(Ui.secondaryButton(this, getString(R.string.clear)) { pad.clear() }, LinearLayout.LayoutParams(-2, Ui.dp(this, 40)))
        drawPanel.addView(colors, LinearLayout.LayoutParams(-1, -2).apply { topMargin = m })
        if (sigFile().exists()) {
            drawPanel.addView(Ui.secondaryButton(this, getString(R.string.sign_use_saved)) {
                BitmapFactory.decodeFile(sigFile().absolutePath)?.let { startPlacing(it) }
            }, LinearLayout.LayoutParams(-1, Ui.dp(this, 46)).apply { topMargin = m })
        }
        drawPanel.addView(Ui.primaryButton(this, getString(R.string.sign_next)) {
            val bmp = pad.export()
            if (bmp == null) { Toast.makeText(this, R.string.sign_empty, Toast.LENGTH_SHORT).show(); return@primaryButton }
            runCatching { sigFile().parentFile?.mkdirs(); sigFile().outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            startPlacing(bmp)
        }, LinearLayout.LayoutParams(-1, Ui.dp(this, 50)).apply { topMargin = m })

        // --- Place panel
        placePanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(m, m, m, m); visibility = View.GONE }
        placePanel.addView(TextView(this).apply {
            text = getString(R.string.sign_place_hint); setTextColor(ContextCompat.getColor(a, R.color.gray_dark)); textSize = 14f
        })
        placement = PlacementView(this)
        placePanel.addView(placement, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = m })
        val nav = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        nav.addView(Ui.secondaryButton(this, "‹") { showPage(pageIndex - 1) }, LinearLayout.LayoutParams(Ui.dp(this, 56), Ui.dp(this, 40)))
        tvPage = TextView(this).apply { gravity = Gravity.CENTER; setTextColor(ContextCompat.getColor(a, R.color.gray_dark)) }
        nav.addView(tvPage, LinearLayout.LayoutParams(0, -2, 1f))
        nav.addView(Ui.secondaryButton(this, "›") { showPage(pageIndex + 1) }, LinearLayout.LayoutParams(Ui.dp(this, 56), Ui.dp(this, 40)))
        placePanel.addView(nav, LinearLayout.LayoutParams(-1, -2).apply { topMargin = m })
        placePanel.addView(Ui.primaryButton(this, getString(R.string.sign_apply)) { apply() },
            LinearLayout.LayoutParams(-1, Ui.dp(this, 50)).apply { topMargin = m })

        val body = FrameLayout(this)
        body.addView(drawPanel, FrameLayout.LayoutParams(-1, -1))
        body.addView(placePanel, FrameLayout.LayoutParams(-1, -1))
        root.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        InsetsUtil.apply(root, header)
    }

    private fun startPlacing(sig: Bitmap) {
        signature = sig
        drawPanel.visibility = View.GONE
        placePanel.visibility = View.VISIBLE
        lifecycleScope.launch {
            pageCount = withContext(Dispatchers.IO) { PdfTools.pageCount(pdf) }
            if (pageCount == 0) { Toast.makeText(this@SignActivity, R.string.pdf_unreadable, Toast.LENGTH_LONG).show(); finish(); return@launch }
            showPage(pageCount - 1)
        }
    }

    private fun showPage(i: Int) {
        if (i < 0 || i >= pageCount) return
        pageIndex = i
        tvPage.text = getString(R.string.page_of, i + 1, pageCount)
        lifecycleScope.launch {
            val bmp = withContext(Dispatchers.IO) { PdfTools.renderPage(pdf, i, 1400) }
            placement.page = bmp
            placement.signature = signature
        }
    }

    private fun apply() {
        val sig = signature ?: return
        val rel = android.graphics.RectF(placement.rel)
        val target = pageIndex
        Toast.makeText(this, R.string.processing, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val out = withContext(Dispatchers.IO) {
                runCatching {
                    val q = SettingsManager.getPdfQuality(this@SignActivity)
                    val dest = Library.uniqueFile(pdf.parentFile!!, pdf.nameWithoutExtension + "_" + getString(R.string.suffix_signed))
                    PdfTools.rebuild(pdf, dest, q.maxDimensionPx, q.jpegQuality) { idx, c, w, h ->
                        if (idx == target) PdfTools.drawSignature(c, w, h, sig, rel)
                    }
                    Library.ocrFile(pdf).takeIf { it.exists() }?.copyTo(Library.ocrFile(dest), true)
                    dest
                }.getOrNull()
            }
            if (out != null) {
                if (!Library.ocrFile(out).exists()) OcrIndexer.indexAsync(this@SignActivity, out)
                Toast.makeText(this@SignActivity, getString(R.string.saved_as, out.name), Toast.LENGTH_LONG).show()
                finish()
            } else Toast.makeText(this@SignActivity, R.string.pdf_failed, Toast.LENGTH_LONG).show()
        }
    }
}

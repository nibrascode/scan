package com.nibras.scan

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import com.nibras.scan.model.Page
import com.nibras.scan.repo.ScanSession
import com.nibras.scan.tools.PlacementView
import com.nibras.scan.tools.QrHistory
import com.nibras.scan.tools.QrHistoryActivity
import com.nibras.scan.tools.SignActivity
import com.nibras.scan.util.FileUtils
import com.nibras.scan.util.Library
import com.nibras.scan.util.PdfTools
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/** Renders real app screens for the store listing. Run with STORE_SHOTS_DIR set. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class StoreScreenshotsTest {

    private val outRoot = System.getenv("STORE_SHOTS_DIR")
    private val ctx get() = ApplicationProvider.getApplicationContext<android.app.Application>()

    data class Sample(val title: String, val lines: List<String>, val docs: List<String>, val folders: List<String>, val qr: List<String>, val ocr: String)

    private val samples = mapOf(
        "az" to Sample("MÜQAVİLƏ № 24/10", listOf("Bakı şəhəri", "Tərəflər aşağıdakı şərtlərlə", "razılığa gəlmişlər:", "1. Xidmətin dəyəri 450 AZN", "2. Müddət: 12 ay", "3. Ödəniş aylıq aparılır"),
            listOf("Müqavilə 24-10", "Faktura oktyabr", "Pasport surəti", "Diplom", "Kirayə sənədi"), listOf("İş", "Şəxsi", "Qəbzlər"),
            listOf("https://nibrascode.com", "4760123456789", "WIFI:S:Ev;T:WPA;P:****;;"),
            "MÜQAVİLƏ № 24/10\nBakı şəhəri\n\nTərəflər aşağıdakı şərtlərlə razılığa gəlmişlər:\n1. Xidmətin dəyəri 450 AZN\n2. Müddət: 12 ay\n3. Ödəniş aylıq aparılır\n\nİmza: ____________"),
        "en" to Sample("SERVICE AGREEMENT", listOf("London, 24 October 2026", "The parties agree to the", "following terms:", "1. Service fee: £450", "2. Term: 12 months", "3. Monthly payment"),
            listOf("Agreement 24-10", "Invoice October", "Passport copy", "Diploma", "Lease contract"), listOf("Work", "Personal", "Receipts"),
            listOf("https://nibrascode.com", "5012345678900", "WIFI:S:Home;T:WPA;P:****;;"),
            "SERVICE AGREEMENT\nLondon, 24 October 2026\n\nThe parties agree to the following terms:\n1. Service fee: £450\n2. Term: 12 months\n3. Monthly payment\n\nSignature: ____________"),
        "tr" to Sample("HİZMET SÖZLEŞMESİ", listOf("İstanbul, 24 Ekim 2026", "Taraflar aşağıdaki şartlarda", "anlaşmıştır:", "1. Hizmet bedeli: 4.500 TL", "2. Süre: 12 ay", "3. Ödeme aylık yapılır"),
            listOf("Sözleşme 24-10", "Fatura Ekim", "Pasaport kopyası", "Diploma", "Kira sözleşmesi"), listOf("İş", "Kişisel", "Fişler"),
            listOf("https://nibrascode.com", "8690123456789", "WIFI:S:Ev;T:WPA;P:****;;"),
            "HİZMET SÖZLEŞMESİ\nİstanbul, 24 Ekim 2026\n\nTaraflar aşağıdaki şartlarda anlaşmıştır:\n1. Hizmet bedeli: 4.500 TL\n2. Süre: 12 ay\n3. Ödeme aylık yapılır\n\nİmza: ____________"),
        "ru" to Sample("ДОГОВОР № 24/10", listOf("г. Москва, 24 октября 2026", "Стороны договорились", "о следующем:", "1. Стоимость услуг: 45 000 ₽", "2. Срок: 12 месяцев", "3. Оплата ежемесячно"),
            listOf("Договор 24-10", "Счёт октябрь", "Копия паспорта", "Диплом", "Договор аренды"), listOf("Работа", "Личное", "Чеки"),
            listOf("https://nibrascode.com", "4601234567890", "WIFI:S:Dom;T:WPA;P:****;;"),
            "ДОГОВОР № 24/10\nг. Москва, 24 октября 2026\n\nСтороны договорились о следующем:\n1. Стоимость услуг: 45 000 ₽\n2. Срок: 12 месяцев\n3. Оплата ежемесячно\n\nПодпись: ____________"),
        "ar" to Sample("عقد خدمة رقم 24/10", listOf("الرياض، 24 أكتوبر 2026", "اتفق الطرفان على", "الشروط التالية:", "1. قيمة الخدمة: 450 ريال", "2. المدة: 12 شهرًا", "3. الدفع شهريًا"),
            listOf("عقد 24-10", "فاتورة أكتوبر", "صورة الجواز", "شهادة", "عقد إيجار"), listOf("العمل", "شخصي", "إيصالات"),
            listOf("https://nibrascode.com", "6281234567890", "WIFI:S:Home;T:WPA;P:****;;"),
            "عقد خدمة رقم 24/10\nالرياض، 24 أكتوبر 2026\n\nاتفق الطرفان على الشروط التالية:\n1. قيمة الخدمة: 450 ريال\n2. المدة: 12 شهرًا\n3. الدفع شهريًا\n\nالتوقيع: ____________"),
    )

    private fun docImage(s: Sample, tint: Int = Color.WHITE): Bitmap {
        val b = Bitmap.createBitmap(900, 1270, Bitmap.Config.ARGB_8888)
        b.eraseColor(tint)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0D1B3E"); textSize = 46f; typeface = Typeface.DEFAULT_BOLD }
        val rtl = s.title.any { it in '\u0600'..'\u06FF' }
        p.textAlign = if (rtl) Paint.Align.RIGHT else Paint.Align.LEFT
        val x = if (rtl) 830f else 70f
        c.drawText(s.title, x, 140f, p)
        p.typeface = Typeface.DEFAULT; p.textSize = 34f; p.color = Color.parseColor("#3A3F47")
        s.lines.forEachIndexed { i, l -> c.drawText(l, x, 240f + i * 62f, p) }
        p.color = Color.parseColor("#C9D1DC")
        for (i in 0 until 7) c.drawRect(70f, 680f + i * 56f, 830f - (i % 3) * 120f, 696f + i * 56f, p)
        p.color = Color.parseColor("#0D1B3E"); p.strokeWidth = 4f; p.style = Paint.Style.STROKE
        val path = android.graphics.Path().apply { moveTo(560f, 1150f); cubicTo(600f, 1060f, 640f, 1190f, 690f, 1110f); cubicTo(720f, 1070f, 760f, 1160f, 820f, 1120f) }
        c.drawPath(path, p)
        return b
    }

    private fun capture(a: Activity, file: File) {
        val root = a.window.decorView
        val w = 1080; val h = 2340
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, w, h)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.parseColor("#0A1128"))
        root.draw(Canvas(bmp))
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun <T : View> find(v: View, cls: Class<T>): T? {
        if (cls.isInstance(v)) return cls.cast(v)
        if (v is ViewGroup) for (i in 0 until v.childCount) find(v.getChildAt(i), cls)?.let { return it }
        return null
    }

    private fun field(o: Any, name: String): Any = o.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(o)!!

    @Test fun storeShots() {
        Assume.assumeTrue(outRoot != null)
        PDFBoxResourceLoader.init(ctx)
        for ((lang, s) in samples) {
            RuntimeEnvironment.setQualifiers("$lang-w411dp-h891dp-xxhdpi")
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(lang))
            ctx.getSharedPreferences("nibras_settings", 0).edit().putBoolean("privacy_accepted", true).apply()
            val out = File(outRoot, lang)
            // Library: folders + documents with thumbnails and OCR text.
            val root = FileUtils.documentsDir(ctx)
            root.deleteRecursively(); root.mkdirs()
            s.folders.forEach { Library.createFolder(root, it) }
            s.docs.forEachIndexed { i, name ->
                val dir = if (i == 1) File(root, s.folders[2]) else if (i == 3) File(root, s.folders[1]) else root
                val pdf = File(dir, "$name.pdf")
                PdfTools.writeJpegPdf(1, pdf, 70) { docImage(s, if (i % 2 == 0) Color.WHITE else Color.parseColor("#FFF8E7")) }
                pdf.setLastModified(1793000000000L - i * 86_400_000L)
                val thumb = Bitmap.createScaledBitmap(docImage(s), 160, 226, true)
                FileOutputStream(File(dir, "$name.thumb.jpg")).use { thumb.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                File(dir, "$name.nibrasmeta").writeText("${pdf.length()}:${pdf.lastModified()}\n${3 - i % 3}")
                Library.ocrFile(pdf).writeText(s.ocr)
            }
            ScanSession.clear()

            // 1 home
            val main = Robolectric.buildActivity(MainActivity::class.java).setup().get()
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(400); Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            capture(main, File(out, "01.png"))
            // 2 library scrolled to documents & folders
            (main.findViewById<View>(R.id.scrollMain) as androidx.core.widget.NestedScrollView).scrollTo(0, 1080)
            capture(main, File(out, "02.png"))

            // 3 edit pages (scan editor with filters)
            val pagesFiles = (0 until 3).map { i ->
                FileUtils.newPageFile(ctx).also { FileUtils.saveBitmap(docImage(s, if (i == 1) Color.parseColor("#FFF8E7") else Color.WHITE), it) }
            }
            pagesFiles.forEach { ScanSession.addPage(Page(originalFile = it)) }
            val edit = Robolectric.buildActivity(EditPagesActivity::class.java).setup().get()
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle(); Thread.sleep(500)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            capture(edit, File(out, "03.png"))
            ScanSession.clear()

            // 4 OCR text
            val firstPdf = File(root, s.docs[0] + ".pdf")
            val ocr = Robolectric.buildActivity(OcrActivity::class.java,
                Intent(ctx, OcrActivity::class.java).putExtra(OcrActivity.EXTRA_PDF_PATH, firstPdf.absolutePath)).setup().get()
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            capture(ocr, File(out, "04.png"))

            // 5 signature pad, 6 placement
            val sign = Robolectric.buildActivity(SignActivity::class.java,
                Intent(ctx, SignActivity::class.java).putExtra(SignActivity.EXTRA_PDF, firstPdf.absolutePath)).setup().get()
            val pad = field(sign, "pad") as com.nibras.scan.tools.SignaturePadView
            pad.layout(0, 0, 1000, 900)
            fun ev(a: Int, x: Float, y: Float) = android.view.MotionEvent.obtain(0, 0, a, x, y, 0)
            pad.onTouchEvent(ev(0, 180f, 520f))
            listOf(260f to 380f, 330f to 600f, 400f to 420f, 470f to 560f, 560f to 400f, 640f to 540f, 760f to 470f, 840f to 520f)
                .forEach { (x, y) -> pad.onTouchEvent(ev(2, x, y)) }
            pad.onTouchEvent(ev(1, 840f, 520f))
            capture(sign, File(out, "05.png"))
            val sig = pad.export()!!
            (field(sign, "drawPanel") as View).visibility = View.GONE
            (field(sign, "placePanel") as View).visibility = View.VISIBLE
            val pv = field(sign, "placement") as PlacementView
            pv.page = docImage(s); pv.signature = sig
            (field(sign, "tvPage") as android.widget.TextView).text = sign.getString(R.string.page_of, 1, 1)
            capture(sign, File(out, "06.png"))

            // 7 QR history
            QrHistory.clear(ctx)
            s.qr.reversed().forEachIndexed { i, v -> QrHistory.add(ctx, QrHistory.Entry(v, if (i == 1) "EAN-13" else "QR", 1793000000000L + i * 3_600_000L)) }
            val qr = Robolectric.buildActivity(QrHistoryActivity::class.java).setup().get()
            capture(qr, File(out, "07.png"))

            // 8 settings
            val st = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()
            capture(st, File(out, "08.png"))
        }
    }
}

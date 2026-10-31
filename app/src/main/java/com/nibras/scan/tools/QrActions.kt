package com.nibras.scan.tools

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.nibras.scan.R

object QrActions {
    fun show(a: Activity, value: String, onDismiss: () -> Unit = {}) {
        val b = AlertDialog.Builder(a)
            .setTitle(R.string.qr_result)
            .setMessage(value)
            .setPositiveButton(R.string.copy_text) { _, _ -> copy(a, value) }
            .setNeutralButton(R.string.share) { _, _ -> share(a, value) }
            .setOnDismissListener { onDismiss() }
        if (QrHistory.isUrl(value)) b.setNegativeButton(R.string.open_link) { _, _ -> open(a, value) }
        b.show()
    }

    fun copy(c: Context, v: String) {
        (c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("QR", v))
        Toast.makeText(c, R.string.text_copied, Toast.LENGTH_SHORT).show()
    }

    fun share(a: Activity, v: String) {
        a.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, v), a.getString(R.string.share)))
    }

    fun open(a: Activity, v: String) {
        val url = if (v.startsWith("www.", true)) "https://$v" else v
        try { a.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { Toast.makeText(a, R.string.no_browser_app, Toast.LENGTH_SHORT).show() }
    }
}

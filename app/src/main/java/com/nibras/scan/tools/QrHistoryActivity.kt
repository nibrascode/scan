package com.nibras.scan.tools

import android.os.Bundle
import android.text.format.DateFormat
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.nibras.scan.R
import com.nibras.scan.util.InsetsUtil

class QrHistoryActivity : AppCompatActivity() {
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val (root, header) = Ui.screen(this, getString(R.string.qr_history))
        header.addView(TextView(this).apply {
            text = getString(R.string.clear); setTextColor(ContextCompat.getColor(context, R.color.nibras_bright_blue))
            setPadding(Ui.dp(context, 12), Ui.dp(context, 8), 0, Ui.dp(context, 8))
            setOnClickListener {
                AlertDialog.Builder(this@QrHistoryActivity).setMessage(R.string.qr_clear_confirm)
                    .setPositiveButton(R.string.delete) { _, _ -> QrHistory.clear(this@QrHistoryActivity); render() }
                    .setNegativeButton(R.string.cancel, null).show()
            }
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, Ui.dp(context, 16)) }
        root.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        InsetsUtil.apply(root, header)
        render()
    }

    private fun render() {
        list.removeAllViews()
        val items = QrHistory.load(this)
        if (items.isEmpty()) {
            list.addView(TextView(this).apply {
                text = getString(R.string.qr_history_empty); gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(context, R.color.gray_medium)); setPadding(0, Ui.dp(context, 48), 0, 0)
            }, LinearLayout.LayoutParams(-1, -2))
            return
        }
        val m = Ui.dp(this, 14)
        for (e in items) {
            val card = Ui.card(this)
            card.addView(TextView(this).apply {
                text = e.value; maxLines = 3; textSize = 15f; textDirection = TextView.TEXT_DIRECTION_ANY_RTL
                setTextColor(ContextCompat.getColor(context, R.color.gray_dark))
            })
            card.addView(TextView(this).apply {
                text = e.format + " · " + DateFormat.getMediumDateFormat(context).format(e.time) + " " + DateFormat.getTimeFormat(context).format(e.time)
                textSize = 12f; setTextColor(ContextCompat.getColor(context, R.color.gray_medium))
            })
            card.setOnClickListener { QrActions.show(this, e.value) }
            card.setOnLongClickListener {
                AlertDialog.Builder(this).setMessage(R.string.delete).setPositiveButton(R.string.delete) { _, _ ->
                    QrHistory.remove(this, e); render()
                }.setNegativeButton(R.string.cancel, null).show(); true
            }
            list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { setMargins(m, m / 2 + 2, m, 0) })
        }
    }
}

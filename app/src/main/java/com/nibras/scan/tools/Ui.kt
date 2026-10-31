package com.nibras.scan.tools

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.nibras.scan.R

/** Small helpers so tool screens match the app's navy / blue design. */
object Ui {
    fun dp(a: android.content.Context, v: Int) = (v * a.resources.displayMetrics.density).toInt()

    /** Root column with a navy header (back button + title). Returns root and the header. */
    fun screen(a: androidx.activity.ComponentActivity, title: String): Pair<LinearLayout, LinearLayout> {
        val root = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(a, R.color.gray_light))
        }
        val header = LinearLayout(a).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(ContextCompat.getColor(a, R.color.nibras_dark_navy))
            setPadding(dp(a, 8), dp(a, 12), dp(a, 16), dp(a, 12))
        }
        val back = ImageButton(a).apply {
            setImageResource(R.drawable.ic_back)
            setBackgroundResource(android.R.color.transparent)
            contentDescription = a.getString(R.string.back)
            setOnClickListener { a.onBackPressedDispatcher.onBackPressed() }
            if (a.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) scaleX = -1f
        }
        header.addView(back, LinearLayout.LayoutParams(dp(a, 44), dp(a, 44)))
        header.addView(TextView(a).apply {
            text = title; setTextColor(Color.WHITE); textSize = 18f; setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(a, 8), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(header, LinearLayout.LayoutParams(-1, -2))
        return root to header
    }

    fun primaryButton(a: Activity, text: String, onClick: () -> Unit) = TextView(a).apply {
        this.text = text; gravity = Gravity.CENTER; setTextColor(Color.WHITE); textSize = 15f
        setTypeface(typeface, Typeface.BOLD)
        setBackgroundResource(R.drawable.bg_button_primary)
        setPadding(dp(a, 16), 0, dp(a, 16), 0)
        isClickable = true; isFocusable = true
        setOnClickListener { onClick() }
    }

    fun secondaryButton(a: Activity, text: String, onClick: () -> Unit) = TextView(a).apply {
        this.text = text; gravity = Gravity.CENTER; textSize = 14f
        setTextColor(ContextCompat.getColor(a, R.color.nibras_deep_blue))
        setTypeface(typeface, Typeface.BOLD)
        background = GradientDrawable().apply {
            cornerRadius = dp(a, 24).toFloat(); setColor(Color.WHITE)
            setStroke(dp(a, 1), ContextCompat.getColor(a, R.color.nibras_bright_blue))
        }
        setPadding(dp(a, 14), 0, dp(a, 14), 0)
        isClickable = true; isFocusable = true
        setOnClickListener { onClick() }
    }

    fun card(a: Activity) = LinearLayout(a).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bg_card)
        setPadding(dp(a, 14), dp(a, 14), dp(a, 14), dp(a, 14))
    }

    fun lp(w: Int, h: Int, weight: Float = 0f, margin: Int = 0) =
        LinearLayout.LayoutParams(w, h, weight).apply { setMargins(margin, margin, margin, margin) }
}

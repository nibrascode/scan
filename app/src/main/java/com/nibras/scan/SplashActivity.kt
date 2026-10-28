package com.nibras.scan

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.nibras.scan.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_DURATION_MS = 2500L
        private const val PREFS = "nibras_privacy"
        private const val KEY_ACCEPTED = "privacy_accepted_v1"
    }

    private lateinit var binding: ActivitySplashBinding
    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) {
                if (isAccepted()) openMain() else showPrivacyDialog()
            }
        }, SPLASH_DURATION_MS)
    }

    override fun onResume() {
        super.onResume()
        // Returning from PrivacyPolicyActivity: re-show the consent dialog.
        if (dialog != null && dialog?.isShowing != true && !isAccepted()) showPrivacyDialog()
    }

    private fun isAccepted() =
        getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ACCEPTED, false)

    private fun showPrivacyDialog() {
        if (isFinishing) return
        dialog?.dismiss()
        val d = AlertDialog.Builder(this)
            .setTitle(R.string.privacy_dialog_title)
            .setMessage(R.string.privacy_dialog_message)
            .setCancelable(false)
            .setPositiveButton(R.string.privacy_dialog_agree, null)
            .setNeutralButton(R.string.privacy_dialog_read, null)
            .setNegativeButton(R.string.privacy_dialog_disagree, null)
            .create()
        d.setCanceledOnTouchOutside(false)
        d.setOnShowListener {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ACCEPTED, true).apply()
                d.dismiss()
                openMain()
            }
            d.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                startActivity(Intent(this, PrivacyPolicyActivity::class.java))
            }
            d.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                d.dismiss()
                finishAffinity()
            }
        }
        dialog = d
        d.show()
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

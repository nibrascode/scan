package com.nibras.scan

import android.app.Application
import com.nibras.scan.repo.ScanSession
import com.nibras.scan.util.DocumentDetector
import org.opencv.android.OpenCVLoader

class NibrasScanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DocumentDetector.openCvReady = try {
            OpenCVLoader.initDebug()
        } catch (_: Throwable) {
            false
        }
        ScanSession.restore(this)
    }
}

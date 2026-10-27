package com.nibras.scan.util

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Shared pool so page previews never decode or filter on the UI thread. */
object RenderJobs {
    val executor: ExecutorService = Executors.newFixedThreadPool(2)
}

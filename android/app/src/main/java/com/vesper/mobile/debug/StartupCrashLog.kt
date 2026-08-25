package com.vesper.mobile.debug

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes the last uncaught exception to app files so a physical-device
 * crash can be recovered without pretending the process is healthy.
 * The previous handler is always invoked afterwards.
 */
object StartupCrashLog {
    private const val FILE = "last-startup-crash.txt"
    private const val TAG = "VesperCrash"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            write(app, error, "uncaught on ${thread.name}")
            previous?.uncaughtException(thread, error)
        }
    }

    fun write(context: Context, error: Throwable, note: String = "") {
        val body = buildString {
            appendLine("VESPER MOBILE CRASH LOG")
            appendLine(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(Date()))
            if (note.isNotBlank()) appendLine(note)
            appendLine(error.toString())
            val sw = StringWriter()
            error.printStackTrace(PrintWriter(sw))
            append(sw.toString())
        }
        Log.e(TAG, body)
        runCatching {
            File(context.applicationContext.filesDir, FILE).writeText(body)
        }
    }

    fun read(context: Context): String? {
        val file = File(context.applicationContext.filesDir, FILE)
        if (!file.exists()) return null
        return runCatching { file.readText() }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    fun clear(context: Context) {
        runCatching { File(context.applicationContext.filesDir, FILE).delete() }
    }
}

package com.musheer360.swiftslate.service

import android.content.Context
import com.musheer360.swiftslate.model.PrefKeys
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Opt-in diagnostic log, shown/copied from Settings. Lines come from both sides of the bridge:
 * the keyboard hook ("kb", sent over MSG_LOG) and this app ("app"). Only written while the
 * "Debug log" switch is on. The keyboard side logs just the last few characters around a
 * possible command (non-ASCII shown as \uXXXX), never whole fields. Capped in size.
 */
object DebugLog {
    private const val FILE = "debug_log.txt"
    private const val MAX_BYTES = 96 * 1024
    private val lock = Any()
    private val fmt = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    fun enabled(context: Context): Boolean = try {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getBoolean(PrefKeys.DEBUG_LOG, false)
    } catch (_: Exception) { false }

    fun log(context: Context, source: String, message: String) {
        if (!enabled(context)) return
        try {
            synchronized(lock) {
                val f = File(context.filesDir, FILE)
                if (f.length() > MAX_BYTES) {
                    val keep = f.readText().takeLast(MAX_BYTES / 2)
                    f.writeText(keep.substringAfter('\n', keep))
                }
                f.appendText("${fmt.format(Date())} [$source] $message\n")
            }
        } catch (_: Exception) { /* diagnostics must never break the real work */ }
    }

    /** Escapes everything but printable ASCII so the log is safe to paste anywhere. */
    fun esc(s: String?): String {
        if (s == null) return "null"
        val sb = StringBuilder()
        for (c in s) {
            when {
                c == '\n' -> sb.append("\\n")
                c.code in 32..126 -> sb.append(c)
                else -> sb.append("\\u").append(String.format("%04x", c.code))
            }
        }
        return sb.toString()
    }

    fun tail(s: String, n: Int = 16): String = esc(s.takeLast(n))

    fun read(context: Context): String = try {
        synchronized(lock) {
            val f = File(context.filesDir, FILE)
            if (f.exists()) f.readText() else ""
        }
    } catch (_: Exception) { "" }

    fun clear(context: Context) {
        try { synchronized(lock) { File(context.filesDir, FILE).delete() } } catch (_: Exception) {}
    }
}

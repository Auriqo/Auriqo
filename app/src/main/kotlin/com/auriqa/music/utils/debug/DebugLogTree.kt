package com.auriqo.music.utils.debug

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DebugLogTree : Timber.Tree() {

    data class LogEntry(
        val timestamp: Long = System.currentTimeMillis(),
        val level: Int,
        val tag: String?,
        val message: String,
        val throwable: Throwable? = null
    ) {
        val levelStr: String get() = when (level) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "WTF"
            else -> "?"
        }

        val formattedTime: String get() {
            val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

        val fullMessage: String get() = buildString {
            append(message)
            throwable?.let {
                append("\n")
                append(it.stackTraceToString())
            }
        }
    }

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val buffer = CopyOnWriteArrayList<LogEntry>()
    private val maxEntries = 500

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val entry = LogEntry(
            level = priority,
            tag = tag,
            message = message,
            throwable = t
        )
        buffer.add(entry)
        if (buffer.size > maxEntries) {
            buffer.removeAt(0)
        }
        _logs.value = buffer.toList()
    }

    fun clear() {
        buffer.clear()
        _logs.value = emptyList()
    }

    fun getFilteredLogs(minLevel: Int = Log.DEBUG): List<LogEntry> {
        return buffer.filter { it.level >= minLevel }
    }

    companion object {
        private var instance: DebugLogTree? = null

        fun install(): DebugLogTree {
            val tree = DebugLogTree()
            Timber.plant(tree)
            instance = tree
            return tree
        }

        fun getInstance(): DebugLogTree? = instance
    }
}

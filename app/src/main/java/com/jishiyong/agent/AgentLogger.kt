package com.jishiyong.agent

import android.util.Log

interface AgentLogger {
    fun warn(message: String, throwable: Throwable? = null)
}

object NoOpAgentLogger : AgentLogger {
    override fun warn(message: String, throwable: Throwable?) = Unit
}

class AndroidAgentLogger(
    private val tag: String = DEFAULT_TAG
) : AgentLogger {
    override fun warn(message: String, throwable: Throwable?) {
        if (throwable == null) {
            Log.w(tag, message)
        } else {
            Log.w(tag, "$message (${throwable.safeLogSummary()})")
        }
    }

    private companion object {
        private const val DEFAULT_TAG = "InventoryAgent"
    }
}

private fun Throwable.safeLogSummary(): String {
    val type = this::class.java.simpleName.ifBlank { "Exception" }
    return "$type: ${message.safeForAgentLog()}"
}

private fun String?.safeForAgentLog(): String {
    val text = this?.lineSequence()?.firstOrNull()?.trim().orEmpty()
    if (text.isBlank()) return "no detail"
    val redacted = text
        .replace(Regex("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]+")) { "${it.groupValues[1]}[redacted]" }
        .replace(Regex("(?i)(api[_-]?key[\"'\\s:=]+)[^\\s,\"'}]+")) { "${it.groupValues[1]}[redacted]" }
        .replace(Regex("(?i)(authorization[\"'\\s:=]+)[^\\s,\"'}]+")) { "${it.groupValues[1]}[redacted]" }
    return redacted.take(MAX_SAFE_LOG_MESSAGE_LENGTH)
}

private const val MAX_SAFE_LOG_MESSAGE_LENGTH = 160

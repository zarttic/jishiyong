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
            Log.w(tag, message, throwable)
        }
    }

    private companion object {
        private const val DEFAULT_TAG = "InventoryAgent"
    }
}

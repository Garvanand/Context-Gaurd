package com.contextguard.app.core.logging

import android.util.Log

object AppLogger {
    private const val DEFAULT_TAG = "ContextGuard"

    fun d(message: String, tag: String = DEFAULT_TAG) {
        Log.d(tag, message)
    }

    fun i(message: String, tag: String = DEFAULT_TAG) {
        Log.i(tag, message)
    }

    fun w(message: String, tag: String = DEFAULT_TAG) {
        Log.w(tag, message)
    }

    fun e(message: String, throwable: Throwable? = null, tag: String = DEFAULT_TAG) {
        Log.e(tag, message, throwable)
    }

    fun audit(event: String, artifactHash: String? = null, latencyMs: Long? = null) {
        val details = buildString {
            append("AUDIT: ").append(event)
            artifactHash?.let { append(" | hash=").append(it.take(12)).append("...") }
            latencyMs?.let { append(" | latency=").append(it).append("ms") }
        }
        Log.i("ContextGuardAudit", details)
    }
}

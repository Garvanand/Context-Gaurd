package com.contextguard.app.core.state

/**
 * Generic state representation for loading, error, and content UI lifecycle.
 */
sealed interface UiState<out T> {
    object Idle : UiState<Nothing>

    data class Loading(
        val message: String = "Evaluating pre-action risk..."
    ) : UiState<Nothing>

    data class Success<out T>(
        val data: T
    ) : UiState<T>

    data class Error(
        val message: String,
        val canRetry: Boolean = true,
        val cause: Throwable? = null
    ) : UiState<Nothing>
}

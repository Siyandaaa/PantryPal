package com.motivation.pantrypal.util

/**
 * Small sealed wrapper so repositories can surface success / loading / error
 * states to ViewModels without leaking Retrofit or Room exceptions upward.
 */
sealed class PantryResult<out T> {
    data class Success<T>(val data: T) : PantryResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : PantryResult<Nothing>()
    data object Loading : PantryResult<Nothing>()
}

/** Runs [block], converting any thrown exception into [PantryResult.Error]. */
suspend fun <T> safeApiCall(tag: String, block: suspend () -> T): PantryResult<T> {
    return try {
        PantryResult.Success(block())
    } catch (e: Exception) {
        android.util.Log.e(tag, "safeApiCall failed: ${e.message}", e)
        PantryResult.Error(e.message ?: "Unknown error", e)
    }
}

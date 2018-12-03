package de.spover.spover.network

/**
 * Wrapper class that serves as a union of a result value and an exception. When the download
 * task has completed, either the result value or exception can be a non-null value.
 * This allows you to pass exceptions to the UI thread that were thrown during doInBackground().
 */
class NetworkResult {
    var resultValue: String? = null
    var exception: Exception? = null

    constructor(resultValue: String) {
        this.resultValue = resultValue
    }

    constructor(exception: Exception) {
        this.exception = exception
    }
}

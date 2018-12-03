package de.spover.spover.network

import android.net.ConnectivityManager
import android.os.AsyncTask
import java.io.*
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class DownloadTask(private val callback: DownloadCallback) : AsyncTask<String, Int, NetworkResult>() {

    /**
     * Cancel background network operation if we do not have network connectivity.
     */
    override fun onPreExecute() {
        val networkInfo = callback.getNetworkInfo()
        if (!networkInfo.isConnected || (networkInfo.type != ConnectivityManager.TYPE_WIFI
                        && networkInfo.type != ConnectivityManager.TYPE_MOBILE)) {
            // If no connectivity, cancel task and update Callback with null data.
            callback.finished(null)
            cancel(true)
        }
    }

    /**
     * Defines work to perform on the background thread.
     */
    override fun doInBackground(vararg urls: String): NetworkResult? {
        if (isCancelled) {
            return NetworkResult(IOException("Download has been cancelled"))
        }
        if (urls.isEmpty()) {
            return NetworkResult(IOException("No url provided"))
        }

        return try {
            val urlString = urls[0]
            val url = URL(urlString)
            val resultString = downloadUrl(url)
            if (resultString != null) {
                NetworkResult(resultString)
            } else {
                throw IOException("No response received.")
            }
        } catch (e: Exception) {
            NetworkResult(e)
        }
    }

    /**
     * Updates the DownloadCallback with the result.
     */
    override fun onPostExecute(result: NetworkResult?) {
        callback.apply {
            result?.exception?.also {
                finished(it.message)
                return
            }
            result?.resultValue?.also {
                finished(it)
                return
            }
        }
    }

    /**
     * Override to add special behavior for cancelled AsyncTask.
     */
    override fun onCancelled(result: NetworkResult) {
        super.onCancelled(result)
    }

    /**
     * Given a URL, sets up a connection and gets the HTTP response body from the server.
     * If the network request is successful, it returns the response body in String form. Otherwise,
     * it will throw an IOException.
     */
    @Throws(IOException::class)
    private fun downloadUrl(url: URL): String? {
        var connection: HttpsURLConnection? = null
        return try {
            connection = (url.openConnection() as? HttpsURLConnection)
            connection?.run {
                // Timeout for reading InputStream arbitrarily set to 3000ms.
                readTimeout = 3000
                // Timeout for connection.connect() arbitrarily set to 3000ms.
                connectTimeout = 3000
                // For this use case, set HTTP method to GET.
                requestMethod = "GET"
                // Already true by default but setting just in case; needs to be true since this request
                // is carrying an input (response) body.
                doInput = true
                // Open communications link (network traffic occurs here).
                connect()
                publishProgress(NetworkResultStatus.CONNECT_SUCCESS.ordinal)

                if (responseCode != HttpsURLConnection.HTTP_OK) {
                    throw IOException("HTTP error code: $responseCode")
                }

                // Retrieve the response body as an InputStream.
                publishProgress(NetworkResultStatus.GET_INPUT_STREAM_SUCCESS.ordinal, 0)
                inputStream?.let { stream ->
                    // Converts Stream to String with max length of 500.
                    readStream(stream, 500)
                }
            }
        } finally {
            // Close Stream and disconnect HTTPS connection.
            connection?.inputStream?.close()
            connection?.disconnect()
        }
    }

    /**
     * Converts the contents of an InputStream to a String.
     */
    @Throws(IOException::class, UnsupportedEncodingException::class)
    fun readStream(stream: InputStream, maxReadSize: Int): String? {
        val reader: Reader? = InputStreamReader(stream, "UTF-8")
        val rawBuffer = CharArray(maxReadSize)
        val buffer = StringBuffer()
        var readSize: Int = reader?.read(rawBuffer) ?: -1
        var maxReadBytes = maxReadSize
        while (readSize != -1 && maxReadBytes > 0) {
            if (readSize > maxReadBytes) {
                readSize = maxReadBytes
            }
            buffer.append(rawBuffer, 0, readSize)
            maxReadBytes -= readSize
            readSize = reader?.read(rawBuffer) ?: -1
        }
        return buffer.toString()
    }
}
package de.spover.spover.network

import android.net.NetworkInfo

interface DownloadCallback {

    /**
     * This needs to provide information about the network, so that the NetworkFragment knows
     * whether we have network access or not
     */
    fun getNetworkInfo(): NetworkInfo

    /**
     * Called every time a progress update is available
     */
    fun onProgressUpdate(progressCode: Int, percentComplete: Int)

    /**
     * Called when the download is finished
     */
    fun finished(result: String?)

}

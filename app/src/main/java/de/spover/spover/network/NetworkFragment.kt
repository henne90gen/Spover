package de.spover.spover.network

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager

class NetworkFragment : Fragment() {
    private var downloadTask: DownloadTask? = null

    companion object {
        private const val TAG = "NetworkFragment"

        fun getInstance(fragmentManager: FragmentManager): NetworkFragment {
            val networkFragment = NetworkFragment()
            fragmentManager.beginTransaction().add(networkFragment, TAG).commit()
            return networkFragment
        }
    }

    fun startDownload() {
        cancelDownload()
//        callback?.also { callback ->
//            downloadTask = DownloadTask(callback).apply {
//                execute(url)
//            }
//        }
    }

    /**
     * Cancel (and interrupt if necessary) any ongoing DownloadTask execution.
     */
    fun cancelDownload() {
        downloadTask?.cancel(true)
    }
}

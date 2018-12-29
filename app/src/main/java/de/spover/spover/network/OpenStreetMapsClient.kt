package de.spover.spover.network

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import android.util.Log
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.spover.spover.BoundingBox
import de.spover.spover.database.*
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class OpenStreetMapsClient : JobService() {

    companion object {
        private const val BASE_URL = "https://overpass-api.de/api/"
        private val TAG = OpenStreetMapsClient::class.java.simpleName
        private const val JOB_ID = 1

        private val xmlMapper = XmlMapper().registerKotlinModule()

        fun scheduleBoundingBoxFetching(context: Context, boundingBox: BoundingBox) {
            val jobScheduler = context
                    .getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            val componentName = ComponentName(context,
                    OpenStreetMapsClient::class.java)

            val bundle = convert(boundingBox)
            val jobInfo = JobInfo.Builder(OpenStreetMapsClient.JOB_ID, componentName)
                    .setExtras(bundle)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .build()
            jobScheduler.schedule(jobInfo)
        }

        private fun convert(boundingBox: BoundingBox): PersistableBundle {
            val bundle = PersistableBundle()
            bundle.putDouble("minLat", boundingBox.minLat)
            bundle.putDouble("minLon", boundingBox.minLon)
            bundle.putDouble("maxLat", boundingBox.maxLat)
            bundle.putDouble("maxLon", boundingBox.maxLon)
            return bundle
        }

        fun convert(bundle: PersistableBundle): BoundingBox {
            return BoundingBox(
                    minLat = bundle.getDouble("minLat"),
                    minLon = bundle.getDouble("minLon"),
                    maxLat = bundle.getDouble("maxLat"),
                    maxLon = bundle.getDouble("maxLon")
            )
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        if (params?.extras == null) {
            Log.e(TAG, "Could not read bounding box information from job parameters")
            return false
        }
        val boundingBox = convert(params.extras)
        val thread = Thread {
            run(this, boundingBox)
        }
        thread.start()
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.e(TAG, "onStopJob")
        return false
    }

    private fun createUrl(boundingBox: BoundingBox): URL {
        return URL("${BASE_URL}xapi?*[maxspeed=*][bbox=${boundingBox.minLon},${boundingBox.minLat},${boundingBox.maxLon},${boundingBox.maxLat}]")
    }

    private fun run(context: Context, boundingBox: BoundingBox) {
        val url = createUrl(boundingBox)
        val downloadResult = download(url)
        val osm = xmlMapper.readValue<Osm>(downloadResult, Osm::class.java)
        val db = AppDatabase.createBuilder(context).fallbackToDestructiveMigration().build()

        OsmPersistenceHelper().persistOsmXmlResult(db, osm, boundingBox)
    }

    /**
     * Given a URL, sets up a connection and gets the HTTP response body from the server.
     * If the network request is successful, it returns the response body in String form. Otherwise,
     * it will throw an IOException.
     */
    @Throws(IOException::class)
    private fun download(url: URL): String? {
        var connection: HttpsURLConnection? = null
        return try {
            connection = (url.openConnection() as? HttpsURLConnection)
            connection?.run {
                // Timeout for reading InputStream arbitrarily set to 3000ms.
                readTimeout = 30000
                // Timeout for connection.connect() arbitrarily set to 3000ms.
                connectTimeout = 30000
                // For this use case, set HTTP method to GET.
                requestMethod = "GET"
                // Already true by default but setting just in case; needs to be true since this request
                // is carrying an input (response) body.
                doInput = true
                // Open communications link (network traffic occurs here).
                connect()

                if (responseCode != HttpsURLConnection.HTTP_OK) {
                    throw IOException("HTTP error code: $responseCode")
                }

                // Retrieve the response body as an InputStream.
                inputStream?.let { stream ->
                    readStream(stream)
                }
            }
        } finally {
            // Close Stream and disconnect HTTPS connection.
            connection?.inputStream?.close()
            connection?.disconnect()

        }
    }

    @Throws(IOException::class, UnsupportedEncodingException::class)
    fun readStream(stream: InputStream): String? {
        val bufferSize = 1024
        val buffer = CharArray(bufferSize)
        val out = StringBuilder()
        val streamReader = InputStreamReader(stream, "UTF-8")
        while (true) {
            val rsz = streamReader.read(buffer, 0, buffer.size)
            if (rsz < 0) {
                break
            }
            out.append(buffer, 0, rsz)
        }
        return out.toString()
    }
}
package de.spover.spover

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.support.v4.content.ContextCompat.getSystemService
import de.spover.spover.network.OpenStreetMapsClient

fun scheduleOSMClient(context: Context) {
    val osmClientComponent = ComponentName(context, OpenStreetMapsClient::class.java)
    // DO NOT USE PERIODIC! It is broken from Android N onwards
    val builder = JobInfo.Builder(MainActivity.OSM_CLIENT_ID, osmClientComponent)
            .setMinimumLatency((1 * 1000).toLong())
    val jobScheduler = getSystemService(context, JobScheduler::class.java)
    jobScheduler?.also {
        it.schedule(builder.build())
    }
}

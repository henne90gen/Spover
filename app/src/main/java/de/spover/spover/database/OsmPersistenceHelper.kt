package de.spover.spover.database

import android.util.Log
import de.spover.spover.network.BoundingBox
import de.spover.spover.network.Osm
import java.time.LocalDateTime

class OsmPersistenceHelper {

    companion object {
        val TAG = OsmPersistenceHelper::class.simpleName
    }

    fun persistOsmXmlResult(db: AppDatabase, osm: Osm, boundingBox: BoundingBox) {
        val startTime = System.currentTimeMillis()
        Log.i(TAG, "Writing ways to database.")
        Log.i(TAG, "Found ${osm.nodes.size} nodes and ${osm.ways.size} ways")

        val request = Request(
                maxLat = boundingBox.maxLat,
                maxLon = boundingBox.maxLon,
                minLat = boundingBox.minLat,
                minLon = boundingBox.minLon,
                creationTime = LocalDateTime.now()
        )
        request.id = db.requestDao().insert(request)

        val nodes = osm.nodes.map {
            val latitude = it.lat.toDouble()
            val longitude = it.lon.toDouble()
            val osmIdentifier = it.id
            osmIdentifier to Node(-1, latitude, longitude, osmIdentifier)
        }.toMap()

        osm.ways.forEach {
            var maxSpeed = ""
            var maxSpeedSource = ""
            it.tags.forEach { tag ->
                when (tag.k) {
                    "maxspeed" -> maxSpeed = tag.v
                    "maxspeedsource" -> maxSpeedSource = tag.v
                }
            }
            val way = Way(request.id!!, maxSpeed, maxSpeedSource)
            way.id = db.wayDao().insert(way)
            it.nodeRefs.forEach { nodeRef ->
                val node = nodes[nodeRef.ref] ?: return
                node.wayId = way.id!!
                db.nodeDao().insert(node)
            }
        }
        val endTime = System.currentTimeMillis()
        val diff = endTime - startTime
        Log.i(TAG, "Writing ways to database. Done. ($diff ms)")
    }
}
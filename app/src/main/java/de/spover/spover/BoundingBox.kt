package de.spover.spover

import android.location.Location

class BoundingBox(
        val minLat: Double,
        val minLon: Double,
        val maxLat: Double,
        val maxLon: Double
) {
    companion object {
        fun createBoundingBox(location: Location, distance: Int): BoundingBox {
            val minLoc = translateLocationByMeters(location, -distance, -distance)
            val maxLoc = translateLocationByMeters(location, distance, distance)
            return BoundingBox(minLoc.latitude, minLoc.longitude, maxLoc.latitude, maxLoc.longitude)
        }

        // https://gis.stackexchange.com/questions/2951/algorithm-for-offsetting-a-latitude-longitude-by-some-amount-of-meters
        fun translateLocationByMeters(location: Location, transX: Int, transY: Int): Location {
            val result = Location("")
            result.latitude = location.latitude + (transY / 111111.0f)
            result.longitude = location.longitude + (transX / (111111.0f * Math.cos(Math.toRadians(location.latitude))))
            return result
        }
    }

    fun isBoundingBoxValid(location: Location, minDistFromBBEdge: Int): Boolean {
        val tmpLocation = Location("")

        // top edge
        tmpLocation.latitude = maxLat
        tmpLocation.longitude = location.longitude
        if (location.distanceTo(tmpLocation) < minDistFromBBEdge || location.latitude > maxLat) return false
        // right edge
        tmpLocation.latitude = location.latitude
        tmpLocation.longitude = maxLon
        if (location.distanceTo(tmpLocation) < minDistFromBBEdge || location.longitude > maxLon) return false
        // bottom edge
        tmpLocation.latitude = minLat
        tmpLocation.longitude = location.longitude
        if (location.distanceTo(tmpLocation) < minDistFromBBEdge || location.latitude < minLat) return false
        // left edge
        tmpLocation.latitude = location.latitude
        tmpLocation.longitude = minLon
        if (location.distanceTo(tmpLocation) < minDistFromBBEdge || location.longitude < minLon) return false

        return true
    }

    override fun toString(): String {
        return "minLat: $minLat, minLon: $minLon, maxLat: $maxLat, maxLon: $maxLon"
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (other !is BoundingBox) {
            return false
        }
        return minLat == other.minLat
                && minLon == other.minLon
                && maxLat == other.maxLat
                && maxLon == other.maxLon
    }

    fun contains(location: Location): Boolean {
        return isBoundingBoxValid(location, 0)
    }
}

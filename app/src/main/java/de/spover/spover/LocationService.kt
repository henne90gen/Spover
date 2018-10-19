package de.spover.spover

class LocationService : ILocationService {
    override fun fetchLocation(): Location {
        return Location(12, 13)
    }

}

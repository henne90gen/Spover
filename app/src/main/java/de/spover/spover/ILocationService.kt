package de.spover.spover

typealias LocationCallback = (Location?) -> Unit

interface ILocationService {
    fun fetchLocation(callback: LocationCallback)
}

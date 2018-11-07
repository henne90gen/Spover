package de.spover.spover

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class LocationServiceTest {

    @Test
    fun canFetchLocation() {
        val locationService = LocationService()
        locationService.fetchLocation()
    }
}

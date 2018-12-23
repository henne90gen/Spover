package de.spover.spover

import android.location.Location
import androidx.test.runner.AndroidJUnit4
import de.spover.spover.database.Node
import de.spover.spover.database.Way
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
open class FindSpeedLimitTest {

    companion object {
        val TAG = FindSpeedLimitTest::class.simpleName
    }

    private var wayMap: LinkedHashMap<Way, List<Node>> = linkedMapOf()
    private var lastLocation = Location("")
    private var location = Location("")

    @Before
    fun setupWays() {
        wayMap[Way(1, "42", "sign", "", 1)] = listOf(
                Node(1, 13.75557, 51.02651, "1"),
                Node(1, 13.75487, 51.02706, "2"))

        wayMap[Way(1, "24", "sign", "", 1)] = listOf(
                Node(1, 13.75487, 51.02706, "2"),
                Node(1, 13.75557, 51.02651, "1"))
    }

    @Test
    fun findsCorrectSpeedLimit() {
        lastLocation.latitude = 13.75557
        lastLocation.longitude = 51.02651

        location.latitude = 13.75487
        location.longitude = 51.02706

        assertEquals(Pair(42, "42"), SpeedLimitService.extractSpeedLimit(SpeedLimitService.getClosestWay(location, lastLocation, wayMap)))
        assertEquals(Pair(24, "24"), SpeedLimitService.extractSpeedLimit(SpeedLimitService.getClosestWay(lastLocation, location, wayMap)))
    }

    @Test
    fun doesNotFindWayBecauseWaysAreToFarAway() {
        lastLocation.latitude = 13.7797
        lastLocation.longitude = 51.0210

        location.latitude = 13.7797
        location.longitude = 51.0210

        assertEquals(null, SpeedLimitService.getClosestWay(location, lastLocation, wayMap))
    }
}
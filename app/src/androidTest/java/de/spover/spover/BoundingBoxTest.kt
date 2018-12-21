package de.spover.spover

import android.location.Location
import androidx.test.runner.AndroidJUnit4
import de.spover.spover.network.BoundingBox
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
open class BoundingBoxTest {
    companion object {
        val TAG = BoundingBoxTest::class.simpleName
    }

    private lateinit var boundingBox: BoundingBox
    private var minDistanceFromBBEdge: Int = 0
    private lateinit var location: Location

    @Before
    fun setupBoundingBox() {
        boundingBox =  BoundingBox(51.0261,13.7303, 51.0359,13.7553)
        minDistanceFromBBEdge = 200
        location = Location("")
    }

    @Test
    fun isValidBoundingBoxDetected() {
        location.latitude = 51.0324
        location.longitude = 13.7418

        assertTrue(boundingBox.isBoundingBoxValid(location, minDistanceFromBBEdge))
    }

    @Test
    fun isInvalidBoundingBoxDetectedWhereLocationStillInsideBB() {
        location.latitude = 51.0345
        location.longitude = 13.7529

        assertFalse(boundingBox.isBoundingBoxValid(location, minDistanceFromBBEdge))
    }

    @Test
    fun isInvalidBoundingBoxDetectedWhereLocationOutsideBB() {
        location.latitude = 51.0238
        location.longitude = 13.7611

        assertFalse(boundingBox.isBoundingBoxValid(location, minDistanceFromBBEdge))
    }

    // Todo add a test for being very close to lat and long zero point
}
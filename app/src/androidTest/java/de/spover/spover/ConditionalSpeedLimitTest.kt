package de.spover.spover

import androidx.test.runner.AndroidJUnit4
import junit.framework.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
open class ConditionalSpeedLimitTest {

    @Test
    fun isRecognizingTimeCondition() {
        var conditionalSpeedValue = "30 @ (06:00-20:00)"
        assertTrue(SpeedLimitService.isTimeCondition(conditionalSpeedValue))

        conditionalSpeedValue = "100@(22:00-06:30)"
        assertTrue(SpeedLimitService.isTimeCondition(conditionalSpeedValue))

        conditionalSpeedValue = "100 @ (06:00 - 20:00)"
        assertTrue(SpeedLimitService.isTimeCondition(conditionalSpeedValue))

        conditionalSpeedValue = "(06:00-20:00)"
        assertFalse(SpeedLimitService.isTimeCondition(conditionalSpeedValue))

        conditionalSpeedValue = "100"
        assertFalse(SpeedLimitService.isTimeCondition(conditionalSpeedValue))
    }

    @Test
    fun isRecognizingDayTimeCondition() {
        var conditionalSpeedValue = "30 @ (Mo-Fr 06:00-17:00)"
        assertTrue(SpeedLimitService.isDayTimeCondition(conditionalSpeedValue))

        conditionalSpeedValue = "30 @ (06:00-17:00)"
        assertFalse(SpeedLimitService.isDayTimeCondition(conditionalSpeedValue))
    }

    @Test
    fun isRecognizingWeatherCondition() {
        var conditionalSpeedValue = "30 @ wet"
        assertTrue(SpeedLimitService.isWeatherCondition(conditionalSpeedValue))

        conditionalSpeedValue = "30 @  "
        assertFalse(SpeedLimitService.isWeatherCondition(conditionalSpeedValue))
    }

    @Test
    fun isReturningSpeedLimitBasedOnTimeCondition() {
        // Todo would be nice to mock the system time without having to give it as parameter into
        // maybe that helps: https://stackoverflow.com/questions/2001671/override-java-system-currenttimemillis-for-testing-time-sensitive-code
        var conditionalSpeedValue = "30 @ (06:00-20:00)"
        assertEquals(30, SpeedLimitService.isTimeCondition(conditionalSpeedValue))
    }
}
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
        assertTrue(SpeedLimitExtractor.isTimeCondition(conditionalSpeedValue))

        conditionalSpeedValue = "100@(22:00-06:30)"
        assertTrue(SpeedLimitExtractor.isTimeCondition(conditionalSpeedValue))

        conditionalSpeedValue = "100 @ (06:00 - 20:00)"
        assertTrue(SpeedLimitExtractor.isTimeCondition(conditionalSpeedValue))

        conditionalSpeedValue = "(06:00-20:00)"
        assertFalse(SpeedLimitExtractor.isTimeCondition(conditionalSpeedValue))

        conditionalSpeedValue = "100"
        assertFalse(SpeedLimitExtractor.isTimeCondition(conditionalSpeedValue))
    }

    @Test
    fun isRecognizingDayTimeCondition() {
        var conditionalSpeedValue = "30 @ (Mo-Fr 06:00-17:00)"
        assertTrue(SpeedLimitExtractor.isDayTimeCondition(conditionalSpeedValue))

        conditionalSpeedValue = "30 @ (06:00-17:00)"
        assertFalse(SpeedLimitExtractor.isDayTimeCondition(conditionalSpeedValue))
    }

    @Test
    fun isRecognizingWeatherCondition() {
        var conditionalSpeedValue = "30 @ wet"
        assertTrue(SpeedLimitExtractor.isWeatherCondition(conditionalSpeedValue))

        conditionalSpeedValue = "30 @  "
        assertFalse(SpeedLimitExtractor.isWeatherCondition(conditionalSpeedValue))
    }

    @Test
    fun isRecognizingFulfilledTimeCondition() {
        // Todo would be nice to mock the system time without having to give it as parameter into
        // maybe that helps: https://stackoverflow.com/questions/2001671/override-java-system-currenttimemillis-for-testing-time-sensitive-code

        var condition = "30 @ (06:00-22:00)"
        assertTrue(SpeedLimitExtractor.isTimeConditionFulfilled(condition))

        condition = "30 @ (22:00-06:00)"
        assertFalse(SpeedLimitExtractor.isTimeConditionFulfilled(condition))
    }

    @Test
    fun extractsSpeedLimitFromCondition() {
        var condition = "30 @ (06:00-20:00)"
        assertEquals(30, SpeedLimitExtractor.extractSpeedLimitFromCondition(condition))

        condition = "120@(06:00-20:00)"
        assertEquals(120, SpeedLimitExtractor.extractSpeedLimitFromCondition(condition))

    }
}
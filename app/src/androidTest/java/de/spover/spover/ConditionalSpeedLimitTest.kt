package de.spover.spover

import androidx.test.runner.AndroidJUnit4
import de.spover.spover.speedlimit.SpeedLimitExtractor
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
open class ConditionalSpeedLimitTest {

    private lateinit var condition: String

    @Before
    fun setupCondition() {
        condition = ""
    }

    @Test
    fun isRecognizingTimeCondition() {
        condition = "30 @ (06:00-20:00)"
        assertTrue(SpeedLimitExtractor.isTimeCondition(condition))

        condition = "100@(22:00-06:30)"
        assertTrue(SpeedLimitExtractor.isTimeCondition(condition))

        condition = "100 @ (06:00 - 20:00)"
        assertTrue(SpeedLimitExtractor.isTimeCondition(condition))

        condition = "(06:00-20:00)"
        assertFalse(SpeedLimitExtractor.isTimeCondition(condition))

        condition = "100"
        assertFalse(SpeedLimitExtractor.isTimeCondition(condition))
    }

    @Test
    fun isRecognizingDayTimeCondition() {
        condition = "30 @ (Mo-Fr 06:00-17:00)"
        assertTrue(SpeedLimitExtractor.isDayTimeCondition(condition))

        condition = "30 @ (06:00-17:00)"
        assertFalse(SpeedLimitExtractor.isDayTimeCondition(condition))
    }

    @Test
    fun isRecognizingWeatherCondition() {
        condition = "30 @ wet"
        assertTrue(SpeedLimitExtractor.isWeatherCondition(condition))

        condition = "30 @  "
        assertFalse(SpeedLimitExtractor.isWeatherCondition(condition))
    }

    @Test
    fun isRecognizingFulfilledTimeCondition() {
        // Todo would be nice to mock the system time without having to give it as parameter into
        // maybe that helps: https://stackoverflow.com/questions/2001671/override-java-system-currenttimemillis-for-testing-time-sensitive-code

        condition = "30 @ (06:00-22:00)"
        assertTrue(SpeedLimitExtractor.isTimeConditionFulfilled(condition))

        condition = "30 @ (22:00-06:00)"
        assertFalse(SpeedLimitExtractor.isTimeConditionFulfilled(condition))
    }

    @Test
    fun isRecognizingFulfilledDateTimeCondition() {
        condition = "30 @ (Mo-Fr 06:00-22:00)"
        assertTrue(SpeedLimitExtractor.isDayTimeConditionFulfilled(condition))

        condition = "30 @ (Su-Mo 06:00-22:00)"
        assertFalse(SpeedLimitExtractor.isDayTimeConditionFulfilled(condition))
    }

    @Test
    fun extractsSpeedLimitFromCondition() {
        condition = "30 @ (06:00-20:00)"
        assertEquals(30, SpeedLimitExtractor.extractSpeedLimitNumberFromCondition(condition))

        condition = "120@(06:00-20:00)"
        assertEquals(120, SpeedLimitExtractor.extractSpeedLimitNumberFromCondition(condition))
    }
}
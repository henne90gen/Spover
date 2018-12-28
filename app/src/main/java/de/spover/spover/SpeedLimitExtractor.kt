package de.spover.spover

import android.location.Location
import android.util.Log
import de.spover.spover.database.Node
import de.spover.spover.database.Way
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class SpeedLimitExtractor {
    companion object {
        private var TAG = SpeedLimitExtractor::class.java.simpleName

        fun getClosestWay(location: Location, lastLocation: Location, wayMap: LinkedHashMap<Way, List<Node>>): Way? {
            if (wayMap.size == 0) return null

            var minDistance = Float.POSITIVE_INFINITY
            var result: Way? = null
            val nodeLocation = Location("")

            for ((way: Way, nodes: List<Node>) in wayMap) {
                var lastNodeLastLocDistance = Float.POSITIVE_INFINITY
                for (node: Node in nodes) {
                    nodeLocation.latitude = node.latitude
                    nodeLocation.longitude = node.longitude
                    val currNodeCurrLocDistance = location.distanceTo(nodeLocation)
                    if ((lastNodeLastLocDistance + currNodeCurrLocDistance) < minDistance) {
                        minDistance = lastNodeLastLocDistance + currNodeCurrLocDistance
                        result = way
                    }
                    lastNodeLastLocDistance = lastLocation.distanceTo(nodeLocation)
                }
            }

            // if the closest way is to far away we don't accept it
            if (minDistance / 2 > 500) {
                Log.e(TAG, "Closest way is to far away ${minDistance / 2}m")
                result = null
            }

            Log.d(TAG, "nearest way is around ${minDistance / 2}m away and has a speed limit of ${extractSpeedLimit(result)}km/h")
            return result
        }

        fun extractSpeedLimit(way: Way?): Pair<Int, String> {
            return when (way) {
                null -> {
                    Log.e(TAG, "current way is undefined, no speed limit available")
                    Pair(Int.MAX_VALUE, "no_w")
                }
                else -> {
                    Log.d(TAG, "conditional ${way.maxSpeedConditional}")

                    val conditionsMap = mapOf(
                            Pair({ condition: String -> isTimeCondition(condition) }, { condition: String -> isTimeConditionFulfilled(condition) }),
                            Pair({ condition: String -> isDayTimeCondition(condition) }, { condition: String -> isDayTimeConditionFulfilled(condition) }),
                            Pair({ condition: String -> isWeatherCondition(condition) }, { condition: String -> isWeatherConditionFulfilled(condition) })
                    )

                    for ((isType, isFulfilled) in conditionsMap) {
                        if (isType(way.maxSpeedConditional) && isFulfilled(way.maxSpeedConditional)) {
                            val speedLimit = extractSpeedLimitNumberFromCondition(way.maxSpeedConditional)
                            return Pair(speedLimit, speedLimit.toString())
                        }
                    }

                    // Default case with speed given as integer
                    val result = way.maxSpeed.toIntOrNull()
                    if (result != null) {
                        return Pair(result, result.toString())
                    }

                    // special speed tag where tag corresponds to a certain speed
                    val maxSpeedTags: HashMap<String, Pair<Int, String>> = hashMapOf("none" to Pair(Int.MAX_VALUE, "inf"), "walk" to Pair(5, "walk"))
                    if (way.maxSpeed in maxSpeedTags) {
                        return maxSpeedTags[way.maxSpeed]!!
                    }

                    Log.e(TAG, "couldn't parse ways max speed tag")
                    Pair(Int.MAX_VALUE, "c_p")
                }
            }
        }

        fun isTimeCondition(condition: String): Boolean {
            // should match conditional speed limits as: "30 @ (06:00-20:00)"
            return condition matches Regex("[0-9]+ ?@ ?[(][0-2][0-9]:[0-5][0-9] ?[-] ?[0-2][0-9]:[0-5][0-9][)]")
        }

        fun isDayTimeCondition(condition: String): Boolean {
            // should match conditional speed limits as: 30 @ (Mo-Fr 06:00-17:00)
            return condition matches Regex("[0-9]+ ?@ ?[(](Mo|Tu|We|Th|Fr|Sa|Su) ?- ?(Mo|Tu|We|Th|Fr|Sa|Su) ?[0-2][0-9]:[0-5][0-9] ?[-] ?[0-2][0-9]:[0-5][0-9][)]")
        }

        fun isWeatherCondition(condition: String): Boolean {
            // should match conditional speed limits as: 40 @ wet
            return condition matches Regex("[0-9]+ ?@ ?wet")
        }

        fun isTimeConditionFulfilled(condition: String): Boolean {
            val time1: Date
            val time2: Date

            val regex = Pattern.compile("[0-2][0-9]:[0-5][0-9]")
            val matcher = regex.matcher(condition)
            val simpleDateFormat = SimpleDateFormat("hh:mm")

            if (matcher.find()) {
                val timeString = matcher.group()
                time1 = simpleDateFormat.parse(timeString)
            } else return false
            if (matcher.find()) {
                val timeString = matcher.group()
                time2 = simpleDateFormat.parse(timeString)
            } else return false

            var currTime = Calendar.getInstance().time
            val currTimeStr = simpleDateFormat.format(currTime)
            currTime = simpleDateFormat.parse(currTimeStr)

            return (currTime.after(time1) && currTime.before(time2))
                    || (currTime.before(time1) && currTime.before(time2) && time1.time - currTime.time < time2.time - currTime.time)
                    || (currTime.after(time1) && currTime.after(time2) && currTime.time - time1.time > currTime.time - time2.time)
        }

        private fun isWeekdayConditionFulfilled(condition: String): Boolean {
            val weekDays = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
            val day1: Int
            val day2: Int
            val currDay: Int

            val regex = Pattern.compile("(Mo|Tu|We|Th|Fr|Sa|Su)")
            val matcher = regex.matcher(condition)

            if (matcher.find()) {
                day1 = weekDays.indexOf(matcher.group())
            } else return false
            if (matcher.find()) {
                day2 = weekDays.indexOf(matcher.group())
            } else return false

            val calendar = Calendar.getInstance()
            val currDayLongName = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
            currDay = weekDays.indexOf(currDayLongName.substring(0, 2))

            return (currDay in day1..day2)
                    || (currDay < day1 && currDay < day2 && day1 - currDay < day2 - currDay)
                    || (currDay > day1 && currDay > day2 && currDay - day1 > currDay - day2)
        }

        fun isDayTimeConditionFulfilled(condition: String): Boolean {
            return isTimeConditionFulfilled(condition) && isWeekdayConditionFulfilled(condition)
        }

        fun isWeatherConditionFulfilled(condition: String): Boolean {
            // no idea how we should know whether the street is wet...
            return false
        }

        fun extractSpeedLimitNumberFromCondition(condition: String): Int {
            var result = -1
            val regex = Pattern.compile("[0-9]+")
            val matcher = regex.matcher(condition)
            if (matcher.find()) {
                val subString = matcher.group().toIntOrNull()
                if (subString != null) result = subString
            }
            return result
        }
    }
}
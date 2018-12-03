package de.spover.spover.database

import androidx.room.*

@Entity
data class Node(
        @PrimaryKey
        var id: Int,

        var latitude: Double,
        var longitude: Double
)

@Entity
data class Way(
        @PrimaryKey
        var id: Int,

        var nodes: List<Node>,
        var maxSpeed: String,
        var maxSpeedSource: String
)

@Entity
data class Request(
        @PrimaryKey
        var id: Int,

        var ways: List<Way>,
        var maxLat: Int,
        var maxLon: Int,
        var minLat: Int,
        var minLon: Int
)

@Dao
interface WayDao {

    @Query("SELECT * FROM way")
    fun getAll(): List<Way>

    @Insert
    fun insertAll(vararg way: Way)

    @Delete
    fun delete(user: Way)
}

@Database(entities = arrayOf(Way::class), version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wayDao(): WayDao
}

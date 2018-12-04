package de.spover.spover.database

import androidx.room.*

@Entity
data class Node(
        @PrimaryKey
        var id: Int,
        var wayId: Int,

        var latitude: Double,
        var longitude: Double
)

@Entity
data class EmptyWay(
        @PrimaryKey
        var id: Int,

        var maxSpeed: String,
        var maxSpeedSource: String
)

class Way {
    @Embedded
    lateinit var emptyWay: EmptyWay

    @Relation(parentColumn = "id", entityColumn = "wayId", entity = Node::class)
    lateinit var nodes: List<Node>
}

@Entity
data class Request(
        @PrimaryKey
        var id: Int,

        var ways: List<EmptyWay>,
        var maxLat: Int,
        var maxLon: Int,
        var minLat: Int,
        var minLon: Int
)

@Dao
interface EmptyWayDao {

    @Query("SELECT * FROM EmptyWay")
    fun getAll(): List<EmptyWay>

//    @Insert
//    fun insertAll(vararg way: Way)
//
//    @Delete
//    fun delete(user: Way)
}

@Database(entities = arrayOf(EmptyWay::class), version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wayDao(): EmptyWayDao
}

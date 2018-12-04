package de.spover.spover.database

import android.content.Context
import androidx.room.*

@Entity
data class Node(
        @PrimaryKey
        var id: Int,

        var latitude: Double,
        var longitude: Double
)

@Entity(tableName = "way")
data class EmptyWay(
        @PrimaryKey
        var wayId: Int,

        var maxSpeed: String,
        var maxSpeedSource: String
)

class Way {
    @Embedded
    lateinit var emptyWay: EmptyWay

    @Relation(parentColumn = "wayId", entityColumn = "id", entity = Node::class)
    lateinit var nodes: List<Node>
}

@Entity
data class EmptyRequest(
        @PrimaryKey
        var requestId: Int,

        var maxLat: Int,
        var maxLon: Int,
        var minLat: Int,
        var minLon: Int
)

class Request {
    @Embedded
    lateinit var emptyRequest: EmptyRequest

    @Relation(parentColumn = "requestId", entityColumn = "wayId", entity = EmptyWay::class)
    lateinit var ways: List<EmptyWay>
}

@Dao
interface WayDao {

    @Query("SELECT * FROM way")
    fun getWays(): List<Way>

    @Insert
    fun insertAll(vararg way: EmptyWay)

    @Delete
    fun delete(way: EmptyWay)
}

@Database(entities = arrayOf(EmptyWay::class, Node::class), version = 1)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        private const val NAME = "spover"

        fun createBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
            return Room.databaseBuilder(context, AppDatabase::class.java, NAME)
        }
    }

    abstract fun wayDao(): WayDao
}

package de.spover.spover.database

import android.content.Context
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

@Entity(tableName = "node", foreignKeys = [
    ForeignKey(entity = Way::class, parentColumns = arrayOf("id"), childColumns = arrayOf("wayId"), onDelete = CASCADE)
])
data class Node(
        var wayId: Long,
        var latitude: Double,
        var longitude: Double,
        var osmIdentifier: String,

        @PrimaryKey(autoGenerate = true)
        var id: Long? = null
)

@Entity(tableName = "way", foreignKeys = [
    ForeignKey(entity = Request::class, parentColumns = arrayOf("id"), childColumns = arrayOf("requestId"), onDelete = CASCADE)
])
data class Way(
        var requestId: Long,
        var maxSpeed: String,
        var maxSpeedSource: String,
        var maxSpeedConditional: String,

        @PrimaryKey(autoGenerate = true)
        var id: Long? = null
)

@Entity(tableName = "request")
data class Request(
        var maxLat: Double,
        var maxLon: Double,
        var minLat: Double,
        var minLon: Double,

        var creationTime: LocalDateTime,

        @PrimaryKey(autoGenerate = true)
        var id: Long? = null
)

@Dao
interface NodeDao {

    @Insert
    fun insert(node: Node): Long

    @Insert
    fun insertAll(nodes: List<Node>)

    @Update
    fun update(node: Node)

    @Query("SELECT * from node WHERE wayId=:wayId")
    fun findNodesByWayId(wayId: Long): List<Node>
}

@Dao
interface WayDao {

    @Insert
    fun insert(way: Way): Long

    @Delete
    fun delete(way: Way)

    @Query("SELECT * FROM way")
    fun findAllWays(): List<Way>

    @Query("SELECT * FROM way WHERE requestId=:requestId")
    fun findWaysByRequestId(requestId: Long): List<Way>
}

@Dao
interface RequestDao {

    @Insert
    fun insert(request: Request): Long

    @Delete
    fun delete(request: Request)

    @Query("SELECT * FROM request")
    fun findAllRequests(): List<Request>

    @Query("SELECT * FROM request WHERE maxLat=:maxLat AND maxLon=:maxLon AND minLat=:minLat AND minLon=:minLon")
    fun findRequestByBoundingBox(minLon: Double, minLat: Double, maxLon: Double, maxLat: Double): Request
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it),
                    TimeZone.getDefault().toZoneId())
        }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.atZone(TimeZone.getDefault().toZoneId())?.toInstant()?.toEpochMilli()
    }
}

@Database(entities = [Request::class, Way::class, Node::class], version = 2)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        private const val NAME = "spover"

        fun createBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
            return Room.databaseBuilder(context, AppDatabase::class.java, NAME)
        }
    }

    abstract fun wayDao(): WayDao

    abstract fun requestDao(): RequestDao

    abstract fun nodeDao(): NodeDao
}

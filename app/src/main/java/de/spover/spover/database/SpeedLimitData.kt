package de.spover.spover.database

import androidx.room.*

@Entity
data class SpeedLimit(

        @PrimaryKey
        var id: Int,

        var latitude: Double,
        var longitude: Double,

        val allowedSpeed: Int
)

@Dao
interface UserDao {

    @Query("SELECT * FROM speedlimit")
    fun getAll(): List<SpeedLimit>

    @Query("SELECT * FROM speedlimit WHERE id IN (:speedLimitIds)")
    fun loadAllByIds(speedLimitIds: IntArray): List<SpeedLimit>

    @Insert
    fun insertAll(vararg speedlimits: SpeedLimit)

    @Delete
    fun delete(user: SpeedLimit)
}

@Database(entities = arrayOf(SpeedLimit::class), version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
}

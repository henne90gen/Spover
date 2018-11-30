//package de.spover.spover.database
//
//import androidx.room.*
//
//@Entity
//data class User(
//        @PrimaryKey var id: Int,
//        @ColumnInfo(name = "first_name") var firstName: String?,
//        @ColumnInfo(name = "last_name") var lastName: String?
//)
//
//@Dao
//interface UserDao {
//    @Query("SELECT * FROM user")
//    fun getAll(): List<User>
//
//    @Query("SELECT * FROM user WHERE id IN (:userIds)")
//    fun loadAllByIds(userIds: IntArray): List<User>
//
//    @Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
//            "last_name LIKE :last LIMIT 1")
//    fun findByName(first: String, last: String): User
//
//    @Insert
//    fun insertAll(vararg users: User)
//
//    @Delete
//    fun delete(user: User)
//}
//
//@Database(entities = arrayOf(User::class), version = 1)
//abstract class AppDatabase : RoomDatabase() {
//    abstract fun userDao(): UserDao
//}
//
//val db = Room.databaseBuilder(
//        this,
//        AppDatabase::class.java, "database-name"
//).build()

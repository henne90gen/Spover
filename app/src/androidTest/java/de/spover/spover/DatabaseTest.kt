package de.spover.spover

import android.content.Context
import android.support.test.runner.AndroidJUnit4
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.spover.spover.database.AppDatabase
import de.spover.spover.database.EmptyWay
import de.spover.spover.database.WayDao
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var wayDao: WayDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        wayDao = db.wayDao()

    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeWay() {
        val w = EmptyWay(123, "123", "sign")
        wayDao.insertAll(w)
        val ways = wayDao.getWays()
        assertEquals(1, ways.size)
    }
}
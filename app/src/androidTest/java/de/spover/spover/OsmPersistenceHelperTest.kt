import androidx.room.Room
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.spover.spover.database.AppDatabase
import de.spover.spover.database.OsmPersistenceHelper
import de.spover.spover.network.BoundingBox
import de.spover.spover.network.Osm
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
open class OsmPersistenceHelperTest {

    companion object {
        val TAG = OsmPersistenceHelperTest::class.simpleName
        private val xmlMapper = XmlMapper().registerKotlinModule()
    }

    private lateinit var db: AppDatabase

    @Before
    fun initDb() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getContext(),
                AppDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    private fun readTestOsm(): Osm {
        val resource = OsmPersistenceHelperTest::class.java.getResourceAsStream("osm.xml")
                ?: throw Exception("Could not read test xml file")
        return xmlMapper.readValue<Osm>(resource, Osm::class.java)
    }

    @Test
    fun canInsertRequest() {
        val osm = readTestOsm()
        val boundingBox = BoundingBox(0.0, 1.0, 2.0, 3.0)
        OsmPersistenceHelper().persistOsmXmlResult(db, osm, boundingBox)

        val requests = db.requestDao().findAllRequests()
        assertEquals(1, requests.size)

        val request = requests[0]
        assertEquals(0.0, request.minLat, 0.0)
        assertEquals(1.0, request.minLon, 0.0)
        assertEquals(2.0, request.maxLat, 0.0)
        assertEquals(3.0, request.maxLon, 0.0)

        val allWays = db.wayDao().findAllWays()
        val requestWays = db.wayDao().findWaysByRequestId(request.id!!)

        assertEquals(277, allWays.size)
        assertEquals(277, requestWays.size)

        val way = requestWays[0]
        assertEquals("30", way.maxSpeed)
        val nodes = db.nodeDao().findNodesByWayId(way.id!!)
        assertEquals(11, nodes.size)

        val osmIdentifiers = listOf<String>("442734", "442735", "308149691", "442736", "4665821726", "442737", "4665821727", "442738", "442739", "2429178447", "442740")
        nodes.forEach {
            assertTrue(osmIdentifiers.contains(it.osmIdentifier))
        }
    }
}

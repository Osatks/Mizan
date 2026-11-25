package com.osatks.mizan

import com.osatks.mizan.core.Mosques
import com.osatks.mizan.i18n.En
import com.osatks.mizan.i18n.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** المساجد القريبة: الاستعلام لا يرسل إلا إحداثيات مقرّبة، وقراءة رد Overpass، والترتيب وحذف المكرر، وتنسيق المسافة */
class MosquesTest {
    @Test
    fun queryRoundsCoordinates() {
        val q = Mosques.query(24.71367, 46.67529, 5000)
        assertTrue(q, q.contains("(around:5000,24.714,46.675);"))
        assertFalse(q, q.contains("24.7136"))
        assertTrue(q.contains("node[") && q.contains("way[") && q.contains("relation["))
        assertTrue(q.endsWith("out tags center;"))
        assertEquals("-0.001", Mosques.coord(-0.0009))
        assertEquals("0.000", Mosques.coord(0.0004))
        assertEquals("-33.869", Mosques.coord(-33.8688))
        assertEquals("151.209", Mosques.coord(151.2093))
    }

    @Test
    fun parsesNodesWaysAndNames() {
        val json = """
            {"version":0.6,"elements":[
              {"type":"node","id":1,"lat":24.7140,"lon":46.6760,"tags":{"amenity":"place_of_worship","religion":"muslim","name":"King Mosque","name:ar":"جامع الملك","addr:street":"Olaya St","addr:housenumber":"12"}},
              {"type":"way","id":2,"center":{"lat":24.7200,"lon":46.6800},"tags":{"amenity":"place_of_worship","religion":"muslim"}},
              {"type":"way","id":3,"center":{"lat":24.71402,"lon":46.67601},"tags":{"name":"King Mosque"}},
              {"type":"relation","id":4,"tags":{"name":"No position"}}
            ]}
        """.trimIndent()
        val ar = Mosques.parse(json, "ar")
        assertEquals(3, ar.size)
        assertEquals("جامع الملك", ar[0].name)
        assertEquals("Olaya St 12", ar[0].address)
        assertEquals("node/1", ar[0].id)
        assertNull(ar[1].name)
        assertEquals(24.72, ar[1].lat, 1e-9)
        assertEquals("King Mosque", Mosques.parse(json, "fr")[0].name)

        // المبنى 3 هو المسجد 1 نفسه (على بعد أمتار وبالاسم نفسه بالإنجليزية) فيُحذف؛ الأقرب أولاً
        val near = Mosques.nearest(Mosques.parse(json, "en"), 24.7139, 46.6759)
        assertEquals(listOf("node/1", "way/2"), near.map { it.mosque.id })
        assertTrue(near[0].meters < near[1].meters)
    }

    @Test
    fun distanceAndDirection() {
        assertEquals(111195.0, Mosques.distanceM(0.0, 0.0, 1.0, 0.0), 50.0)
        assertEquals(90.0, Mosques.bearing(0.0, 0.0, 0.0, 1.0), 0.01)
        assertEquals(0.0, Mosques.bearing(0.0, 0.0, 1.0, 0.0), 0.01)
        assertEquals("east", Mosques.direction(90.0, En))
        assertEquals("north", Mosques.direction(355.0, En))
        assertEquals("350 m", Mosques.distance(347.0, En, Lang.EN))
        assertEquals("10 m", Mosques.distance(2.0, En, Lang.EN))
        assertEquals("1 km", Mosques.distance(960.0, En, Lang.EN))
        assertEquals("2.4 km", Mosques.distance(2430.0, En, Lang.EN))
        assertEquals("12 km", Mosques.distance(12300.0, En, Lang.EN))
    }
}

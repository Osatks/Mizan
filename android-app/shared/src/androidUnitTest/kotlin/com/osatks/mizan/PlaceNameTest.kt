package com.osatks.mizan

import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.LocationHelper
import com.osatks.mizan.core.Place
import com.osatks.mizan.core.PlaceNames
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.fixtures.ContentFixturesExport
import com.osatks.mizan.i18n.Ar
import com.osatks.mizan.i18n.En
import com.osatks.mizan.i18n.city
import com.osatks.mizan.i18n.placeCountryCode
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.platform.Prefs
import com.osatks.mizan.platform.geoName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** اسم الموقع: المدينة المختارة، ثم اسمه من محدد المواقع، ثم المدينة المعروفة على ٢٥ كم، ثم «موقعي الحالي» */
class PlaceNameTest {
    init { TestPacks.load() }

    private val amman = PrayTimes.city("amman")!!

    // إربد: نحو ٧٠ كم من عمّان، وأقرب مدينة معروفة إليها. كان يُكتب فيها «عمّان» بقاعدة ١٫٥ درجة
    private val irbidLat = 32.5556
    private val irbidLng = 35.85

    // على نحو ١٠ كم من مركز عمّان
    private val nearAmmanLat = 31.9454 + 0.09
    private val nearAmmanLng = 35.9284

    @Test
    fun distanceIsHaversine() {
        assertEquals(111.195, LocationHelper.distanceKm(0.0, 0.0, 0.0, 1.0), 0.01)
        assertEquals(0.0, LocationHelper.distanceKm(amman.lat, amman.lng, amman.lat, amman.lng), 1e-9)
        // بين قطبين متقابلين: نصف محيط الأرض بلا NaN
        assertEquals(Math.PI * 6371.0, LocationHelper.distanceKm(90.0, 0.0, -90.0, 0.0), 1e-6)
    }

    @Test
    fun nearCityOnlyWithin25Km() {
        val dNear = LocationHelper.distanceKm(nearAmmanLat, nearAmmanLng, amman.lat, amman.lng)
        assertTrue("$dNear", dNear in 5.0..LocationHelper.NEAR_KM)
        assertEquals("amman", LocationHelper.nearCity(nearAmmanLat, nearAmmanLng)?.key)

        val dFar = LocationHelper.distanceKm(irbidLat, irbidLng, amman.lat, amman.lng)
        assertTrue("$dFar", dFar > LocationHelper.NEAR_KM)
        assertNull(LocationHelper.nearCity(irbidLat, irbidLng))
        // في وسط الصحراء الكبرى لا مدينة معروفة قريبة
        assertNull(LocationHelper.nearCity(23.0, 12.0))
        // كل مدينة معروفة هي أقرب مدينة إلى نفسها
        for (c in PrayTimes.CITIES) assertEquals(c.key, LocationHelper.nearCity(c.lat, c.lng)?.key)
    }

    @Test
    fun placeFromUsesNearestCityForMethodButNamesOnlyWithin25Km() {
        val far = LocationHelper.placeFrom(irbidLat, irbidLng)
        assertTrue(far.gps)
        assertNull(far.key)
        assertNull(far.name)
        // الطريقة الافتراضية من أقرب مدينة مهما بعدت (كما كانت)
        assertEquals(PrayTimes.nearestCity(far.lat, far.lng).first.method, far.method)
        assertEquals("jordan", far.method)
        assertEquals("jordan", AppSettings.defaultMethod(far))

        val near = LocationHelper.placeFrom(nearAmmanLat, nearAmmanLng)
        assertEquals("amman", near.key)
        assertEquals("jordan", near.method)
    }

    @Test
    fun manualCityShowsTheChosenCity() {
        val p = Place.of(amman)
        assertEquals(En.city("amman"), En.placeName(p))
        assertEquals(Ar.city("amman"), Ar.placeName(p))
        // اسم شارد على مدينة مختارة لا يغلبها
        assertEquals(En.city("amman"), En.placeName(p.copy(name = "Somewhere", nameLang = "en")))
        assertEquals("JO", placeCountryCode(p))
    }

    @Test
    fun gpsShowsGeocodedNameFirst() {
        val far = LocationHelper.placeFrom(irbidLat, irbidLng).copy(name = "Irbid", nameLang = "en", cc = "JO")
        assertEquals("Irbid", En.placeName(far))
        // بلغة أخرى ولا مدينة معروفة قريبة: الاسم السابق حتى يصل بلغة الواجهة، لا «موقعي الحالي»
        assertEquals("Irbid", Ar.placeName(far))
        assertEquals("JO", placeCountryCode(far))

        val near = LocationHelper.placeFrom(nearAmmanLat, nearAmmanLng).copy(name = "Wadi as-Sir", nameLang = "en")
        assertEquals("Wadi as-Sir", En.placeName(near))
        // بلغة أخرى وقرب مدينة معروفة: اسم المدينة بلغة الواجهة إلى أن يصل الاسم بها
        assertEquals(Ar.city("amman"), Ar.placeName(near))
        assertEquals("إربد", Ar.placeName(far.copy(name = "إربد", nameLang = "ar")))
    }

    @Test
    fun gpsWithoutNameUsesCityWithin25KmElseMyLocation() {
        val near = LocationHelper.placeFrom(nearAmmanLat, nearAmmanLng)
        assertEquals(En.city("amman"), En.placeName(near))
        assertEquals("JO", placeCountryCode(near))

        val far = LocationHelper.placeFrom(irbidLat, irbidLng)
        assertEquals(En.myLocation, En.placeName(far))
        assertEquals(Ar.myLocation, Ar.placeName(far))
        assertNull(placeCountryCode(far))

        // مكان GPS حُفظ بقاعدة ١٫٥ درجة القديمة (مفتاح عمّان في إربد): لا يُكتب فيه «عمّان» بعد الآن
        val legacy = far.copy(key = "amman")
        assertEquals(En.myLocation, En.placeName(legacy))
    }

    @Test
    fun needsNameOnlyForGpsWithoutNameInThisLanguage() {
        val gps = LocationHelper.placeFrom(irbidLat, irbidLng)
        assertTrue(PlaceNames.needs(gps, "en"))
        assertFalse(PlaceNames.needs(gps.copy(name = "Irbid", nameLang = "en"), "en"))
        assertTrue(PlaceNames.needs(gps.copy(name = "Irbid", nameLang = "en"), "ar"))
        assertFalse(PlaceNames.needs(Place.of(amman), "en"))
    }

    @Test
    fun geocoderFieldsFallBackInOrder() {
        assertEquals("Irbid", geoName("Irbid", "Irbid Qasabah", "Irbid Governorate", "jo")?.name)
        assertEquals("Irbid Qasabah", geoName(null, "Irbid Qasabah", "Irbid Governorate", null)?.name)
        val g = geoName(" ", null, " Irbid Governorate ", "jo")
        assertEquals("Irbid Governorate", g?.name)
        assertEquals("JO", g?.cc)
        assertNull(geoName(null, "", null, "JO"))
        assertNull(geoName("Irbid", null, null, "JOR")?.cc)
    }

    // ---------- الحفظ ----------

    private fun memPrefs(): Pair<Prefs, ContentFixturesExport.MemPrefs> {
        val mem = ContentFixturesExport.MemPrefs()
        val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null) as sun.misc.Unsafe
        val prefs = unsafe.allocateInstance(Prefs::class.java) as Prefs
        Prefs::class.java.getDeclaredField("sp").apply { isAccessible = true }.set(prefs, mem)
        return prefs to mem
    }

    @Test
    fun oldSavedPlaceWithoutNameStillLoads() {
        val (prefs, mem) = memPrefs()
        assertNull(AppSettings.readPlace(prefs))
        // ما يحفظه الإصدار السابق: بلا مفاتيح الاسم
        mem.m.putAll(mapOf(
            "place_key" to "amman", "place_lat" to "32.5556", "place_lng" to "35.85", "place_tz" to "Asia/Amman",
            "place_method" to "jordan", "place_gps" to true, "place_elev" to "0", "method" to "jordan",
        ))
        val p = AppSettings.readPlace(prefs)!!
        assertEquals(Place("amman", 32.5556, 35.85, "Asia/Amman", "jordan", gps = true, elev = 0), p)
        assertNull(p.name)
        assertNull(p.nameLang)
        assertNull(p.cc)
        // ويُعرض بالقاعدة الجديدة (لا «عمّان» على بعد ٧٠ كم)
        assertEquals(En.myLocation, En.placeName(p))

        // مدينة مختارة من إصدار قديم جداً بلا ارتفاع
        mem.m.clear()
        mem.m.putAll(mapOf("place_key" to "riyadh", "place_lat" to "24.7136", "place_lng" to "46.6753"))
        val r = AppSettings.readPlace(prefs)!!
        assertEquals("riyadh", r.key)
        assertFalse(r.gps)
        assertNull(r.name)
        assertEquals(En.city("riyadh"), En.placeName(r))
    }

    @Test
    fun nameIsSavedAndRemovedWithThePlace() {
        val (prefs, mem) = memPrefs()
        val named = Place(null, 32.5556, 35.85, "Asia/Amman", "jordan", gps = true, name = "Irbid", nameLang = "en", cc = "JO")
        AppSettings.writePlace(prefs, named)
        assertEquals(named, AppSettings.readPlace(prefs))
        assertEquals("Irbid", mem.m["place_geo_name"])
        // «place_name» مفتاح الإصدارات العربية الأولى (يرحّله migrate)، فلا يُكتب
        assertFalse(mem.m.containsKey("place_name"))

        AppSettings.writePlace(prefs, Place.of(amman))
        val back = AppSettings.readPlace(prefs)!!
        assertEquals(Place.of(amman), back)
        assertFalse(mem.m.containsKey("place_geo_name"))
        assertFalse(mem.m.containsKey("place_geo_lang"))
        assertFalse(mem.m.containsKey("place_geo_cc"))
    }

    @Test
    fun geocodedNameArrivesAfterThePlaceIsSaved() {
        val (prefs, _) = memPrefs()
        val spField = AppSettings::class.java.getDeclaredField("sp").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        fun <T> state(name: String) = AppSettings::class.java.getDeclaredField("$name\$delegate").apply { isAccessible = true }
            .get(null) as androidx.compose.runtime.MutableState<T>
        val placeState = state<Place>("place")
        val placeSetState = state<Boolean>("placeSet")
        val methodState = state<String>("method")
        val saved = listOf(spField.get(null), placeState.value, placeSetState.value, methodState.value)
        spField.set(null, prefs)
        try {
            // الموقع يُحفظ فوراً بلا اسم
            AppSettings.updatePlace(LocationHelper.placeFrom(irbidLat, irbidLng))
            val p = AppSettings.place
            assertNull(p.name)
            assertEquals(En.myLocation, En.placeName(p))

            // يصل الاسم: يُحفظ مع لغته ودولته
            assertTrue(AppSettings.updatePlaceName(p.lat, p.lng, " Irbid ", "en", "jo"))
            assertEquals("Irbid", AppSettings.place.name)
            assertEquals("en", AppSettings.place.nameLang)
            assertEquals("JO", AppSettings.place.cc)
            assertEquals(AppSettings.place, AppSettings.readPlace(prefs))
            assertEquals("Irbid", En.placeName(AppSettings.place))
            assertFalse(PlaceNames.needs(AppSettings.place, "en"))

            // اسم لموقع سابق (تغيّر الموقع قبل أن يصل) يُهمل
            assertFalse(AppSettings.updatePlaceName(p.lat + 0.5, p.lng, "Elsewhere", "en", null))
            assertEquals("Irbid", AppSettings.place.name)

            // موقع جديد على نحو ١٠٠ م: يبقى الاسم
            AppSettings.updatePlace(LocationHelper.placeFrom(irbidLat + 0.0009, irbidLng))
            assertEquals("Irbid", AppSettings.place.name)
            assertEquals("Irbid", AppSettings.readPlace(prefs)?.name)

            // موقع جديد على نحو ٥ كم: بلا اسم حتى يُطلب له
            AppSettings.updatePlace(LocationHelper.placeFrom(irbidLat + 0.045, irbidLng))
            assertNull(AppSettings.place.name)
            assertNotNull(AppSettings.readPlace(prefs))
            assertNull(AppSettings.readPlace(prefs)?.name)

            // مدينة مختارة من القائمة لا تأخذ اسماً من محدد المواقع
            AppSettings.updatePlace(Place.of(amman))
            assertFalse(AppSettings.updatePlaceName(amman.lat, amman.lng, "Somewhere", "en", null))
            assertEquals(En.city("amman"), En.placeName(AppSettings.place))
        } finally {
            spField.set(null, saved[0])
            placeState.value = saved[1] as Place
            placeSetState.value = saved[2] as Boolean
            methodState.value = saved[3] as String
        }
    }
}

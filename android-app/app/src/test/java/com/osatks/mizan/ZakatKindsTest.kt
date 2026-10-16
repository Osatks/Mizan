package com.osatks.mizan

import com.osatks.mizan.core.ZakatKinds
import com.osatks.mizan.core.ZakatKinds.Due
import org.junit.Assert.assertEquals
import org.junit.Test

/** أنصبة الأنعام كما في كتاب أبي بكر (البخاري 1454) وحديث معاذ، والزروع والركاز */
class ZakatKindsTest {
    @Test
    fun sheep() {
        assertEquals(emptyList<Due>(), ZakatKinds.sheep(39))
        assertEquals(listOf(Due("shah", 1)), ZakatKinds.sheep(40))
        assertEquals(listOf(Due("shah", 1)), ZakatKinds.sheep(120))
        assertEquals(listOf(Due("shah", 2)), ZakatKinds.sheep(121))
        assertEquals(listOf(Due("shah", 3)), ZakatKinds.sheep(201))
        assertEquals(listOf(Due("shah", 3)), ZakatKinds.sheep(399))
        assertEquals(listOf(Due("shah", 4)), ZakatKinds.sheep(400))
        assertEquals(listOf(Due("shah", 5)), ZakatKinds.sheep(599))
    }

    @Test
    fun cattle() {
        assertEquals(emptyList<Due>(), ZakatKinds.cattle(29))
        assertEquals(listOf(Due("tabi", 1)), ZakatKinds.cattle(30))
        assertEquals(listOf(Due("musinnah", 1)), ZakatKinds.cattle(40))
        assertEquals(listOf(Due("tabi", 2)), ZakatKinds.cattle(60))
        assertEquals(listOf(Due("tabi", 1), Due("musinnah", 1)), ZakatKinds.cattle(70))
        assertEquals(listOf(Due("musinnah", 2)), ZakatKinds.cattle(80))
        assertEquals(listOf(Due("tabi", 3)), ZakatKinds.cattle(90))
        assertEquals(listOf(Due("musinnah", 3)), ZakatKinds.cattle(120))
    }

    @Test
    fun camels() {
        assertEquals(emptyList<Due>(), ZakatKinds.camels(4))
        assertEquals(listOf(Due("shah", 1)), ZakatKinds.camels(9))
        assertEquals(listOf(Due("shah", 4)), ZakatKinds.camels(24))
        assertEquals(listOf(Due("bintMakhad", 1)), ZakatKinds.camels(25))
        assertEquals(listOf(Due("bintLabun", 1)), ZakatKinds.camels(36))
        assertEquals(listOf(Due("hiqqah", 1)), ZakatKinds.camels(46))
        assertEquals(listOf(Due("jadhaah", 1)), ZakatKinds.camels(61))
        assertEquals(listOf(Due("bintLabun", 2)), ZakatKinds.camels(76))
        assertEquals(listOf(Due("hiqqah", 2)), ZakatKinds.camels(120))
        assertEquals(listOf(Due("bintLabun", 3)), ZakatKinds.camels(121))
        assertEquals(listOf(Due("bintLabun", 2), Due("hiqqah", 1)), ZakatKinds.camels(130))
        assertEquals(listOf(Due("hiqqah", 3)), ZakatKinds.camels(150))
        assertEquals(listOf(Due("bintLabun", 4)), ZakatKinds.camels(160))
    }

    @Test
    fun cropsAndTreasure() {
        assertEquals(0.0, ZakatKinds.crops(600.0, "rain"), 0.0)
        assertEquals(100.0, ZakatKinds.crops(1000.0, "rain"), 1e-9)
        assertEquals(50.0, ZakatKinds.crops(1000.0, "irrigated"), 1e-9)
        assertEquals(75.0, ZakatKinds.crops(1000.0, "mixed"), 1e-9)
        assertEquals(200.0, ZakatKinds.rikaz(1000.0), 1e-9)
        assertEquals(0.0, ZakatKinds.mineral(1000.0, 5000.0), 0.0)
        assertEquals(250.0, ZakatKinds.mineral(10000.0, 5000.0), 1e-9)
    }
}

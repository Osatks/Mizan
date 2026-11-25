package com.osatks.mizan

import com.osatks.mizan.core.Backup
import com.osatks.mizan.core.BackupException
import com.osatks.mizan.core.BackupStore
import com.osatks.mizan.core.CloudChunks
import com.osatks.mizan.i18n.pack.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * النسخ الاحتياطي: المستند يُكتب ثم يُقرأ بالقيم نفسها، والمستثنى لا يُنسخ ولا يُمسّ عند الاستعادة،
 * ونسخة iOS تُستعاد في أندرويد والعكس (الأنواع موحّدة)، والملفات غير الصالحة تُرفض بسببها.
 */
class BackupTest {
    /** مخزن في الذاكرة بدل Prefs */
    private class MapStore(initial: Map<String, Any> = emptyMap()) : BackupStore {
        val m = LinkedHashMap<String, Any>(initial)
        override fun snapshot(): Map<String, Any> = LinkedHashMap(m)
        override fun put(k: String, v: Any) { m[k] = v }
        override fun remove(k: String) { m.remove(k) }
    }

    /** ما يكتبه Prefs في أندرويد: Int و Long و Float و Boolean و String و Set<String> */
    private val android: Map<String, Any> = linkedMapOf(
        "accent" to "teal",
        "lang" to "ar",
        "notify" to true,
        "use24h" to false,
        "q_size" to 30,
        "q_last" to 2250,
        "q_marks" to "255,6235,12",
        "k_read" to "AAAA//8=",
        "ts_total" to 123456,
        "big_counter" to 9_000_000_000L,
        "ratio" to 1.5f,
        "place_lat" to "21.4225",
        "tags" to setOf("b", "a"),
        "fast_2026_ramadan" to "1111011",   // ميزة جديدة: تُنسخ دون أن تُذكر هنا
        "kids_stars" to 7,
        // مستثناة: حال الجهاز
        "lang_synced" to true,
        "rv_sessions" to 12,
        "icloud_stamp" to "1764064800000",
        "onboarded" to true,
        "calendar.id" to "ABC",
        "notif_scheduled" to "x",
    )

    @Test fun roundTripKeepsValuesAndTypes() {
        val text = Backup.encode(android, 1_764_064_800_000L, "android", "2.4.2")
        val doc = Backup.decode(text)
        assertEquals(Backup.FORMAT, doc.format)
        assertEquals("android", doc.platform)
        assertEquals("2.4.2", doc.appVersion)
        assertEquals(1_764_064_800_000L, doc.createdAt)
        val d = doc.data
        assertEquals("teal", d["accent"])
        assertEquals(true, d["notify"])
        assertEquals(false, d["use24h"])
        assertEquals(30L, d["q_size"])
        assertEquals(2250L, d["q_last"])
        assertEquals("255,6235,12", d["q_marks"])
        assertEquals("AAAA//8=", d["k_read"])
        assertEquals(9_000_000_000L, d["big_counter"])
        assertEquals(1.5, d["ratio"])
        assertEquals("21.4225", d["place_lat"])
        assertEquals(setOf("a", "b"), d["tags"])
        assertEquals("1111011", d["fast_2026_ramadan"])
        assertEquals(7L, d["kids_stars"])
        for (k in listOf("lang_synced", "rv_sessions", "icloud_stamp", "onboarded", "calendar.id", "notif_scheduled")) {
            assertFalse("$k must not be exported", k in d)
            assertFalse("$k must not be in the file", "\"$k\"" in text)
        }
        // المستند نفسه يُكتب مرة ثانية بلا تغيير
        assertEquals(text, Backup.encode(d, doc.createdAt, doc.platform, doc.appVersion))
    }

    @Test fun documentHasVersionedEnvelope() {
        val root = Json.obj(Backup.encode(mapOf("accent" to "teal"), 0L, "ios", "1.0"))
        assertEquals("mizan-backup", root["kind"])
        assertEquals(Backup.FORMAT.toLong(), root["format"])
        assertEquals("ios", root["platform"])
        assertEquals("1970-01-01T00:00:00Z", root["createdAtUtc"])
        assertTrue(root["data"] is Map<*, *>)
    }

    /** الاستعادة تستبدل البيانات: يُحذف ما ليس في النسخة، ويبقى المستثنى كما هو في هذا الجهاز */
    @Test fun restoreReplacesDataButKeepsDeviceKeys() {
        val doc = Backup.decode(Backup.encode(android, 1L, "android", "2.4.2"))
        val device = MapStore(
            mapOf(
                "accent" to "rose",
                "q_marks" to "1",
                "zk_cur" to "EUR",          // ليس في النسخة: يُحذف
                "lang_synced" to false,     // حال هذا الجهاز: يبقى
                "rv_sessions" to 3L,
                "icloud_device" to "abcd",
            )
        )
        Backup.apply(device, doc.data)
        assertEquals("teal", device.m["accent"])
        assertEquals("255,6235,12", device.m["q_marks"])
        assertFalse("zk_cur" in device.m)
        assertEquals(false, device.m["lang_synced"])
        assertEquals(3L, device.m["rv_sessions"])
        assertEquals("abcd", device.m["icloud_device"])
        assertEquals(doc.data, device.snapshot().filterKeys { Backup.included(it) })
    }

    /** نسخة كتبها iPhone (الأعداد بلا تمييز Int من Long، و NSArray للمجموعة) تُقرأ في أندرويد */
    @Test fun iosBackupRestoresOnAndroid() {
        val ios = """
            {
              "kind": "mizan-backup", "format": 1, "app": "Mizan", "appVersion": "2.4.2", "platform": "ios",
              "createdAt": 1764064800000,
              "data": { "notify": true, "q_size": 28, "accent": "gold", "tags": ["x", "y"], "speed": 1.25,
                        "spotlight_quran": "v3", "weird": {"nested": 1}, "mixed": [1, "a"] }
            }
        """.trimIndent()
        val doc = Backup.decode(ios)
        assertEquals("ios", doc.platform)
        assertEquals(mapOf("notify" to true, "q_size" to 28L, "accent" to "gold", "tags" to setOf("x", "y"), "speed" to 1.25), doc.data)
        val store = MapStore()
        Backup.apply(store, doc.data)
        assertEquals(28L, store.m["q_size"])
    }

    @Test fun rejectsBadFiles() {
        fun reason(text: String): BackupException.Reason? = try {
            Backup.decode(text); null
        } catch (e: BackupException) {
            e.reason
        }
        assertEquals(BackupException.Reason.INVALID, reason("not json"))
        assertEquals(BackupException.Reason.INVALID, reason("[1,2]"))
        assertEquals(BackupException.Reason.INVALID, reason("""{"kind":"other","format":1,"data":{"a":"b"}}"""))
        assertEquals(BackupException.Reason.INVALID, reason("""{"kind":"mizan-backup","data":{"a":"b"}}"""))
        assertEquals(BackupException.Reason.INVALID, reason("""{"kind":"mizan-backup","format":1}"""))
        assertEquals(BackupException.Reason.TOO_NEW, reason("""{"kind":"mizan-backup","format":${Backup.FORMAT + 1},"data":{"a":"b"}}"""))
        assertEquals(BackupException.Reason.EMPTY, reason("""{"kind":"mizan-backup","format":1,"data":{"rv_days":4}}"""))
        // علامة ترتيب البايتات في أول الملف (بعض المحررات) لا تمنع قراءته
        assertNull(reason("﻿" + Backup.encode(mapOf("accent" to "teal"), 0L, "android", "1")))
    }

    @Test fun nonFiniteNumbersAreDropped() {
        val d = Backup.decode(Backup.encode(mapOf("a" to Double.NaN, "b" to Float.POSITIVE_INFINITY, "c" to "ok"), 0L, "android", "1")).data
        assertEquals(mapOf("c" to "ok"), d)
    }

    @Test fun exclusionRules() {
        for (k in Backup.EXCLUDED_KEYS) assertFalse(k, Backup.included(k))
        for (k in listOf("rv_asked_version", "icloud_sync", "calendar.hash", "cache_x", "device_y", "widget_z")) assertFalse(k, Backup.included(k))
        for (k in listOf("accent", "q_marks", "k_read", "ts_total", "zk_cur", "adhkar_done_morning", "fast_x", "kids_x", "live_countdown"))
            assertTrue(k, Backup.included(k))
    }

    /** القطع في iCloud: تُجمع بترتيبها، وترفض ناقصةً أو بطول غير متوقع */
    @Test fun cloudChunksRoundTrip() {
        val text = Backup.encode(android, 5L, "ios", "2.4.2").repeat(7)
        val parts = CloudChunks.split(text, 1000)
        assertTrue(parts.size > 1)
        assertTrue(parts.all { it.length <= 1000 })
        assertEquals(text, CloudChunks.join(parts, text.length))
        assertNull(CloudChunks.join(parts.dropLast(1) + null, text.length))
        assertNull(CloudChunks.join(parts.dropLast(1), text.length))
        assertEquals(listOf(""), CloudChunks.split("", 10))
        try {
            CloudChunks.split("x", 0); fail()
        } catch (e: IllegalArgumentException) {
        }
    }
}

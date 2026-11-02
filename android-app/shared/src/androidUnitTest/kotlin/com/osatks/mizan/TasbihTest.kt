package com.osatks.mizan

import com.osatks.mizan.core.Tasbih
import com.osatks.mizan.core.TasbihState
import org.junit.Assert.assertEquals
import org.junit.Test

/** المسبحة: العدّ وإتمام الدورة، وتسبيح ما بعد الصلاة (33 + 33 + 33 ثم تمام المائة)، والتصفير والتراجع ويوم جديد */
class TasbihTest {
    private fun times(s0: TasbihState, n: Int): Pair<TasbihState, List<Tasbih.Event>> {
        var s = s0
        val events = ArrayList<Tasbih.Event>()
        repeat(n) { val (next, e) = s.count(); s = next; events += e }
        return s to events
    }

    @Test fun freeRoundCompletes() {
        val (s, events) = times(TasbihState(target = 33), 33)
        assertEquals(0, s.count)
        assertEquals(1, s.rounds)
        assertEquals(33, s.total)
        assertEquals(33, s.today)
        assertEquals(Tasbih.Event.ROUND, events.last())
        assertEquals(32, events.count { it == Tasbih.Event.COUNT })
    }

    @Test fun noLimitNeverCompletes() {
        val (s, events) = times(TasbihState(target = 0), 250)
        assertEquals(250, s.count)
        assertEquals(0, s.rounds)
        assertEquals(setOf(Tasbih.Event.COUNT), events.toSet())
    }

    @Test fun afterPrayerSequence() {
        val (s, events) = times(TasbihState(mode = "salah"), 100)
        assertEquals(0, s.step)
        assertEquals(0, s.count)
        assertEquals(1, s.rounds)
        assertEquals(listOf(Tasbih.Event.NEXT_DHIKR, Tasbih.Event.NEXT_DHIKR, Tasbih.Event.NEXT_DHIKR, Tasbih.Event.HUNDRED), events.filter { it != Tasbih.Event.COUNT })
        // في الخطوة الأخيرة: تمام المائة مرة واحدة
        val (last, _) = times(TasbihState(mode = "salah"), 99)
        assertEquals(3, last.step)
        assertEquals(-1, last.dhikrIndex)
        assertEquals(Tasbih.TAMAM, last.text)
        assertEquals(1, last.goal)
    }

    @Test fun resetUndoAndNewDay() {
        val (s, _) = times(TasbihState(target = 100, day = "2026-11-01"), 10)
        assertEquals(0, s.reset().count)
        assertEquals(10, s.reset().total)
        val u = s.undo()
        assertEquals(9, u.count)
        assertEquals(9, u.total)
        assertEquals(9, u.today)
        assertEquals(0, TasbihState().undo().count)
        val next = s.on("2026-11-02")
        assertEquals(0, next.today)
        assertEquals(10, next.total)
        assertEquals(10, next.count)
    }
}

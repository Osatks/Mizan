package com.osatks.mizan.platform

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/** قفل بسيط يعمل في أندرويد و iOS */
class Lock : SynchronizedObject()

inline fun <T> Lock.locked(block: () -> T): T = synchronized(this, block)

/** خريطة آمنة بين الخيوط (بديل ConcurrentHashMap في الكود المشترك) */
class SyncMap<K, V> {
    private val lock = Lock()
    private val m = HashMap<K, V>()
    operator fun get(k: K): V? = lock.locked { m[k] }
    operator fun set(k: K, v: V) { lock.locked { m[k] = v } }
    fun getOrPut(k: K, make: () -> V): V = lock.locked { m.getOrPut(k, make) }
    fun remove(k: K): V? = lock.locked { m.remove(k) }
    fun clear() = lock.locked { m.clear() }
    fun removeKeys(pred: (K) -> Boolean) = lock.locked { m.keys.removeAll(pred) }
    val size: Int get() = lock.locked { m.size }
}

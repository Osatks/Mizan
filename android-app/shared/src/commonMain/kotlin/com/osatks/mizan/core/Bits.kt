package com.osatks.mizan.core

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * مجموعة بتات بصيغة java.util.BitSet نفسها عند الحفظ (toByteArray: البت i في البايت i/8)،
 * فتبقى بيانات الختمة المحفوظة في الإصدارات السابقة صالحة.
 */
class Bits(size: Int) {
    private var bytes = ByteArray((size + 7) / 8)

    operator fun get(i: Int): Boolean = i / 8 < bytes.size && (bytes[i / 8].toInt() shr (i % 8)) and 1 == 1

    fun set(i: Int) {
        if (i / 8 >= bytes.size) bytes = bytes.copyOf(i / 8 + 1)
        bytes[i / 8] = (bytes[i / 8].toInt() or (1 shl (i % 8))).toByte()
    }

    fun cardinality(): Int = bytes.sumOf { it.toInt().and(0xFF).countOneBits() }

    /** كما في BitSet.toByteArray: تُحذف البايتات الصفرية في الآخر */
    fun toByteArray(): ByteArray {
        var n = bytes.size
        while (n > 0 && bytes[n - 1].toInt() == 0) n--
        return bytes.copyOf(n)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun toBase64(): String = Base64.encode(toByteArray())

    companion object {
        fun valueOf(b: ByteArray, size: Int) = Bits(maxOf(size, b.size * 8)).also { it.bytes = b.copyOf(maxOf(b.size, (size + 7) / 8)) }

        @OptIn(ExperimentalEncodingApi::class)
        fun fromBase64(s: String, size: Int): Bits = runCatching { valueOf(Base64.decode(s), size) }.getOrElse { Bits(size) }
    }
}

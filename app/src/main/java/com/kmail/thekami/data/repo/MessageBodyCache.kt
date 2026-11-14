package com.kmail.thekami.data.repo

import javax.inject.Inject
import javax.inject.Singleton

/** Small in-memory cache of raw messages. Nothing is written to disk yet. */
@Singleton
class MessageBodyCache @Inject constructor() {
    private val map = LinkedHashMap<Long, ByteArray>(16, 0.75f, true)
    private var total = 0L

    @Synchronized
    fun get(id: Long): ByteArray? = map[id]

    @Synchronized
    fun put(id: Long, raw: ByteArray) {
        if (raw.size > MAX_BYTES) return
        map.remove(id)?.let { total -= it.size }
        map[id] = raw
        total += raw.size
        val iterator = map.entries.iterator()
        while (total > MAX_BYTES && iterator.hasNext()) {
            val eldest = iterator.next()
            total -= eldest.value.size
            iterator.remove()
        }
    }

    private companion object {
        const val MAX_BYTES = 16L * 1024 * 1024
    }
}

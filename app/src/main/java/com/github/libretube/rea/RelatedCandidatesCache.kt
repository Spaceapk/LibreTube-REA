package com.github.libretube.rea

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** Small provider boundary: injectable clock/fetcher, stale fallback, no cached failures. */
internal class RelatedCandidatesCache<T>(
    private val now: () -> Long = System::currentTimeMillis,
    private val capacity: Int = 8,
    private val ttlMillis: Long = 900000L,
    private val timeoutMillis: Long = 4000L
) {
    private data class Entry<T>(val saved: Long, val items: List<T>)
    private val lock = Mutex()
    private val cache = LinkedHashMap<String, Entry<T>>()

    suspend fun get(key: String, fetch: suspend () -> List<T>): List<T> {
        val previous = lock.withLock { cache[key] }
        if (previous != null && now() - previous.saved in 0 until ttlMillis) return previous.items
        val loaded = try {
            withTimeoutOrNull(timeoutMillis) { fetch().take(40) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        if (loaded != null) lock.withLock {
            cache[key] = Entry(now(), loaded)
            while (cache.size > capacity.coerceAtLeast(1)) cache.remove(cache.keys.first())
        }
        return loaded ?: previous?.items.orEmpty()
    }
}

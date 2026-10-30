package com.github.libretube.rea

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Provider contract tests use injected fetchers; these are not live YouTube tests. */
class RelatedCandidatesCacheTest {
    @Test fun cacheAndStaleFallback() = runBlocking {
        var clock = 0L
        var calls = 0
        val cache = RelatedCandidatesCache<String>(now = { clock }, ttlMillis = 10)
        assertEquals(listOf("real-provider-item"), cache.get("seed") { calls++; listOf("real-provider-item") })
        assertEquals(listOf("real-provider-item"), cache.get("seed") { error("cache should be used") })
        assertEquals(1, calls)
        clock = 11
        assertEquals(listOf("real-provider-item"), cache.get("seed") { error("network down") })
        assertEquals(emptyList<String>(), cache.get("other") { error("network down") })
    }
    @Test fun boundedAndEvicted() = runBlocking {
        val cache = RelatedCandidatesCache<Int>(capacity = 1)
        assertEquals(40, cache.get("one") { (0..100).toList() }.size)
        cache.get("two") { listOf(2) }
        assertEquals(listOf(3), cache.get("one") { listOf(3) })
    }
    @Test fun timeoutFallsBack() = runBlocking {
        val cache = RelatedCandidatesCache<Int>(timeoutMillis = 1)
        assertEquals(emptyList<Int>(), cache.get("one") { delay(100); listOf(1) })
    }
    @Test fun cancellationPropagates() = runBlocking {
        val cache = RelatedCandidatesCache<Int>()
        try {
            cache.get("one") { throw CancellationException("cancelled") }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }
}

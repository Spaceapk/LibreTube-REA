package com.github.libretube.rea

import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.api.obj.ContentItem
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.enums.WatchHistoryStatus
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.repo.LocalUserDataRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** All profiling stays on the device. History requires a separate, default-off consent. */
object Personalization {
    private val prefs get() = PreferenceHelper.settings
    private fun enabled() = prefs.getBoolean("rea_enabled", true)
    private val relatedCache = RelatedCandidatesCache<StreamItem>()

    private suspend fun <T> optional(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private suspend fun config(query: String = "", search: Boolean = false): LocalRanker.Config {
        val settings = LocalRanker.Config()
        settings.subscriptions = optional { withTimeoutOrNull(1500) { SubscriptionHelper.getSubscriptionChannelIds().map { it.toID() }.toSet() } }
            ?: emptySet()
        settings.dismissed = prefs.getString("rea_dismissed", "").orEmpty().split(',')
            .map { it.trim().toID() }.filter { it.isNotEmpty() }.toSet()
        settings.interests = LocalRanker.tokens(prefs.getString("rea_interests", ""))
        settings.query = query
        settings.subscriptionWeight = prefs.getString("rea_subscription_weight", "3")?.toDoubleOrNull()?.coerceIn(0.0, 10.0) ?: 3.0
        settings.recencyWeight = prefs.getString("rea_recency_weight", "2")?.toDoubleOrNull()?.coerceIn(0.0, 10.0) ?: 2.0
        settings.topicWeight = prefs.getString("rea_topic_weight", "2")?.toDoubleOrNull()?.coerceIn(0.0, 10.0) ?: 2.0
        if (search) {
            settings.subscriptionsOnly = prefs.getBoolean("rea_search_subscriptions", false)
            val days = prefs.getString("rea_search_days", "0")?.toLongOrNull()?.coerceIn(0, 3650) ?: 0
            settings.minDuration = prefs.getString("rea_min_duration", "0")?.toLongOrNull()?.coerceIn(0, 86400) ?: 0
            settings.maxDuration = prefs.getString("rea_max_duration", "0")?.toLongOrNull()?.coerceIn(0, 86400) ?: 0
            if (days > 0) settings.after = System.currentTimeMillis() - days * 86400000L
        }
        if (prefs.getBoolean("rea_history_consent", false) && PlayerHelper.watchHistoryEnabled) {
            val history = optional {
                LocalUserDataRepository().getWatchHistory(100, null, WatchHistoryStatus.ALL).first
            }.orEmpty()
            settings.historyChannels = history.mapNotNull { it.video.uploaderUrl?.toID() }.toSet()
        }
        return settings
    }

    private suspend fun related(seed: StreamItem): List<StreamItem> {
        val id = seed.url?.toID() ?: return emptyList()
        return relatedCache.get(id) {
            MediaServiceRepository.instance.getStreams(id).relatedStreams
        }
    }

    suspend fun home(videos: List<StreamItem>): List<StreamItem> = withContext(Dispatchers.Default) {
        if (!enabled()) return@withContext videos
        val settings = config()
        // Related-video edges provide real candidates from other channels, without inventing channel similarity.
        val discovery = if (prefs.getBoolean("rea_discovery", false)) {
            videos.firstOrNull { it.url != null && it.uploaderUrl?.toID() in settings.subscriptions }
                ?.let { related(it) }.orEmpty()
        } else emptyList()
        val all = (videos.take(1500) + discovery).filter { it.url != null }
        settings.limit = all.size.coerceAtMost(1500)
        val byId = all.associateBy { it.url!!.toID() }
        LocalRanker.rank(all.map {
            LocalRanker.Video(it.url!!.toID(), it.uploaderUrl?.toID(), it.title, it.uploaded, it.duration ?: -1, it.views ?: -1)
        }, settings, System.currentTimeMillis()).mapNotNull { byId[it] }
    }

    suspend fun search(items: List<ContentItem>, query: String): List<ContentItem> = withContext(Dispatchers.Default) {
        if (!enabled()) return@withContext items
        val settings = config(query, search = true)
        val streams = items.filter { it.type == StreamItem.TYPE_STREAM }
        settings.limit = streams.size
        val byId = streams.associateBy { it.url.toID() }
        val sorted = LocalRanker.rank(streams.map {
            LocalRanker.Video(it.url.toID(), it.uploaderUrl?.toID(), it.title, it.uploaded, it.duration, it.views)
        }, settings, System.currentTimeMillis()).mapNotNull { byId[it] }.iterator()
        // Retain channel/playlist positions and upstream pagination tokens.
        // Video-only filters exclude nonvideo entries rather than implying they satisfy a date.
        if (settings.subscriptionsOnly || settings.after > 0 || settings.minDuration > 0 || settings.maxDuration > 0) return@withContext sorted.asSequence().toList()
        buildList {
            items.forEach { item ->
                if (item.type != StreamItem.TYPE_STREAM) add(item)
                else if (sorted.hasNext()) add(sorted.next())
            }
        }
    }
}

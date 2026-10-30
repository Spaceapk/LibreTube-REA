package com.github.libretube.rea;

import java.text.Normalizer;
import java.util.*;

/** Offline, bounded, deterministic ranking. Missing metadata never becomes invented evidence. */
public final class LocalRanker {
    public static final class Video {
        public final String id, channel, title;
        public final long uploaded, duration, views;
        public Video(String id, String channel, String title, long uploaded, long duration, long views) {
            this.id = id; this.channel = channel; this.title = title;
            this.uploaded = uploaded; this.duration = duration; this.views = views;
        }
    }
    public static final class Config {
        public Set<String> subscriptions = Collections.emptySet();
        public Set<String> dismissed = Collections.emptySet();
        public Set<String> interests = Collections.emptySet();
        public Set<String> historyChannels = Collections.emptySet();
        public String query = "";
        public boolean subscriptionsOnly = false;
        public long after = 0, minDuration = 0, maxDuration = 0;
        public double subscriptionWeight = 3, recencyWeight = 2, topicWeight = 2;
        public int limit = 200, channelWindow = 3;
    }
    public static Set<String> tokens(String text) {
        Set<String> result = new HashSet<>();
        String normalized = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        for (String token : normalized.split("[^\\p{L}\\p{N}]+")) if (token.length() > 1) result.add(token);
        return result;
    }
    private static double overlap(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        int n = 0;
        for (String token : a) if (b.contains(token)) n++;
        return (double)n / a.size();
    }
    public static List<String> rank(List<Video> input, Config config, long now) {
        Set<String> query = tokens(config.query);
        Map<String, Video> unique = new LinkedHashMap<>();
        for (Video video : input) {
            if (video.id == null || video.id.isEmpty() || config.dismissed.contains(video.id)) continue;
            if (config.subscriptionsOnly && !config.subscriptions.contains(video.channel)) continue;
            if (config.after > 0 && (video.uploaded <= 0 || video.uploaded < config.after || video.uploaded > now)) continue;
            if (config.minDuration > 0 && video.duration < config.minDuration) continue;
            if (config.maxDuration > 0 && (video.duration < 0 || video.duration > config.maxDuration)) continue;
            unique.putIfAbsent(video.id, video);
            if (unique.size() >= 2000) break;
        }
        Map<String, Double> scores = new HashMap<>();
        for (Video video : unique.values()) {
            Set<String> title = tokens(video.title);
            double ageDays = Math.max(0, (now - video.uploaded) / 86400000.0);
            double recency = video.uploaded > 0 && video.uploaded <= now ? Math.exp(-ageDays / 14) : 0;
            double score = (config.subscriptions.contains(video.channel) ? config.subscriptionWeight : 0)
                + config.recencyWeight * recency + config.topicWeight * overlap(config.interests, title)
                + (config.historyChannels.contains(video.channel) ? 1 : 0)
                + 8 * overlap(query, title);
            // Popularity is a weak, capped signal, never a proxy for relevance.
            if (video.views > 0) score += Math.min(0.3, Math.log1p(video.views) / 60);
            scores.put(video.id, score);
        }
        List<Video> remaining = new ArrayList<>(unique.values());
        remaining.sort((a, b) -> Double.compare(scores.get(b.id), scores.get(a.id)));
        List<String> result = new ArrayList<>();
        Deque<String> recentChannels = new ArrayDeque<>();
        while (!remaining.isEmpty() && result.size() < Math.max(0, config.limit)) {
            int index = 0;
            for (int i = 0; i < remaining.size(); i++) {
                if (remaining.get(i).channel == null || !recentChannels.contains(remaining.get(i).channel)) { index = i; break; }
            }
            Video chosen = remaining.remove(index);
            result.add(chosen.id);
            if (chosen.channel != null) recentChannels.addLast(chosen.channel);
            while (recentChannels.size() > Math.max(0, config.channelWindow)) recentChannels.removeFirst();
        }
        return result;
    }
    private LocalRanker() {}
}

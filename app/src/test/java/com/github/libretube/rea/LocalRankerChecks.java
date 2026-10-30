package com.github.libretube.rea;

import java.util.*;

/** Runs with plain javac/java as well as via the JUnit wrapper. */
public final class LocalRankerChecks {
    private static int checks;
    private static final long NOW = 1800000000000L;
    private static LocalRanker.Video v(String id, String channel, String title, long time) {
        return new LocalRanker.Video(id, channel, title, time, 120, 0);
    }
    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
        checks++;
    }
    public static void main(String[] args) {
        LocalRanker.Config c = new LocalRanker.Config();
        c.channelWindow = 0;
        List<LocalRanker.Video> items = Arrays.asList(
            v("old", "a", "Música", NOW - 365L*86400000),
            v("new", "b", "Android libre", NOW - 86400000),
            v("sub", "c", "Android nuevo", NOW - 86400000),
            v("sub", "c", "duplicado", NOW));
        c.subscriptions = Collections.singleton("c");
        List<String> rank = LocalRanker.rank(items, c, NOW);
        check(rank.get(0).equals("sub"), "subscription priority");
        check(rank.indexOf("new") < rank.indexOf("old"), "recent first");
        check(rank.size() == 3, "deduplication");
        check(rank.contains("old"), "old relevant videos retained");
        c.dismissed = Collections.singleton("sub");
        check(!LocalRanker.rank(items,c,NOW).contains("sub"), "dismissal");
        c.dismissed = Collections.emptySet();
        c.subscriptionsOnly = true;
        check(LocalRanker.rank(items,c,NOW).equals(Collections.singletonList("sub")), "subscription filter");
        c.subscriptionsOnly = false;
        c.after = NOW - 7L*86400000;
        List<LocalRanker.Video> missing = Arrays.asList(v("unknown",null,"Android",0),v("future",null,"Android",NOW+86400000));
        check(LocalRanker.rank(missing,c,NOW).isEmpty(), "unknown/future dates excluded");
        c.after = 0;
        c.query = "musica";
        check(LocalRanker.rank(items,c,NOW).get(0).equals("old"), "accent insensitive title search");
        c.query = "";
        c.subscriptions = Collections.emptySet();
        c.recencyWeight = 0;
        c.interests = LocalRanker.tokens("MÚSICA");
        check(LocalRanker.rank(items,c,NOW).get(0).equals("old"), "explicit interests");
        c.interests = Collections.emptySet();
        check(LocalRanker.rank(items,c,NOW).size() == 3, "works without history");
        c.historyChannels = Collections.singleton("c");
        check(LocalRanker.rank(items,c,NOW).get(0).equals("sub"), "consented channel affinity");
        c = new LocalRanker.Config();
        List<LocalRanker.Video> diverse = Arrays.asList(v("a1","a","",NOW),v("a2","a","",NOW),v("b1","b","",NOW));
        check(LocalRanker.rank(diverse,c,NOW).subList(0,2).equals(Arrays.asList("a1","b1")), "channel diversity");
        check(LocalRanker.rank(Collections.emptyList(),c,NOW).isEmpty(), "empty source");
        check(LocalRanker.rank(missing,c,NOW).size() == 2, "missing metadata safe without filters");
        c.minDuration = 121;
        check(LocalRanker.rank(items,c,NOW).isEmpty(), "minimum duration filter");
        c.minDuration = 0;
        c.maxDuration = 119;
        check(LocalRanker.rank(items,c,NOW).isEmpty(), "maximum duration filter");
        c.maxDuration = 0;
        c.subscriptionWeight = 0;
        c.recencyWeight = 0;
        c.topicWeight = 0;
        c.channelWindow = 0;
        check(LocalRanker.rank(items,c,NOW).get(0).equals("old"), "zero weights and stable ties");
        c.limit = 1;
        check(LocalRanker.rank(items,c,NOW).size() == 1, "configurable output limit");
        c = new LocalRanker.Config();
        List<LocalRanker.Video> large = new ArrayList<>();
        for(int i=0;i<10000;i++) large.add(v("v"+i,"c"+(i%100),"Android",NOW-i));
        long start = System.nanoTime();
        check(LocalRanker.rank(large,c,NOW).size() == 200, "bounded output");
        double ms = (System.nanoTime()-start)/1e6;
        System.out.println("PASS: "+checks+" checks; 10,000 supplied candidates, 2,000 processed, 200 returned in "+ms+" ms (host JVM; not Android benchmark)");
    }
}

package com.github.libretube.rea;

import com.github.libretube.util.NewPipeDownloaderImpl;
import org.junit.Test;
import org.junit.Assume;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import static org.junit.Assert.*;

/** Explicit opt-in network integration against the actual upstream content extractor. */
public class LiveProviderTest {
    @Test(timeout = 60000) public void realSearchProviderReturnsVideoMetadata() throws Exception {
        Assume.assumeTrue("Set REA_LIVE_PROVIDER_TESTS=true to call the real provider",
            "true".equals(System.getenv("REA_LIVE_PROVIDER_TESTS")));
        NewPipe.init(new NewPipeDownloaderImpl());
        SearchExtractor extractor = ServiceList.YouTube.getSearchExtractor("Kotlin Android tutorial");
        extractor.fetchPage();
        StreamInfoItem video = extractor.getInitialPage().getItems().stream()
            .filter(item -> item instanceof StreamInfoItem)
            .map(item -> (StreamInfoItem)item).findFirst().orElseThrow();
        assertFalse(video.getUrl().isEmpty());
        assertFalse(video.getName().isEmpty());
        assertNotNull(video.getUploaderUrl());
        // Publication date, duration and view count can be absent; no invented assertions.
    }
}

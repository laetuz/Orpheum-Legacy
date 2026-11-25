package id.neotica.orpheum.network.youtube;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class YouTubeUrlHelperTest {

    @Test
    public void normalizesInstanceBases() {
        assertEquals("http://host:8080",
                YouTubeUrlHelper.normalizeBase("host:8080/"));
        assertEquals("https://host",
                YouTubeUrlHelper.normalizeBase(" https://host/ "));
    }

    @Test
    public void routesAbsoluteMediaThroughInstance() {
        assertEquals("http://instance/videoplayback?x=1",
                YouTubeUrlHelper.mapThrough(
                        "http://instance",
                        "https://googlevideo.example/videoplayback?x=1"));
        assertEquals("http://instance/audio/id",
                YouTubeUrlHelper.mapThrough("http://instance", "/audio/id"));
        assertEquals("http://instance/vi/id.jpg",
                YouTubeUrlHelper.mapThrough("http://instance",
                        "//i.ytimg.com/vi/id.jpg"));
    }

    @Test
    public void reportsSafeUrlWithoutQuery() {
        assertEquals("http://host:8080/audio/id",
                YouTubeUrlHelper.safeUrl("http://host:8080/audio/id?token=secret"));
    }

    @Test
    public void extractsVideoIds() {
        assertEquals("abc123", YouTubeUrlHelper.videoIdFromUrl("/watch?v=abc123"));
        assertEquals("abc123", YouTubeUrlHelper.videoIdFromUrl(
                "https://www.youtube.com/watch?v=abc123&t=5"));
        assertEquals("dQw4w9WgXcQ",
                YouTubeUrlHelper.videoIdFromUrl("https://youtu.be/dQw4w9WgXcQ"));
    }
}

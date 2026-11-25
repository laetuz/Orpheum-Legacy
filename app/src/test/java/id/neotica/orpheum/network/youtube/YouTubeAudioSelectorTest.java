package id.neotica.orpheum.network.youtube;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class YouTubeAudioSelectorTest {

    @Test
    public void prefersHighestBitrateM4a() {
        String[] mimes = {"audio/webm", "audio/mp4", "audio/mp4"};
        String[] codecs = {"opus", "mp4a.40.2", "mp4a.40.5"};
        int[] bitrates = {160000, 128000, 192000};

        assertEquals(2, YouTubeAudioSelector.chooseBest(mimes, null, codecs, bitrates));
    }

    @Test
    public void rejectsOpusOnly() {
        String[] mimes = {"audio/webm"};
        String[] codecs = {"opus"};
        int[] bitrates = {160000};

        assertEquals(-1, YouTubeAudioSelector.chooseBest(mimes, null, codecs, bitrates));
    }

    @Test
    public void acceptsPipedM4aFormat() {
        String[] mimes = {"audio/mp4"};
        String[] formats = {"M4A"};
        String[] codecs = {"mp4a.40.2"};
        int[] bitrates = {128000};

        assertEquals(0, YouTubeAudioSelector.chooseBest(mimes, formats, codecs, bitrates));
    }
}

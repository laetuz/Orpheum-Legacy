package id.neotica.orpheum.source.subsonic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SubsonicClientTest {

    @Test
    public void normalizesServerUrls() {
        assertEquals("http://192.168.1.10:4533",
                SubsonicClient.normalizeBase("192.168.1.10:4533"));
        assertEquals("http://host:4533",
                SubsonicClient.normalizeBase(" http://host:4533/ "));
        assertEquals("https://host",
                SubsonicClient.normalizeBase("https://host/rest/"));
        assertEquals("http://host/music",
                SubsonicClient.normalizeBase("http://host/music/rest"));
        assertEquals("http://host/music",
                SubsonicClient.normalizeBase("http://host/music/"));
    }

    @Test
    public void computesMd5Tokens() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", SubsonicClient.md5Hex("abc"));
        assertEquals("b305cadbb3bce54f3aa59c64fec00dea",
                SubsonicClient.token("password", "salt"));
    }

    @Test
    public void buildsAuthenticatedUrl() {
        String url = SubsonicClient.buildUrl("host:4533", "ping", "user name", "s&1", "t t", null);
        assertEquals("http://host:4533/rest/ping.view"
                + "?u=user+name&t=t+t&s=s%261&v=1.16.1&c=Orpheum&f=json", url);
    }

    @Test
    public void generatesHexSalts() {
        String salt = SubsonicClient.randomSalt();
        assertEquals(16, salt.length());
        assertFalse(salt.equals(SubsonicClient.randomSalt()));
        assertTrue(salt.matches("[0-9a-f]+"));
    }
}

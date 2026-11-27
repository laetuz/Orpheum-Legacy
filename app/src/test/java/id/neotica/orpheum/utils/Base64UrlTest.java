package id.neotica.orpheum.utils;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class Base64UrlTest {

    @Test
    public void decodesJwtPayload() {
        byte[] decoded = Base64Url.decode("eyJzdWIiOiJqb2huIn0");
        assertEquals("{\"sub\":\"john\"}", new String(decoded));
    }

    @Test
    public void acceptsPadding() {
        byte[] decoded = Base64Url.decode("eyJzdWIiOiJqb2huIn0=");
        assertEquals("{\"sub\":\"john\"}", new String(decoded));
    }

    @Test
    public void decodesUrlAlphabet() {
        assertArrayEquals(new byte[]{(byte) 0xFB, (byte) 0xFF},
                Base64Url.decode("-_8"));
    }

    @Test
    public void decodesStandardAlphabet() {
        assertArrayEquals(new byte[]{(byte) 0xFB, (byte) 0xFF},
                Base64Url.decode("+/8="));
    }

    @Test
    public void rejectsInvalidInput() {
        assertNull(Base64Url.decode("A"));
        assertNull(Base64Url.decode("A*"));
        assertNull(Base64Url.decode(null));
    }
}

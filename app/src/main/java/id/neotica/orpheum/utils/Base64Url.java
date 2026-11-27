package id.neotica.orpheum.utils;

/**
 * Minimal pure-Java base64url decoder (RFC 4648 section 5), used for JWT payloads.
 *
 * <p>Deliberately avoids {@code android.util.Base64} (API 8), which is not
 * available on API 3. This decoder works on API 1+.
 */
public final class Base64Url {

    private static final int[] VALUES = new int[128];

    static {
        for (int i = 0; i < VALUES.length; i++) {
            VALUES[i] = -1;
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            VALUES[i] = i - 'A';
        }
        for (int i = 'a'; i <= 'z'; i++) {
            VALUES[i] = i - 'a' + 26;
        }
        for (int i = '0'; i <= '9'; i++) {
            VALUES[i] = i - '0' + 52;
        }
        VALUES['-'] = 62;
        VALUES['_'] = 63;
        VALUES['+'] = 62;
        VALUES['/'] = 63;
    }

    private Base64Url() { }

    public static byte[] decode(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder cleaned = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '=') {
                break;
            }
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                continue;
            }
            if (c >= 128 || VALUES[c] < 0) {
                return null;
            }
            cleaned.append(c);
        }

        int length = cleaned.length();
        if (length == 0) {
            return new byte[0];
        }
        if (length % 4 == 1) {
            return null;
        }

        byte[] out = new byte[length * 3 / 4];
        int buffer = 0;
        int bits = 0;
        int index = 0;
        for (int i = 0; i < length; i++) {
            buffer = (buffer << 6) | VALUES[cleaned.charAt(i)];
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out[index++] = (byte) ((buffer >> bits) & 0xFF);
            }
        }
        return out;
    }
}

package id.neotica.orpheum.network.youtube;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

/**
 * Turns a progressive muxed MP4 into an audio-only MP4 by replacing each video
 * {@code trak} box with a {@code free} box of the same size. The moov and mdat
 * sizes/offsets stay unchanged, so the audio sample tables remain valid.
 */
public final class Mp4AudioOnlyStripper {

    private Mp4AudioOnlyStripper() { }

    public static void stripVideoTrack(File file) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        try {
            long length = raf.length();
            long moovOffset = -1L;
            long moovSize = 0L;
            boolean fragmented = false;
            long offset = 0L;

            while (offset + 8 <= length) {
                Box box = readBox(raf, offset, length);
                if ("moov".equals(box.type)) {
                    moovOffset = offset;
                    moovSize = box.size;
                } else if ("moof".equals(box.type)) {
                    fragmented = true;
                }
                offset += box.size;
            }

            if (fragmented) {
                throw new IOException("Fragmented MP4 is not supported");
            }
            if (moovOffset < 0) {
                throw new IOException("MP4 has no moov box");
            }

            ArrayList<Long> videoTracks = new ArrayList<Long>();
            boolean audio = false;
            long position = moovOffset + 8;
            long end = moovOffset + moovSize;
            while (position + 8 <= end) {
                Box box = readBox(raf, position, end);
                if ("trak".equals(box.type)) {
                    String handler = findHandler(raf, position + box.headerSize,
                            position + box.size);
                    if ("vide".equals(handler)) {
                        videoTracks.add(position);
                    } else if ("soun".equals(handler)) {
                        audio = true;
                    }
                }
                position += box.size;
            }

            if (!audio) {
                throw new IOException("MP4 has no audio track");
            }

            for (int i = 0; i < videoTracks.size(); i++) {
                raf.seek(videoTracks.get(i) + 4);
                raf.write("free".getBytes("US-ASCII"));
            }
        } finally {
            raf.close();
        }
    }

    private static String findHandler(RandomAccessFile raf, long start, long end)
            throws IOException {
        long position = start;
        while (position + 8 <= end) {
            Box box = readBox(raf, position, end);
            if ("mdia".equals(box.type)) {
                long child = position + box.headerSize;
                long childEnd = position + box.size;
                while (child + 8 <= childEnd) {
                    Box inside = readBox(raf, child, childEnd);
                    if ("hdlr".equals(inside.type)) {
                        raf.seek(child + inside.headerSize + 8);
                        byte[] handler = new byte[4];
                        raf.readFully(handler);
                        return new String(handler, "US-ASCII");
                    }
                    child += inside.size;
                }
            }
            position += box.size;
        }
        return null;
    }

    private static Box readBox(RandomAccessFile raf, long offset, long end)
            throws IOException {
        raf.seek(offset);
        long size = ((long) raf.readInt()) & 0xffffffffL;
        byte[] type = new byte[4];
        raf.readFully(type);
        long header = 8L;
        if (size == 1L) {
            size = raf.readLong();
            header = 16L;
        } else if (size == 0L) {
            size = end - offset;
        }
        if (size < header || offset + size > end) {
            throw new IOException("Invalid MP4 box");
        }
        return new Box(size, header, new String(type, "US-ASCII"));
    }

    private static class Box {
        final long size;
        final long headerSize;
        final String type;

        Box(long size, long headerSize, String type) {
            this.size = size;
            this.headerSize = headerSize;
            this.type = type;
        }
    }
}

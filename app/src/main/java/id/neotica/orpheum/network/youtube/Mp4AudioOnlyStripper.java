package id.neotica.orpheum.network.youtube;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

/**
 * Turns a progressive muxed MP4 into an audio-only MP4 by removing video
 * {@code trak} boxes from {@code moov} and replacing their bytes with a
 * top-level {@code free} box. The moov + free total size stays unchanged, so
 * the mdat offset and all audio sample offsets remain valid.
 */
public final class Mp4AudioOnlyStripper {

    private Mp4AudioOnlyStripper() { }

    public static void stripVideoTrack(File file) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        try {
            long length = raf.length();
            long moovOffset = -1L;
            Box moov = null;
            boolean fragmented = false;
            long offset = 0L;

            while (offset + 8 <= length) {
                Box box = readFileBox(raf, offset, length);
                if ("moov".equals(box.type)) {
                    moovOffset = offset;
                    moov = box;
                } else if ("moof".equals(box.type)) {
                    fragmented = true;
                }
                offset = box.start + box.size;
            }

            if (fragmented) {
                throw new IOException("Fragmented MP4 is not supported");
            }
            if (moov == null || moovOffset < 0) {
                throw new IOException("MP4 has no moov box");
            }
            if (moov.size > Integer.MAX_VALUE) {
                throw new IOException("MP4 moov is too large");
            }

            byte[] data = new byte[(int) moov.size];
            raf.seek(moovOffset);
            raf.readFully(data);

            ArrayList<Box> children = childBoxes(data, (int) moov.headerSize,
                    data.length);
            ArrayList<Box> videoTracks = new ArrayList<Box>();
            boolean audio = false;
            for (int i = 0; i < children.size(); i++) {
                Box child = children.get(i);
                if (!"trak".equals(child.type)) {
                    continue;
                }
                String handler = handlerOfTrack(data, child);
                if ("vide".equals(handler)) {
                    videoTracks.add(child);
                } else if ("soun".equals(handler)) {
                    audio = true;
                }
            }

            if (!audio) {
                throw new IOException("MP4 has no audio track");
            }
            if (videoTracks.isEmpty()) {
                return;
            }

            ByteArrayOutputStream remaining = new ByteArrayOutputStream();
            for (int i = 0; i < children.size(); i++) {
                Box child = children.get(i);
                boolean remove = false;
                for (int j = 0; j < videoTracks.size(); j++) {
                    if (videoTracks.get(j).start == child.start) {
                        remove = true;
                        break;
                    }
                }
                if (!remove) {
                    remaining.write(data, (int) child.start, (int) child.size);
                }
            }

            byte[] body = remaining.toByteArray();
            int newMoovSize = (int) moov.headerSize + body.length;
            int removedBytes = (int) moov.size - newMoovSize;
            if (removedBytes < 8) {
                throw new IOException("MP4 video track is too small");
            }

            ByteArrayOutputStream replacement = new ByteArrayOutputStream(
                    (int) moov.size);
            writeBoxHeader(replacement, newMoovSize, "moov", (int) moov.headerSize);
            replacement.write(body);
            writeBoxHeader(replacement, removedBytes, "free", 8);
            byte[] padding = new byte[removedBytes - 8];
            replacement.write(padding);

            if (replacement.size() != moov.size) {
                throw new IOException("MP4 remux size mismatch");
            }

            raf.seek(moovOffset);
            raf.write(replacement.toByteArray());
        } finally {
            raf.close();
        }
    }

    private static String handlerOfTrack(byte[] data, Box track) {
        ArrayList<Box> trackChildren = childBoxes(data,
                (int) (track.start + track.headerSize),
                (int) (track.start + track.size));
        for (int i = 0; i < trackChildren.size(); i++) {
            Box box = trackChildren.get(i);
            if (!"mdia".equals(box.type)) {
                continue;
            }
            ArrayList<Box> mdiaChildren = childBoxes(data,
                    (int) (box.start + box.headerSize),
                    (int) (box.start + box.size));
            for (int j = 0; j < mdiaChildren.size(); j++) {
                Box hdlr = mdiaChildren.get(j);
                if (!"hdlr".equals(hdlr.type)) {
                    continue;
                }
                int at = (int) (hdlr.start + hdlr.headerSize + 8);
                if (at + 4 > data.length) {
                    return null;
                }
                try {
                    return new String(data, at, 4, "US-ASCII");
                } catch (IOException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private static ArrayList<Box> childBoxes(byte[] data, int start, int end) {
        ArrayList<Box> result = new ArrayList<Box>();
        int offset = start;
        while (offset + 8 <= end) {
            Box box;
            try {
                box = readArrayBox(data, offset, end);
            } catch (IOException e) {
                break;
            }
            result.add(box);
            offset = (int) (box.start + box.size);
        }
        return result;
    }

    private static Box readFileBox(RandomAccessFile raf, long offset, long end)
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
        return new Box(offset, size, header, new String(type, "US-ASCII"));
    }

    private static Box readArrayBox(byte[] data, int offset, int end)
            throws IOException {
        if (offset + 8 > end) {
            throw new IOException("Invalid MP4 box");
        }
        long size = ((long) readInt(data, offset)) & 0xffffffffL;
        byte[] type = new byte[4];
        System.arraycopy(data, offset + 4, type, 0, 4);
        long header = 8L;
        if (size == 1L) {
            if (offset + 16 > end) {
                throw new IOException("Invalid MP4 box");
            }
            size = (((long) readInt(data, offset + 8)) << 32)
                    | (((long) readInt(data, offset + 12)) & 0xffffffffL);
            header = 16L;
        } else if (size == 0L) {
            size = end - offset;
        }
        if (size < header || offset + size > end) {
            throw new IOException("Invalid MP4 box");
        }
        return new Box(offset, size, header, new String(type, "US-ASCII"));
    }

    private static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xff) << 24)
                | ((data[offset + 1] & 0xff) << 16)
                | ((data[offset + 2] & 0xff) << 8)
                | (data[offset + 3] & 0xff);
    }

    private static void writeBoxHeader(ByteArrayOutputStream out, int size,
                                       String type, int headerSize)
            throws IOException {
        if (headerSize == 16) {
            out.write(new byte[]{0, 0, 0, 1});
            out.write(type.getBytes("US-ASCII"));
            out.write(new byte[]{
                    0, 0, 0, 0,
                    (byte) (size >>> 24), (byte) (size >>> 16),
                    (byte) (size >>> 8), (byte) size
            });
        } else {
            out.write(new byte[]{
                    (byte) (size >>> 24), (byte) (size >>> 16),
                    (byte) (size >>> 8), (byte) size
            });
            out.write(type.getBytes("US-ASCII"));
        }
    }

    private static class Box {
        final long start;
        final long size;
        final long headerSize;
        final String type;

        Box(long start, long size, long headerSize, String type) {
            this.start = start;
            this.size = size;
            this.headerSize = headerSize;
            this.type = type;
        }
    }
}

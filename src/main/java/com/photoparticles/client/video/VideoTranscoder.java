package com.photoparticles.client.video;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.jcodec.api.awt.AWTFrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;

/**
 * 把 GIF 或 MP4 解析成固定帧率、固定尺寸的帧序列，再打包成可传输的数据。
 * 解析可能很慢，调用方必须放在后台线程。
 */
public final class VideoTranscoder {
    public static final int MAX_SIDE = 192;
    public static final int MAX_FRAMES = 40;
    public static final int MIN_FRAMES = 8;
    /** 打包后允许上传的最大字节数。 */
    public static final int MAX_BLOB_BYTES = 8 * 1024 * 1024;

    public interface Progress {
        void update(float fraction, String text);
    }

    private record Frames(List<int[]> frames, int width, int height) {
    }

    private VideoTranscoder() {
    }

    public static boolean isVideoFile(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".mp4");
    }

    /**
     * 返回打包好的视频数据，体积过大时自动抽帧和降尺寸；完全无法解析时返回 null。
     */
    public static byte[] transcode(File file, Progress progress) throws Exception {
        byte[] blob = build(file, MAX_SIDE, MAX_FRAMES, progress);
        if (blob != null && blob.length <= MAX_BLOB_BYTES) {
            return blob;
        }

        // 超了就抽掉一半的帧再试
        int frames = MAX_FRAMES;
        while (blob != null && blob.length > MAX_BLOB_BYTES && frames > MIN_FRAMES) {
            frames /= 2;
            progress.update(0.0F, "体积过大，压缩到 " + frames + " 帧");
            blob = build(file, MAX_SIDE, frames, progress);
        }
        if (blob != null && blob.length > MAX_BLOB_BYTES) {
            progress.update(0.0F, "体积仍然过大，降低分辨率");
            blob = build(file, MAX_SIDE * 2 / 3, MIN_FRAMES, progress);
        }
        return blob != null && blob.length <= MAX_BLOB_BYTES ? blob : null;
    }

    private static byte[] build(File file, int maxSide, int maxFrames, Progress progress) throws Exception {
        Frames decoded = isVideoFile(file)
                ? decodeVideo(file, maxSide, maxFrames, progress)
                : decodeGif(file, maxSide, maxFrames, progress);
        if (decoded.frames.isEmpty()) {
            return null;
        }
        return VideoBlob.pack(decoded.width, decoded.height, decoded.frames);
    }

    private static Frames decodeGif(File file, int maxSide, int maxFrames, Progress progress) {
        List<int[]> frames = new ArrayList<>();
        int width = 0;
        int height = 0;
        try (ImageInputStream stream = ImageIO.createImageInputStream(file)) {
            if (stream == null) {
                return new Frames(frames, 0, 0);
            }
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) {
                return new Frames(frames, 0, 0);
            }
            ImageReader reader = readers.next();
            reader.setInput(stream, false, false);
            int count = Math.min(reader.getNumImages(true), maxFrames);
            for (int i = 0; i < count; i++) {
                BufferedImage frame = reader.read(i);
                if (frame == null) {
                    continue;
                }
                if (width == 0) {
                    double scale = Math.min(1.0D, (double) maxSide / Math.max(frame.getWidth(), frame.getHeight()));
                    width = Math.max(1, (int) Math.round(frame.getWidth() * scale));
                    height = Math.max(1, (int) Math.round(frame.getHeight() * scale));
                }
                frames.add(resize(frame, width, height));
                progress.update((i + 1) / (float) count, "解析 GIF " + (i + 1) + "/" + count);
            }
            reader.dispose();
        } catch (Throwable e) {
            return new Frames(frames, 0, 0);
        }
        return new Frames(frames, width, height);
    }

    private static Frames decodeVideo(File file, int maxSide, int maxFrames, Progress progress) throws Exception {
        List<int[]> frames = new ArrayList<>();
        int width = 0;
        int height = 0;
        try (SeekableByteChannel channel = NIOUtils.readableChannel(file)) {
            AWTFrameGrab grab = AWTFrameGrab.createAWTFrameGrab(channel);
            int total = Math.max(1, grab.getVideoTrack().getMeta().getTotalFrames());
            int step = Math.max(1, total / Math.max(1, maxFrames));
            for (int index = 0; index < total && frames.size() < maxFrames; index++) {
                BufferedImage image = grab.getFrame();
                if (image == null) {
                    continue;
                }
                if (width == 0) {
                    double scale = Math.min(1.0D, (double) maxSide / Math.max(image.getWidth(), image.getHeight()));
                    width = Math.max(1, (int) Math.round(image.getWidth() * scale));
                    height = Math.max(1, (int) Math.round(image.getHeight() * scale));
                }
                if (index % step == 0) {
                    frames.add(resize(image, width, height));
                    progress.update(Math.min(0.98F, index / (float) total),
                            "解析视频 " + frames.size() + "/" + maxFrames);
                }
            }
        }
        return new Frames(frames, width, height);
    }

    private static int[] resize(BufferedImage source, int width, int height) {
        int[] pixels = new int[width * height];
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int[] row = new int[sourceWidth];
        for (int y = 0; y < height; y++) {
            int y0 = (int) ((long) y * sourceHeight / height);
            int y1 = Math.max(y0 + 1, (int) ((long) (y + 1) * sourceHeight / height));
            for (int x = 0; x < width; x++) {
                int x0 = (int) ((long) x * sourceWidth / width);
                int x1 = Math.max(x0 + 1, (int) ((long) (x + 1) * sourceWidth / width));
                pixels[y * width + x] = average(source, x0, y0, x1, y1, row);
            }
        }
        return pixels;
    }

    private static int average(BufferedImage source, int x0, int y0, int x1, int y1, int[] row) {
        long r = 0;
        long g = 0;
        long b = 0;
        long a = 0;
        int count = 0;
        for (int y = y0; y < y1; y++) {
            source.getRGB(x0, y, x1 - x0, 1, row, 0, source.getWidth());
            for (int i = 0; i < x1 - x0; i++) {
                int value = row[i];
                a += (value >>> 24) & 0xFF;
                r += (value >>> 16) & 0xFF;
                g += (value >>> 8) & 0xFF;
                b += value & 0xFF;
                count++;
            }
        }
        if (count == 0) {
            return 0;
        }
        return (int) (a / count) << 24 | (int) (r / count) << 16 | (int) (g / count) << 8 | (int) (b / count);
    }
}

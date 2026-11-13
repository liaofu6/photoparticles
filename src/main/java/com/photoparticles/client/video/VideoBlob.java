package com.photoparticles.client.video;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * 视频帧的打包格式：头信息 + Deflate 压缩的 ARGB 像素。
 * 视频文件在客户端解析一次，之后客户端之间直接传这份数据，不用再解码。
 */
public final class VideoBlob {
    private static final int MAGIC = 0x50505646; // "PPVF"
    private static final int FORMAT = 1;

    public record Decoded(int width, int height, List<int[]> frames) {
    }

    private VideoBlob() {
    }

    public static boolean isBlob(byte[] data) {
        if (data == null || data.length < 5) {
            return false;
        }
        return (data[0] & 0xFF) == ((MAGIC >>> 24) & 0xFF)
                && (data[1] & 0xFF) == ((MAGIC >>> 16) & 0xFF)
                && (data[2] & 0xFF) == ((MAGIC >>> 8) & 0xFF)
                && (data[3] & 0xFF) == (MAGIC & 0xFF);
    }

    public static byte[] pack(int width, int height, List<int[]> frames) throws Exception {
        ByteArrayOutputStream raw = new ByteArrayOutputStream(width * height * 4 * frames.size());
        DataOutputStream rawOut = new DataOutputStream(raw);
        for (int[] frame : frames) {
            for (int pixel : frame) {
                rawOut.writeInt(pixel);
            }
        }
        rawOut.flush();

        ByteArrayOutputStream compressed = new ByteArrayOutputStream(Math.max(1024, raw.size() / 2));
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        try (DeflaterOutputStream out = new DeflaterOutputStream(compressed, deflater)) {
            raw.writeTo(out);
        } finally {
            deflater.end();
        }

        ByteArrayOutputStream header = new ByteArrayOutputStream(32);
        DataOutputStream head = new DataOutputStream(header);
        head.writeInt(MAGIC);
        head.writeByte(FORMAT);
        head.writeShort(width);
        head.writeShort(height);
        head.writeShort(frames.size());
        head.writeInt(compressed.size());
        head.flush();

        byte[] headBytes = header.toByteArray();
        byte[] body = compressed.toByteArray();
        byte[] result = new byte[headBytes.length + body.length];
        System.arraycopy(headBytes, 0, result, 0, headBytes.length);
        System.arraycopy(body, 0, result, headBytes.length, body.length);
        return result;
    }

    public static Decoded unpack(byte[] data) throws Exception {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        if (in.readInt() != MAGIC || in.readByte() != FORMAT) {
            return null;
        }
        int width = in.readShort() & 0xFFFF;
        int height = in.readShort() & 0xFFFF;
        int frameCount = in.readShort() & 0xFFFF;
        int compressedLength = in.readInt();
        byte[] compressed = in.readNBytes(compressedLength);

        ByteArrayOutputStream plain = new ByteArrayOutputStream(width * height * 4 * frameCount);
        try (InflaterInputStream iis = new InflaterInputStream(new ByteArrayInputStream(compressed))) {
            iis.transferTo(plain);
        }

        byte[] bytes = plain.toByteArray();
        DataInputStream pixelsIn = new DataInputStream(new ByteArrayInputStream(bytes));
        int pixels = width * height;
        List<int[]> frames = new ArrayList<>(frameCount);
        for (int i = 0; i < frameCount; i++) {
            int[] frame = new int[pixels];
            for (int p = 0; p < pixels; p++) {
                frame[p] = pixelsIn.readInt();
            }
            frames.add(frame);
        }
        return new Decoded(width, height, frames);
    }
}
